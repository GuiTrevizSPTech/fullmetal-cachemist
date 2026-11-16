package br.com.cachemist.fullmetal.conteudo;

import br.com.cachemist.fullmetal.biblioteca.BibliotecaLenta;
import br.com.cachemist.fullmetal.cache.ConteudoCache;
import br.com.cachemist.fullmetal.erro.ConteudoNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
    === GET ===
    1. Busca na memória (cache)
    2. Se não achar na memória, procura na biblioteca
    3. Se achou na biblioteca, guarda na memória - se não achou, throw ConteudoNaoEncontradoException

    === PUT/DELETE ===
    1. Atualiza o conteúdo diretamente no banco
    2. Invalida o conteúdo na memória
*/
@Service
public class ConteudoService {

    private static final Logger log = LoggerFactory.getLogger(ConteudoService.class);

    private final ConteudoRepository repository;
    private final BibliotecaLenta biblioteca;
    private final ConteudoCache cache;

    public ConteudoService(ConteudoRepository repository, BibliotecaLenta biblioteca, ConteudoCache cache) {
        this.repository = repository;
        this.biblioteca = biblioteca;
        this.cache = cache;
    }

    public ResultadoBusca buscarPorId(Long id) {
        long inicio = System.nanoTime();

        Optional<Conteudo> daMemoria = cache.buscar(id);
        if (daMemoria.isPresent()) {
            log.info("Cache HIT  - id={} - {} ms", id, decorridoMs(inicio));
            return new ResultadoBusca(daMemoria.get(), true);
        }

        Conteudo daBiblioteca = biblioteca.consultar(id)
                .orElseThrow(() -> new ConteudoNaoEncontradoException(id));
        cache.guardar(daBiblioteca);
        log.info("Cache MISS - id={} - {} ms", id, decorridoMs(inicio));
        return new ResultadoBusca(daBiblioteca, false);
    }

    public List<Conteudo> listar() {
        return repository.findAll();
    }

    public Conteudo criar(Conteudo novo) {
        return repository.save(novo);
    }

    public Conteudo atualizar(Long id, Conteudo dados) {
        Conteudo existente = repository.findById(id)
                .orElseThrow(() -> new ConteudoNaoEncontradoException(id));
        existente.setTitulo(dados.getTitulo());
        existente.setCorpo(dados.getCorpo());
        existente.setAutor(dados.getAutor());
        Conteudo salvo = repository.save(existente);
        cache.invalidar(id); // revisao externa: a lembranca antiga nao vale mais
        return salvo;
    }

    public void remover(Long id) {
        if (!repository.existsById(id)) {
            throw new ConteudoNaoEncontradoException(id);
        }
        repository.deleteById(id);
        cache.invalidar(id); // proibido por Amestris (ordem oficial do governo): some da memoria tambem
    }

    private static long decorridoMs(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
