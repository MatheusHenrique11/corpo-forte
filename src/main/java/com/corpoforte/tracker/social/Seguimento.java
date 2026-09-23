package com.corpoforte.tracker.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * "seguidorId segue seguidoId". Sem aprovacao e sem estado: a linha
 * existir e' o seguimento, e deixar de seguir apaga - mesmo modelo da
 * Curtida. Ids soltos, sem @ManyToOne, como no resto do projeto.
 */
@Entity
@Table(name = "seguimento",
        uniqueConstraints = @UniqueConstraint(columnNames = {"seguidor_id", "seguido_id"}))
public class Seguimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seguidor_id", nullable = false)
    private Long seguidorId;

    @Column(name = "seguido_id", nullable = false)
    private Long seguidoId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Seguimento() {
    }

    public Seguimento(Long seguidorId, Long seguidoId, LocalDateTime criadoEm) {
        this.seguidorId = seguidorId;
        this.seguidoId = seguidoId;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Long getSeguidorId() {
        return seguidorId;
    }

    public Long getSeguidoId() {
        return seguidoId;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
