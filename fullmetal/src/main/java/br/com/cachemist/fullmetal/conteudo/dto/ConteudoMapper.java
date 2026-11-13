package br.com.cachemist.fullmetal.conteudo.dto;

import br.com.cachemist.fullmetal.conteudo.Conteudo;

public final class ConteudoMapper {

    private ConteudoMapper() {
    }

    public static Conteudo toEntity(ConteudoRequest request) {
        return new Conteudo(request.titulo(), request.corpo(), request.autor());
    }

    public static ConteudoResponse toResponse(Conteudo conteudo) {
        return new ConteudoResponse(
                conteudo.getId(),
                conteudo.getTitulo(),
                conteudo.getCorpo(),
                conteudo.getAutor(),
                conteudo.getAtualizadoEm()
        );
    }
}
