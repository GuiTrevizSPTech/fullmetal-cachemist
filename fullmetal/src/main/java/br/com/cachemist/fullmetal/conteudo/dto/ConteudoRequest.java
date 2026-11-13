package br.com.cachemist.fullmetal.conteudo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConteudoRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotBlank @Size(max = 2000) String corpo,
        @NotBlank @Size(max = 255) String autor
) {
}
