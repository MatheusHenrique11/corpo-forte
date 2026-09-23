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

    private static final int TAMANHO_MAXIMO_FOTO_URL = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true)
    private String email;

    /** Identidade publica (Fase 12). Unico sem diferenciar maiusculas
     * (indice em lower(username), V15); null ate o onboarding. */
    private String username;

    private String bio;

    /** Foto do provedor de login (claim "picture" do Google). */
    @Column(name = "foto_url")
    private String fotoUrl;

    /** Enquanto false, a API responde onboarding-pendente em quase tudo
     * (OnboardingPendenteInterceptor). Conta nova nasce com dado inventado
     * (100 kg, 178 cm...), e o onboarding e' o que troca isso por dado real. */
    @Column(name = "onboarding_concluido", nullable = false)
    private boolean onboardingConcluido = false;

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
     *
     * NENHUM e' descartado: e' o "equipamento" de exercicio de peso
     * corporal, sempre compativel (ExercicioFiltroService), nao algo que se
     * possui. A tela nunca oferecia essa opcao, mas a API aceita qualquer
     * valor do enum - a regra mora aqui pra valer nas duas portas.
     */
    public void atualizarEquipamentos(Set<Equipamento> equipamentos) {
        this.equipamentosDisponiveis = new HashSet<>(equipamentos);
        this.equipamentosDisponiveis.remove(Equipamento.NENHUM);
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

    /**
     * Onboarding (Fase 12): troca os dados inventados da conta nova pelos
     * reais e escolhe o username. O peso nao entra aqui: vira o primeiro
     * registro de peso (OnboardingService), a fonte unica desde a Fase 3.
     */
    public void concluirOnboarding(String nome, String username, double alturaCm, int idade,
                                   ObjetivoTreino objetivo, NivelTreino nivel) {
        atualizarPerfil(nome, alturaCm, idade, objetivo, nivel);
        this.username = username;
        this.onboardingConcluido = true;
    }

    public void atualizarPerfilPublico(String username, String bio) {
        this.username = username;
        this.bio = bio;
    }

    /**
     * Foto do provedor, atualizada a cada login - a pessoa troca a foto no
     * Google e a daqui acompanha. Login sem foto (cliente que nao pediu o
     * escopo "profile") nao apaga a que ja existe. Devolve se mudou, pra
     * quem chama so' gravar quando precisa.
     */
    public boolean atualizarFoto(String fotoUrl) {
        // acima do tamanho da coluna: fica a foto anterior em vez de o
        // login inteiro falhar por causa de um campo cosmetico
        if (fotoUrl == null || fotoUrl.length() > TAMANHO_MAXIMO_FOTO_URL || fotoUrl.equals(this.fotoUrl)) {
            return false;
        }
        this.fotoUrl = fotoUrl;
        return true;
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

    public String getUsername() {
        return username;
    }

    public String getBio() {
        return bio;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public boolean isOnboardingConcluido() {
        return onboardingConcluido;
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
