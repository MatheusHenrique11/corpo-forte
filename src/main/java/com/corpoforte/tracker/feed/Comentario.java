package com.corpoforte.tracker.feed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Comentario de um usuario em um post - que pode ser o post de outra
 * pessoa, e e' justamente esse o ponto (ver Post.java sobre a excecao
 * deliberada ao isolamento por usuario da Fase 6).
 *
 * postId e usuarioId sao Long soltos, sem @ManyToOne, igual ao resto do
 * projeto: usuarioId aqui so' responde "quem escreveu", nunca "quem pode
 * ler".
 */
@Entity
@Table(name = "comentario")
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private String texto;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Comentario() {
    }

    public Comentario(Long postId, Long usuarioId, String texto, LocalDateTime criadoEm) {
        this.postId = postId;
        this.usuarioId = usuarioId;
        this.texto = texto;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getTexto() {
        return texto;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
