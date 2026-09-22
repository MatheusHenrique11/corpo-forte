package com.corpoforte.tracker.perfil;

import com.corpoforte.tracker.usuario.NivelTreino;
import com.corpoforte.tracker.usuario.ObjetivoTreino;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO ligado ao <form> do Thymeleaf (nao e' a entidade JPA). As anotacoes
 * de bean validation sao a "regra de negocio rigida" pedida: o Controller
 * nunca chega a chamar o calculo com um valor fora dessas faixas.
 */
public class PerfilForm {

    @NotBlank(message = "Informe seu nome")
    private String nome;

    @NotNull(message = "Informe seu peso")
    @DecimalMin(value = "30.0", message = "Peso minimo: 30 kg")
    @DecimalMax(value = "300.0", message = "Peso maximo: 300 kg")
    private Double pesoKg;

    @NotNull(message = "Informe sua altura")
    @DecimalMin(value = "100.0", message = "Altura minima: 100 cm")
    @DecimalMax(value = "250.0", message = "Altura maxima: 250 cm")
    private Double alturaCm;

    @NotNull(message = "Informe sua idade")
    @Min(value = 10, message = "Idade minima: 10 anos")
    @Max(value = 100, message = "Idade maxima: 100 anos")
    private Integer idade;

    @NotNull(message = "Escolha um objetivo")
    private ObjetivoTreino objetivo;

    @NotNull(message = "Escolha um nivel de treino")
    private NivelTreino nivel;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public Double getAlturaCm() {
        return alturaCm;
    }

    public void setAlturaCm(Double alturaCm) {
        this.alturaCm = alturaCm;
    }

    public Integer getIdade() {
        return idade;
    }

    public void setIdade(Integer idade) {
        this.idade = idade;
    }

    public ObjetivoTreino getObjetivo() {
        return objetivo;
    }

    public void setObjetivo(ObjetivoTreino objetivo) {
        this.objetivo = objetivo;
    }

    public NivelTreino getNivel() {
        return nivel;
    }

    public void setNivel(NivelTreino nivel) {
        this.nivel = nivel;
    }
}
