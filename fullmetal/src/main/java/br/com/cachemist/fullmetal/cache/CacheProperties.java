package br.com.cachemist.fullmetal.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Classe responsável por ler as variáveis do application.properties */
@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(
        @DefaultValue("5") int maxItens,
        @DefaultValue("60") long ttlSegundos,
        @DefaultValue("LRU") PoliticaRemocao politica,
        @DefaultValue("30") long limpezaSegundos
) {
}
