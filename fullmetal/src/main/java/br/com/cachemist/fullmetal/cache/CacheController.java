package br.com.cachemist.fullmetal.cache;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cache")
public class CacheController {

    private final ConteudoCache cache;

    public CacheController(ConteudoCache cache) {
        this.cache = cache;
    }

    /** Retorna tamanho atual, limite, hits, misses, evictions e hit ratio. */
    @GetMapping("/estatisticas")
    public EstatisticasCache estatisticas() {
        return cache.estatisticas();
    }

    /** Limpa o cache. */
    @DeleteMapping
    public ResponseEntity<Void> limpar() {
        cache.limpar();
        return ResponseEntity.noContent().build();
    }
}
