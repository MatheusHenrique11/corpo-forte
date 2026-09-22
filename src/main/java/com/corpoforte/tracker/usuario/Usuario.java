package com.corpoforte.tracker.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Conta do usuario. email/googleSub ficam nullable por enquanto porque o
 * login com Google so entra numa fase futura (hoje o sistema roda com um
 * unico usuario "local"); quando o login chegar, essas colunas passam a
 * ser preenchidas sem precisar de uma migration destrutiva.
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

    @Column(name = "google_sub", unique = true)
    private String googleSub;

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

    @Column(name = "data_inicio_ciclo", nullable = false)
    private LocalDate dataInicioCiclo;

    // construtor sem argumentos exigido pelo JPA/Hibernate para instanciar
    // a entidade via reflection ao carregar do banco
    protected Usuario() {
    }

    public Usuario(String nome, double pesoKg, double alturaCm, int idade,
                    ObjetivoTreino objetivo, NivelTreino nivel, LocalDate dataInicioCiclo) {
        this.nome = nome;
        this.pesoKg = pesoKg;
        this.alturaCm = alturaCm;
        this.idade = idade;
        this.objetivo = objetivo;
        this.nivel = nivel;
        this.dataInicioCiclo = dataInicioCiclo;
    }

    public void atualizarPerfil(String nome, double pesoKg, double alturaCm, int idade,
                                 ObjetivoTreino objetivo, NivelTreino nivel) {
        this.nome = nome;
        this.pesoKg = pesoKg;
        this.alturaCm = alturaCm;
        this.idade = idade;
        this.objetivo = objetivo;
        this.nivel = nivel;
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

    public String getGoogleSub() {
        return googleSub;
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

    public LocalDate getDataInicioCiclo() {
        return dataInicioCiclo;
    }
}
