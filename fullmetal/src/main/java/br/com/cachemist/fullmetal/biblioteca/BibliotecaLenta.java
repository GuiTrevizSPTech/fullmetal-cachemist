package br.com.cachemist.fullmetal.biblioteca;

import br.com.cachemist.fullmetal.conteudo.Conteudo;
import br.com.cachemist.fullmetal.conteudo.ConteudoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * A "fonte lenta" - representa uma biblioteca com circulos de transmutacao de Amestris.
 * Toda consulta aqui demora (Thread.sleep). A memoria do alquimista (o cache) tenta evitar.
 */
@Component
public class BibliotecaLenta {

    private final ConteudoRepository repository;
    private final long atrasoMs;

    public BibliotecaLenta(ConteudoRepository repository,
                           @Value("${app.biblioteca.atraso-ms:2000}") long atrasoMs) {
        this.repository = repository;
        this.atrasoMs = atrasoMs;
    }

    public Optional<Conteudo> consultar(Long id) {
        simularDemora();
        return repository.findById(id);
    }

    private void simularDemora() {
        try {
            Thread.sleep(atrasoMs);
        } catch (InterruptedException e) {
            // Boa pratica: restaurar a flag de interrupcao em vez de engolir a excecao.
            Thread.currentThread().interrupt();
        }
    }
}
