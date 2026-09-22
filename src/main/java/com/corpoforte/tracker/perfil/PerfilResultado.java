package com.corpoforte.tracker.perfil;

/**
 * "record" = classe imutavel do Java so para carregar dados: o compilador
 * gera construtor, getters (tmb(), tdee(), ...), equals/hashCode/toString
 * sozinho. Serve bem aqui porque PerfilResultado e' so o resultado de um
 * calculo, sem comportamento proprio.
 */
public record PerfilResultado(
        double tmb,
        double tdee,
        double imc,
        ClassificacaoImc classificacaoImc,
        double metaCalorica,
        String rotuloMeta,
        double proteinaG,
        double gorduraG,
        double carboidratoG
) {
}
