package br.com.cachemist.fullmetal.biblioteca;

import br.com.cachemist.fullmetal.conteudo.Conteudo;
import br.com.cachemist.fullmetal.conteudo.ConteudoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ao subir a aplicacao, coloca 7 circulos de transmutacao na biblioteca (se ela estiver vazia),
 * para a demonstracao ja comecar com dados.
 */
@Component
public class PovoarBiblioteca implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PovoarBiblioteca.class);

    private final ConteudoRepository repository;

    public PovoarBiblioteca(ConteudoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        List<Conteudo> circulos = List.of(
                new Conteudo("Circulo basico de transmutacao",
                        "Compreensao, decomposicao e reconstrucao. A base de toda alquimia.", "Izumi Curtis"),
                new Conteudo("Alquimia das chamas",
                        "Controle da concentracao de oxigenio para gerar combustao a distancia.", "Roy Mustang"),
                new Conteudo("Alquimia do gelo",
                        "Manipulacao da agua presente no ar para congelar o ambiente.", "Isaac McDougal"),
                new Conteudo("Transmutacao de armadura",
                        "Selo de sangue que prende uma alma a uma armadura.", "Edward Elric"),
                new Conteudo("Alquimia de bombas",
                        "Transmutacao de materia em explosivos pelo toque.", "Solf J. Kimblee"),
                new Conteudo("Alkahestria",
                        "Alquimia de Xing, que usa o fluxo do Dragao para cura e ataque a distancia.", "Mei Chang"),
                new Conteudo("Alquimia do aco",
                        "Transmutacao de metais para criar laminas e estruturas.", "Alex Louis Armstrong")
        );
        repository.saveAll(circulos);
        log.info("Biblioteca de Amestris aberta com {} circulos de transmutacao", circulos.size());
    }
}
