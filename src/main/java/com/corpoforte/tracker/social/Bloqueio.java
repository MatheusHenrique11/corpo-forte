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
 * "bloqueadorId bloqueou bloqueadoId". O efeito vale nos dois sentidos
 * (RegraDeBloqueio), mas a linha guarda quem bloqueou: so' ele desbloqueia.
 * Sem estado, como Seguimento e Curtida: desbloquear apaga a linha.
 */
@Entity
@Table(name = "bloqueio",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bloqueador_id", "bloqueado_id"}))
public class Bloqueio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bloqueador_id", nullable = false)
    private Long bloqueadorId;

    @Column(name = "bloqueado_id", nullable = false)
    private Long bloqueadoId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Bloqueio() {
    }

    public Bloqueio(Long bloqueadorId, Long bloqueadoId, LocalDateTime criadoEm) {
        this.bloqueadorId = bloqueadorId;
        this.bloqueadoId = bloqueadoId;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Long getBloqueadorId() {
        return bloqueadorId;
    }

    public Long getBloqueadoId() {
        return bloqueadoId;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
