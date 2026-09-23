package com.corpoforte.tracker.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * "Este login (provedor + sub) pertence a este usuario". Substituiu
 * usuario.google_sub na Fase 10 (V12): a conta e' uma so', e cada forma de
 * entrar nela e' uma linha aqui.
 *
 * usuarioId e' Long solto, sem @ManyToOne, igual ao resto do projeto. Sem
 * mutator nenhum: vincular de novo e' linha nova, nunca trocar o dono de
 * uma identidade existente.
 */
@Entity
@Table(name = "identidade_externa",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provedor", "sub"}))
public class IdentidadeExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provedor provedor;

    @Column(nullable = false)
    private String sub;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected IdentidadeExterna() {
    }

    public IdentidadeExterna(Long usuarioId, Provedor provedor, String sub, LocalDateTime criadoEm) {
        this.usuarioId = usuarioId;
        this.provedor = provedor;
        this.sub = sub;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Provedor getProvedor() {
        return provedor;
    }

    public String getSub() {
        return sub;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
