package br.com.cachemist.fullmetal.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Classe responsável por ler as variáveis do application.properties */
@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(int maxItens, long ttlSegundos) {
}
