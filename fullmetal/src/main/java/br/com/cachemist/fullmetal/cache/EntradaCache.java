package br.com.cachemist.fullmetal.cache;

import br.com.cachemist.fullmetal.conteudo.Conteudo;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

@Getter
public class EntradaCache {

    private final Conteudo conteudo;
    private final Instant armazenadoEm;
    //volatile é um modificador que garante visibilidade das alterações para multi-threads
    private volatile Instant ultimoAcesso;

    public EntradaCache(Conteudo conteudo, Instant agora) {
        this.conteudo = conteudo;
        this.armazenadoEm = agora;
        this.ultimoAcesso = agora;
    }

    public boolean expirou(Instant agora, Duration ttl) {
        return armazenadoEm.plus(ttl).isBefore(agora);
    }

    public void registrarAcesso(Instant agora) {
        this.ultimoAcesso = agora;
    }
}
