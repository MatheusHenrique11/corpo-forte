package com.corpoforte.tracker.peso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/**
 * Primeira tabela de historico do projeto: uma linha por dia por usuario
 * (upsert por data - pesar de novo no mesmo dia atualiza o registro daquele
 * dia, nao cria um segundo). usuarioId e' Long solto, sem @ManyToOne, mesmo
 * padrao de AvaliacaoFisica.
 */
@Entity
@Table(name = "registro_peso", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "data"}))
public class RegistroPeso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "peso_kg", nullable = false)
    private double pesoKg;

    protected RegistroPeso() {
    }

    public RegistroPeso(Long usuarioId, LocalDate data, double pesoKg) {
        this.usuarioId = usuarioId;
        this.data = data;
        this.pesoKg = pesoKg;
    }

    public void atualizarPeso(double pesoKg) {
        this.pesoKg = pesoKg;
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

    public double getPesoKg() {
        return pesoKg;
    }
}
