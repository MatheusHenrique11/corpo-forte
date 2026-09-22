package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.NivelTreino;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Primeira tabela de dado de referencia/catalogo do projeto: diferente de
 * Usuario/AvaliacaoFisica/RegistroPeso (estado por usuario), Exercicio e' a
 * mesma lista pra todo mundo, sem usuarioId. Populada via migration de
 * dado (V5__seed_exercicio.sql), nao criada em runtime.
 */
@Entity
@Table(name = "exercicio")
public class Exercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovimentoPadrao movimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelTreino nivel;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipamento_necessario", nullable = false)
    private Equipamento equipamentoNecessario;

    protected Exercicio() {
    }

    public Exercicio(String nome, MovimentoPadrao movimento, NivelTreino nivel, Equipamento equipamentoNecessario) {
        this.nome = nome;
        this.movimento = movimento;
        this.nivel = nivel;
        this.equipamentoNecessario = equipamentoNecessario;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public MovimentoPadrao getMovimento() {
        return movimento;
    }

    public NivelTreino getNivel() {
        return nivel;
    }

    public Equipamento getEquipamentoNecessario() {
        return equipamentoNecessario;
    }
}
