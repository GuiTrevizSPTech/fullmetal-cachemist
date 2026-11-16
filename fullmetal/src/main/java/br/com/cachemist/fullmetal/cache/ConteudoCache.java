package br.com.cachemist.fullmetal.cache;

import br.com.cachemist.fullmetal.conteudo.Conteudo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
/** Classe responsável por representar a memória (cache) */
public class ConteudoCache {

    private static final Logger log = LoggerFactory.getLogger(ConteudoCache.class);

    private final Map<Long, EntradaCache> memoria = new ConcurrentHashMap<>();
    private final int maxItens;
    private final Duration ttl;

    // AtomicLong: contador seguro para várias threads sem precisar de synchronized.
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();
    private final AtomicLong expiracoes = new AtomicLong();

    public ConteudoCache(CacheProperties props) {
        this.maxItens = props.maxItens();
        this.ttl = Duration.ofSeconds(props.ttlSegundos());
    }

    public Optional<Conteudo> buscar(Long id) {
        Instant agora = Instant.now();
        EntradaCache entrada = memoria.get(id);

        if (entrada == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }

        if (entrada.expirou(agora, ttl)) {
            if (memoria.remove(id, entrada)) {
                expiracoes.incrementAndGet();
                log.info("Lembranca desbotou (TTL) - id={} - guardada em {}", id, entrada.getArmazenadoEm());
            }
            misses.incrementAndGet();
            return Optional.empty();
        }

        entrada.registrarAcesso(agora);
        hits.incrementAndGet();
        return Optional.of(entrada.getConteudo());
    }

    public synchronized void guardar(Conteudo conteudo) {
        Long id = conteudo.getId();
        if (!memoria.containsKey(id)) {
            while (memoria.size() >= maxItens) {
                esquecerMenosUsado();
            }
        }
        memoria.put(id, new EntradaCache(conteudo, Instant.now()));
    }

    private void esquecerMenosUsado() {
        memoria.entrySet().stream()
                .min(Comparator.comparing(e -> e.getValue().getUltimoAcesso()))
                .ifPresent(maisAntigo -> {
                    memoria.remove(maisAntigo.getKey());
                    evictions.incrementAndGet();
                    log.info("Memoria cheia: esquecendo circulo id={} (LRU, ultimo acesso {})",
                            maisAntigo.getKey(), maisAntigo.getValue().getUltimoAcesso());
                });
    }

    public void invalidar(Long id) {
        if (memoria.remove(id) != null) {
            log.info("Lembranca invalidada - id={}", id);
        }
    }

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
        return new EstatisticasCache(memoria.size(), maxItens, ttl.toSeconds(),
                h, m, evictions.get(), expiracoes.get(), hitRatio);
    }
}
