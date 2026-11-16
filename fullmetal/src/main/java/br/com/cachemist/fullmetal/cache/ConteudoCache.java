package br.com.cachemist.fullmetal.cache;

import br.com.cachemist.fullmetal.conteudo.Conteudo;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/** Classe responsável por representar a memória (cache) do alquimista: um CacheSimples de círculos por id */
@Component
public class ConteudoCache extends CacheSimples<Long, Conteudo> {

    public ConteudoCache(CacheProperties props) {
        super(props.maxItens(), Duration.ofSeconds(props.ttlSegundos()), props.politica(), Clock.systemUTC());
    }

    /** Guarda o conteúdo usando o próprio id como chave. */
    public void guardar(Conteudo conteudo) {
        guardar(conteudo.getId(), conteudo);
    }
}
