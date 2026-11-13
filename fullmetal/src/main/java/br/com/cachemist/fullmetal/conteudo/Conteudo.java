package br.com.cachemist.fullmetal.conteudo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Círculo de Transmutação */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Conteudo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String corpo;

    @Column(nullable = false)
    private String autor;

    private LocalDateTime atualizadoEm;

    public Conteudo(String titulo, String corpo, String autor) {
        this.titulo = titulo;
        this.corpo = corpo;
        this.autor = autor;
    }

    @PrePersist
    @PreUpdate
    void marcarAtualizacao() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
