package com.corpoforte.tracker.avaliacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/**
 * Historico de avaliacoes desde a Fase 9: uma linha por dia por usuario
 * (upsert por data - corrigir um numero no mesmo dia atualiza a medicao do
 * dia em vez de criar uma falsa). Ate a Fase 8 era uma linha por usuario,
 * sobrescrita a cada teste; a Fase 8 passou a pedir reavaliacao ao fim do
 * ciclo, e obedecer esse aviso apagava a medicao anterior - justamente a
 * que torna visivel o progresso.
 *
 * "Avaliacao atual" = a mais recente por data (ver
 * AvaliacaoFisicaService.obterMaisRecenteDoUsuario), inclusive pro ciclo da
 * periodizacao, que e' ancorado nessa data.
 *
 * usuarioId e' um Long simples, sem @ManyToOne: mantem a entidade isolada e
 * testavel sem carregar Usuario junto, no mesmo espirito de simplicidade do
 * resto do projeto, que ainda nao usa relacoes JPA em lugar nenhum.
 */
@Entity
@Table(name = "avaliacao_fisica",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "data_avaliacao"}))
public class AvaliacaoFisica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "data_avaliacao", nullable = false)
    private LocalDate dataAvaliacao;

    @Column(name = "reps_puxar_vertical", nullable = false)
    private int repsPuxarVertical;

    @Column(name = "reps_empurrar_vertical", nullable = false)
    private int repsEmpurrarVertical;

    @Column(name = "reps_pernas_bilateral", nullable = false)
    private int repsPernasBilateral;

    @Column(name = "reps_puxar_horizontal", nullable = false)
    private int repsPuxarHorizontal;

    @Column(name = "reps_empurrar_horizontal", nullable = false)
    private int repsEmpurrarHorizontal;

    @Column(name = "reps_pernas_unilateral", nullable = false)
    private int repsPernasUnilateral;

    protected AvaliacaoFisica() {
    }

    public AvaliacaoFisica(Long usuarioId, int repsPuxarVertical, int repsEmpurrarVertical,
                            int repsPernasBilateral, int repsPuxarHorizontal, int repsEmpurrarHorizontal,
                            int repsPernasUnilateral, LocalDate dataAvaliacao) {
        this.usuarioId = usuarioId;
        this.repsPuxarVertical = repsPuxarVertical;
        this.repsEmpurrarVertical = repsEmpurrarVertical;
        this.repsPernasBilateral = repsPernasBilateral;
        this.repsPuxarHorizontal = repsPuxarHorizontal;
        this.repsEmpurrarHorizontal = repsEmpurrarHorizontal;
        this.repsPernasUnilateral = repsPernasUnilateral;
        this.dataAvaliacao = dataAvaliacao;
    }

    public void atualizar(int repsPuxarVertical, int repsEmpurrarVertical, int repsPernasBilateral,
                           int repsPuxarHorizontal, int repsEmpurrarHorizontal, int repsPernasUnilateral,
                           LocalDate dataAvaliacao) {
        this.repsPuxarVertical = repsPuxarVertical;
        this.repsEmpurrarVertical = repsEmpurrarVertical;
        this.repsPernasBilateral = repsPernasBilateral;
        this.repsPuxarHorizontal = repsPuxarHorizontal;
        this.repsEmpurrarHorizontal = repsEmpurrarHorizontal;
        this.repsPernasUnilateral = repsPernasUnilateral;
        this.dataAvaliacao = dataAvaliacao;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public LocalDate getDataAvaliacao() {
        return dataAvaliacao;
    }

    public int getRepsPuxarVertical() {
        return repsPuxarVertical;
    }

    public int getRepsEmpurrarVertical() {
        return repsEmpurrarVertical;
    }

    public int getRepsPernasBilateral() {
        return repsPernasBilateral;
    }

    public int getRepsPuxarHorizontal() {
        return repsPuxarHorizontal;
    }

    public int getRepsEmpurrarHorizontal() {
        return repsEmpurrarHorizontal;
    }

    public int getRepsPernasUnilateral() {
        return repsPernasUnilateral;
    }
}
