package com.corpoforte.tracker.usuario;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * Conta do usuario. email nasceu nullable na Fase 1 exatamente pra isso: a
 * Fase 6 (login com Google) preenche a coluna em contas ja existentes sem
 * precisar de migration destrutiva nenhuma. A identidade de login em si
 * (antes a coluna google_sub) mora em IdentidadeExterna desde a Fase 10.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "peso_kg", nullable = false)
    private double pesoKg;

    @Column(name = "altura_cm", nullable = false)
    private double alturaCm;

    @Column(nullable = false)
    private int idade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ObjetivoTreino objetivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelTreino nivel;

    // EAGER de proposito: com "open-in-view: false" nao ha sessao Hibernate
    // aberta fora do repository, e essa colecao e' lida no Controller (view
    // /exercicios, filtro de compatibilidade) - LAZY quebraria com
    // LazyInitializationException. Colecao pequena (no maximo 5 itens), sem
    // custo real de carregar sempre.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_equipamento", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "equipamento")
    private Set<Equipamento> equipamentosDisponiveis = new HashSet<>();

    // construtor sem argumentos exigido pelo JPA/Hibernate para instanciar
    // a entidade via reflection ao carregar do banco
    protected Usuario() {
    }

    public Usuario(String nome, double pesoKg, double alturaCm, int idade,
                    ObjetivoTreino objetivo, NivelTreino nivel) {
        this.nome = nome;
        this.pesoKg = pesoKg;
        this.alturaCm = alturaCm;
        this.idade = idade;
        this.objetivo = objetivo;
        this.nivel = nivel;
    }

    /**
     * Peso nao entra mais aqui a partir da Fase 3: o peso atual passa a vir
     * so do registro de peso (atualizarPeso), pra nao ter duas fontes de
     * verdade pro mesmo dado.
     */
    public void atualizarPerfil(String nome, double alturaCm, int idade,
                                 ObjetivoTreino objetivo, NivelTreino nivel) {
        this.nome = nome;
        this.alturaCm = alturaCm;
        this.idade = idade;
        this.objetivo = objetivo;
        this.nivel = nivel;
    }

    /**
     * Usado pelo modulo de registro de peso: o registro mais recente por
     * data vira o peso atual do perfil (usado no TMB/TDEE), sem precisar
     * passar pelo formulario de perfil inteiro.
     */
    public void atualizarPeso(double pesoKg) {
        this.pesoKg = pesoKg;
    }

    /**
     * Usado pelo modulo de equipamentos/catalogo de exercicios (Fase 4):
     * substitui o conjunto inteiro, nao acumula - marcar a tela de novo com
     * menos itens precisa remover o que foi desmarcado.
     */
    public void atualizarEquipamentos(Set<Equipamento> equipamentos) {
        this.equipamentosDisponiveis = new HashSet<>(equipamentos);
    }

    /**
     * Usado uma unica vez, no primeiro login (Fase 6): ou na conta
     * "reivindicada" (usuario local existente que ainda nao tinha login) ou
     * na criacao de uma conta nova. Nao e' chamado de novo em logins
     * seguintes - o nome que o usuario editar depois em /perfil nao e'
     * sobrescrito a cada login. O vinculo com o provedor em si e' uma
     * IdentidadeExterna, criada junto pelo UsuarioAtualService.
     */
    public void vincularEmail(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public double getAlturaCm() {
        return alturaCm;
    }

    public int getIdade() {
        return idade;
    }

    public ObjetivoTreino getObjetivo() {
        return objetivo;
    }

    public NivelTreino getNivel() {
        return nivel;
    }

    public Set<Equipamento> getEquipamentosDisponiveis() {
        return equipamentosDisponiveis;
    }
}
