package br.com.cachemist.fullmetal.cache;

public record EstatisticasCache(
        int tamanho,
        int limite,
        long ttlSegundos,
        long hits,
        long misses,
        long evictions,
        long expiracoes,
        double hitRatio
) {
}
