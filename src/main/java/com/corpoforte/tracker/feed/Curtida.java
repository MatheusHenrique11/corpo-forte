package com.corpoforte.tracker.feed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Uma curtida por usuario por post - curtir de novo remove a linha
 * (PostService.alternarCurtida), entao a entidade nao tem estado "ativa":
 * a linha existir E' a curtida.
 *
 * Sem campo mutavel nenhum de proposito: e' o modelo mais simples que
 * torna impossivel um "descurtido" ficar contando como curtida por
 * engano.
 */
@Entity
@Table(name = "curtida",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "usuario_id"}))
public class Curtida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Curtida() {
    }

    public Curtida(Long postId, Long usuarioId, LocalDateTime criadoEm) {
        this.postId = postId;
        this.usuarioId = usuarioId;
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

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
