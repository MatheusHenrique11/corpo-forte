package com.corpoforte.tracker.atividade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Um treino realizado (Fase 16) - o lado "diario" do app. Privada por
 * natureza: so' a dona ve, como o registro de peso. As series ficam em
 * AtividadeSerie; os ids sao soltos, sem @OneToMany, como no resto do
 * projeto.
 *
 * Nao participa da periodizacao: o ciclo da Fase 8 continua avancando so'
 * pelos itens do treino do dia marcados como concluidos. Treino livre e'
 * registro, nao programa prescrito.
 */
@Entity
@Table(name = "atividade")
public class Atividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemAtividade origem;

    @Column(name = "treino_do_dia_id")
    private Long treinoDoDiaId;

    @Column(name = "duracao_minutos")
    private Integer duracaoMinutos;

    @Column(name = "esforco_percebido")
    private Integer esforcoPercebido;

    private String notas;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Atividade() {
    }

    public Atividade(Long usuarioId, LocalDate data, OrigemAtividade origem, Long treinoDoDiaId,
                     Integer duracaoMinutos, Integer esforcoPercebido, String notas, LocalDateTime criadoEm) {
        this.usuarioId = usuarioId;
        this.data = data;
        this.origem = origem;
        this.treinoDoDiaId = treinoDoDiaId;
        this.duracaoMinutos = duracaoMinutos;
        this.esforcoPercebido = esforcoPercebido;
        this.notas = notas;
        this.criadoEm = criadoEm;
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

    public OrigemAtividade getOrigem() {
        return origem;
    }

    public Long getTreinoDoDiaId() {
        return treinoDoDiaId;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public Integer getEsforcoPercebido() {
        return esforcoPercebido;
    }

    public String getNotas() {
        return notas;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
