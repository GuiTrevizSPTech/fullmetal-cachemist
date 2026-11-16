package br.com.cachemist.fullmetal.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
    Classe responsável por um cache genérico: funciona para qualquer chave (K) e qualquer valor (V).

    === BUSCAR ===
    1. Não achou a chave -> MISS
    2. Achou, mas passou do TTL -> remove, conta expiração e MISS
    3. Achou e está válida -> atualiza o último acesso e HIT

    === GUARDAR ===
    1. Se a chave é nova e a memória está cheia, esquece um item segundo a política (LRU ou FIFO)
    2. Guarda a nova entrada
*/
public class CacheSimples<K, V> {

    //getClass() faz o log sair com o nome da classe filha (ex.: ConteudoCache)
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<K, EntradaCache<V>> memoria = new ConcurrentHashMap<>();
    private final int maxItens;
    private final Duration ttl;
    private final PoliticaRemocao politica;
    //Clock: o relógio fica injetável para os testes conseguirem "avançar o tempo" sem Thread.sleep
    private final Clock relogio;

    // AtomicLong: contador seguro para várias threads sem precisar de synchronized.
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();
    private final AtomicLong expiracoes = new AtomicLong();

    public CacheSimples(int maxItens, Duration ttl, PoliticaRemocao politica, Clock relogio) {
        if (maxItens <= 0) {
            throw new IllegalArgumentException("app.cache.max-itens precisa ser maior que zero");
        }
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("app.cache.ttl-segundos precisa ser maior que zero");
        }
        this.maxItens = maxItens;
        this.ttl = ttl;
        this.politica = politica;
        this.relogio = relogio;
    }

    /** Procura na memória. Optional vazio = MISS (não estava ou expirou). */
    public Optional<V> buscar(K chave) {
        Instant agora = relogio.instant();
        EntradaCache<V> entrada = memoria.get(chave);

        if (entrada == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }

        if (entrada.expirou(agora, ttl)) {
            removerSeExpirada(chave, entrada);
            misses.incrementAndGet();
            return Optional.empty();
        }

        entrada.registrarAcesso(agora);
        hits.incrementAndGet();
        return Optional.of(entrada.getValor());
    }

    /** Guarda na memória. Se estiver cheia, esquece um item de acordo com a política. */
    public synchronized void guardar(K chave, V valor) {
        if (!memoria.containsKey(chave)) {
            while (memoria.size() >= maxItens) {
                esquecerUm();
            }
        }
        memoria.put(chave, new EntradaCache<>(valor, relogio.instant()));
    }

    private void esquecerUm() {
        memoria.entrySet().stream()
                .min((a, b) -> politica.comparador().compare(a.getValue(), b.getValue()))
                .ifPresent(escolhido -> {
                    memoria.remove(escolhido.getKey());
                    evictions.incrementAndGet();
                    log.info("Memoria cheia: esquecendo id={} ({}, guardado em {}, ultimo acesso {})",
                            escolhido.getKey(), politica,
                            escolhido.getValue().getArmazenadoEm(), escolhido.getValue().getUltimoAcesso());
                });
    }

    /** Remove todas as entradas vencidas. Usado pela rotina agendada; devolve quantas saíram. */
    public int removerExpirados() {
        Instant agora = relogio.instant();
        int removidos = 0;
        for (Map.Entry<K, EntradaCache<V>> item : memoria.entrySet()) {
            if (item.getValue().expirou(agora, ttl) && removerSeExpirada(item.getKey(), item.getValue())) {
                removidos++;
            }
        }
        return removidos;
    }

    //remove(chave, entrada) só remove se ainda for ESSA entrada (evita apagar uma recém-guardada)
    private boolean removerSeExpirada(K chave, EntradaCache<V> entrada) {
        if (memoria.remove(chave, entrada)) {
            expiracoes.incrementAndGet();
            log.info("Lembranca desbotou (TTL) - id={} - guardada em {}", chave, entrada.getArmazenadoEm());
            return true;
        }
        return false;
    }

    /** Invalidação de um item (usada no PUT e no DELETE). */
    public void invalidar(K chave) {
        if (memoria.remove(chave) != null) {
            log.info("Lembranca invalidada - id={}", chave);
        }
    }

    /** Esquece tudo. Os contadores continuam (são histórico). */
    public void limpar() {
        int quantos = memoria.size();
        memoria.clear();
        log.info("Memoria do alquimista apagada - {} itens esquecidos", quantos);
    }

    public EstatisticasCache estatisticas() {
        long h = hits.get();
        long m = misses.get();
        long total = h + m;
        double hitRatio = total == 0 ? 0.0 : (double) h / total;
        return new EstatisticasCache(memoria.size(), maxItens, ttl.toSeconds(), politica.name(),
                h, m, evictions.get(), expiracoes.get(), hitRatio);
    }
}
