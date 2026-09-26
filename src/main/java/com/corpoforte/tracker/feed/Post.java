package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.usuario.Visibilidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Primeira entidade do projeto que e' intencionalmente compartilhada entre
 * usuarios - o feed mostra posts de todo mundo, de proposito. Diferente do
 * resto do sistema (Usuario/AvaliacaoFisica/RegistroPeso/TreinoDoDia), que
 * a Fase 6 garante ser estritamente isolado por usuario. usuarioId e' Long
 * solto, sem @ManyToOne, mesmo padrao do resto do projeto - so pra saber
 * quem postou, nao pra restringir quem le.
 */
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private String texto;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    /** Quem pode ver (Fase 14). Quem aplica e' RegraDeVisibilidade, em toda
     * consulta de post - nunca um if solto no codigo. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibilidade visibilidade = Visibilidade.PUBLICO;

    protected Post() {
    }

    public Post(Long usuarioId, String texto, LocalDateTime criadoEm) {
        this(usuarioId, texto, criadoEm, Visibilidade.PUBLICO);
    }

    public Post(Long usuarioId, String texto, LocalDateTime criadoEm, Visibilidade visibilidade) {
        this.usuarioId = usuarioId;
        this.texto = texto;
        this.criadoEm = criadoEm;
        this.visibilidade = visibilidade;
    }

    public Long getId() {
        return id;
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

    public Visibilidade getVisibilidade() {
        return visibilidade;
    }
}
