package br.com.cachemist.fullmetal.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testes unitários do cache: HIT/MISS, limite (LRU e FIFO), TTL, invalidação e estatísticas */
class CacheSimplesTest {

    private static final Duration TTL = Duration.ofSeconds(60);

    private RelogioFalso relogio;

    @BeforeEach
    void preparar() {
        relogio = new RelogioFalso();
    }

    private CacheSimples<Long, String> novoCache(int maxItens, PoliticaRemocao politica) {
        return new CacheSimples<>(maxItens, TTL, politica, relogio);
    }

    private void guardarEmOrdem(CacheSimples<Long, String> cache, long... chaves) {
        for (long chave : chaves) {
            cache.guardar(chave, "circulo " + chave);
            relogio.avancar(Duration.ofSeconds(1));
        }
    }

    // ===== HIT / MISS =====

    @Test
    @DisplayName("Primeira busca é MISS; depois de guardar, a busca é HIT")
    void missDepoisHit() {
        CacheSimples<Long, String> cache = novoCache(5, PoliticaRemocao.LRU);

        assertThat(cache.buscar(1L)).isEmpty();
        cache.guardar(1L, "circulo 1");
        assertThat(cache.buscar(1L)).contains("circulo 1");

        EstatisticasCache stats = cache.estatisticas();
        assertThat(stats.misses()).isEqualTo(1);
        assertThat(stats.hits()).isEqualTo(1);
        assertThat(stats.hitRatio()).isEqualTo(0.5);
    }

    // ===== LIMITE =====

    @Test
    @DisplayName("Nunca passa do limite de itens")
    void respeitaLimite() {
        CacheSimples<Long, String> cache = novoCache(5, PoliticaRemocao.LRU);

        guardarEmOrdem(cache, 1, 2, 3, 4, 5, 6, 7);

        assertThat(cache.estatisticas().tamanho()).isEqualTo(5);
        assertThat(cache.estatisticas().evictions()).isEqualTo(2);
    }

    @Test
    @DisplayName("LRU: esquece o item com o último acesso mais antigo")
    void lruEsqueceMenosUsadoRecentemente() {
        CacheSimples<Long, String> cache = novoCache(3, PoliticaRemocao.LRU);
        guardarEmOrdem(cache, 1, 2, 3);

        cache.buscar(1L);
        relogio.avancar(Duration.ofSeconds(1));
        cache.guardar(4L, "circulo 4");

        assertThat(cache.buscar(2L)).isEmpty();
        assertThat(cache.buscar(1L)).isPresent();
        assertThat(cache.buscar(3L)).isPresent();
        assertThat(cache.buscar(4L)).isPresent();
    }

    @Test
    @DisplayName("FIFO: esquece o item guardado há mais tempo, mesmo que tenha sido usado agora")
    void fifoEsqueceMaisAntigo() {
        CacheSimples<Long, String> cache = novoCache(3, PoliticaRemocao.FIFO);
        guardarEmOrdem(cache, 1, 2, 3);

        cache.buscar(1L);
        relogio.avancar(Duration.ofSeconds(1));
        cache.guardar(4L, "circulo 4");

        assertThat(cache.buscar(1L)).isEmpty();
        assertThat(cache.buscar(2L)).isPresent();
    }

    @Test
    @DisplayName("Guardar de novo uma chave que já existe não gera eviction")
    void regravarNaoRemoveOutro() {
        CacheSimples<Long, String> cache = novoCache(2, PoliticaRemocao.LRU);
        guardarEmOrdem(cache, 1, 2);

        cache.guardar(2L, "circulo 2 revisado");

        assertThat(cache.estatisticas().evictions()).isZero();
        assertThat(cache.buscar(2L)).contains("circulo 2 revisado");
    }

    // ===== TTL =====

    @Test
    @DisplayName("Item mais velho que o TTL vira MISS e sai da memória")
    void expiraPorTtl() {
        CacheSimples<Long, String> cache = novoCache(5, PoliticaRemocao.LRU);
        cache.guardar(1L, "circulo 1");

        relogio.avancar(TTL.plusSeconds(1));

        assertThat(cache.buscar(1L)).isEmpty();
        assertThat(cache.estatisticas().expiracoes()).isEqualTo(1);
        assertThat(cache.estatisticas().tamanho()).isZero();
    }

    @Test
    @DisplayName("Item ainda dentro do TTL continua sendo HIT")
    void naoExpiraAntesDoTtl() {
        CacheSimples<Long, String> cache = novoCache(5, PoliticaRemocao.LRU);
        cache.guardar(1L, "circulo 1");

        relogio.avancar(TTL.minusSeconds(1));

        assertThat(cache.buscar(1L)).isPresent();
    }

    @Test
    @DisplayName("TTL sem renovação: acessar não aumenta a validade")
    void acessoNaoRenovaTtl() {
        CacheSimples<Long, String> cache = novoCache(5, PoliticaRemocao.LRU);
        cache.guardar(1L, "circulo 1");

        relogio.avancar(Duration.ofSeconds(50));
        assertThat(cache.buscar(1L)).isPresent();
        relogio.avancar(Duration.ofSeconds(11));

        assertThat(cache.buscar(1L)).isEmpty();
    }

    @Test
    @DisplayName("Rotina de limpeza remove só os expirados")
    void removerExpirados() {
        CacheSimples<Long, String> cache = novoCache(5, PoliticaRemocao.LRU);
        cache.guardar(1L, "circulo 1");
        relogio.avancar(Duration.ofSeconds(30));
        cache.guardar(2L, "circulo 2");
        relogio.avancar(Duration.ofSeconds(31));

        int removidos = cache.removerExpirados();

        assertThat(removidos).isEqualTo(1);
        assertThat(cache.estatisticas().tamanho()).isEqualTo(1);
        assertThat(cache.estatisticas().misses()).isZero();
    }

    // ===== REMOÇÃO =====

    @Test
    @DisplayName("Invalidar remove só a chave pedida")
    void invalidar() {
        CacheSimples<Long, String> cache = novoCache(5, PoliticaRemocao.LRU);
        guardarEmOrdem(cache, 1, 2);

        cache.invalidar(1L);

        assertThat(cache.buscar(1L)).isEmpty();
        assertThat(cache.buscar(2L)).isPresent();
    }

    @Test
    @DisplayName("Limpar esvazia a memória, mas mantém os contadores")
    void limpar() {
        CacheSimples<Long, String> cache = novoCache(5, PoliticaRemocao.LRU);
        guardarEmOrdem(cache, 1, 2);
        cache.buscar(1L);

        cache.limpar();

        assertThat(cache.estatisticas().tamanho()).isZero();
        assertThat(cache.estatisticas().hits()).isEqualTo(1);
    }

    // ===== CONFIGURAÇÃO =====

    @Test
    @DisplayName("Limite zero ou negativo é recusado")
    void recusaLimiteInvalido() {
        assertThatThrownBy(() -> novoCache(0, PoliticaRemocao.LRU))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
