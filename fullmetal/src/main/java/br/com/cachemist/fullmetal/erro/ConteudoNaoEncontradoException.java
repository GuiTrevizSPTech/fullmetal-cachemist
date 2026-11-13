package br.com.cachemist.fullmetal.erro;

public class ConteudoNaoEncontradoException extends RuntimeException {

    public ConteudoNaoEncontradoException(Long id) {
        super("Circulo de transmutacao id=" + id + " nao existe na biblioteca de Amestris");
    }
}
