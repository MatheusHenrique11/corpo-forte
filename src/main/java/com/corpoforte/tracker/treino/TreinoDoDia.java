package com.corpoforte.tracker.treino;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/**
 * Um treino gerado por dia por usuario (upsert por data - mesmo mecanismo
 * de "um por dia" que RegistroPeso ja usa). usuarioId e' Long solto, sem
 * @ManyToOne, mesmo padrao do resto do projeto.
 */
@Entity
@Table(name = "treino_do_dia", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "data"}))
public class TreinoDoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDate data;

    protected TreinoDoDia() {
    }

    public TreinoDoDia(Long usuarioId, LocalDate data) {
        this.usuarioId = usuarioId;
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public LocalDate getData() {
        return data;
    }
}
