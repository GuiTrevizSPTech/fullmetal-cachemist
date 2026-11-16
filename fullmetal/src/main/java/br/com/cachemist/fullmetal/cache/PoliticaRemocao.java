package br.com.cachemist.fullmetal.cache;

import java.time.Instant;
import java.util.Comparator;
import java.util.function.Function;

/** Enum responsável por definir qual item sai da memória quando ela está cheia */
public enum PoliticaRemocao {

    /** Least Recently Used: esquece o item com o último acesso mais antigo */
    LRU(EntradaCache::getUltimoAcesso),

    /** First In, First Out: esquece o item guardado há mais tempo, mesmo que tenha sido usado agora */
    FIFO(EntradaCache::getArmazenadoEm);

    //Function: recebe uma entrada e devolve o momento usado para comparar (último acesso ou armazenamento)
    private final Function<EntradaCache<?>, Instant> momentoDeReferencia;

    PoliticaRemocao(Function<EntradaCache<?>, Instant> momentoDeReferencia) {
        this.momentoDeReferencia = momentoDeReferencia;
    }

    /** Ordena as entradas da que deve sair primeiro para a que deve sair por último. */
    public Comparator<EntradaCache<?>> comparador() {
        return Comparator.comparing(momentoDeReferencia);
    }
}
