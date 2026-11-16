package br.com.cachemist.fullmetal.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Classe responsável por varrer a memória de tempos em tempos e remover os itens expirados (TTL) */
@Component
public class RotinaLimpezaCache {

    private static final Logger log = LoggerFactory.getLogger(RotinaLimpezaCache.class);

    private final ConteudoCache cache;

    public RotinaLimpezaCache(ConteudoCache cache) {
        this.cache = cache;
    }

    /** Roda a cada app.cache.limpeza-segundos (padrão 30 s). Sem ela, o expirado só sai quando alguém busca por ele. */
    @Scheduled(fixedRateString = "${app.cache.limpeza-segundos:30}", timeUnit = TimeUnit.SECONDS)
    public void removerExpirados() {
        int removidos = cache.removerExpirados();
        if (removidos > 0) {
            log.info("Rotina de limpeza: {} lembrancas expiradas removidas", removidos);
        }
    }
}
