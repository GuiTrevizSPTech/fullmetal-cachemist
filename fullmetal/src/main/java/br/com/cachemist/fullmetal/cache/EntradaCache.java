package br.com.cachemist.fullmetal.cache;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

/** Classe responsável por representar um item guardado na memória: o valor, quando foi guardado e o último acesso */
@Getter
public class EntradaCache<V> {

    private final V valor;
    private final Instant armazenadoEm;
    //volatile é um modificador que garante visibilidade das alterações para multi-threads
    private volatile Instant ultimoAcesso;

    public EntradaCache(V valor, Instant agora) {
        this.valor = valor;
        this.armazenadoEm = agora;
        this.ultimoAcesso = agora;
    }

    /** O TTL conta a partir de armazenadoEm e não é renovado pelo acesso. */
    public boolean expirou(Instant agora, Duration ttl) {
        return armazenadoEm.plus(ttl).isBefore(agora);
    }

    public void registrarAcesso(Instant agora) {
        this.ultimoAcesso = agora;
    }
}
