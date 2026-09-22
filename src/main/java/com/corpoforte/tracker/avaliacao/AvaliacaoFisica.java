package com.corpoforte.tracker.avaliacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Um registro por usuario, sobrescrito a cada novo teste (sem historico
 * ainda - tendencia ao longo do tempo e' escopo da Fase 3, so pra peso).
 * usuarioId e' um Long simples, sem @ManyToOne: mantem a entidade isolada e
 * testavel sem carregar Usuario junto, no mesmo espirito de simplicidade do
 * resto do projeto, que ainda nao usa relacoes JPA em lugar nenhum.
 */
@Entity
@Table(name = "avaliacao_fisica")
public class AvaliacaoFisica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
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
