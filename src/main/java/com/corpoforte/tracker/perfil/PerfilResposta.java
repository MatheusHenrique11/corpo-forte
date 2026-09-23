package com.corpoforte.tracker.perfil;

import com.corpoforte.tracker.usuario.NivelTreino;
import com.corpoforte.tracker.usuario.ObjetivoTreino;
import com.corpoforte.tracker.usuario.Usuario;

/**
 * Perfil do proprio usuario, com o calculo derivado (TMB, TDEE, IMC,
 * macros). Dado corporal: so' existe no GET do proprio dono, nunca em
 * resposta que outra pessoa veja.
 *
 * Os enums saem como codigo (PERDA_GORDURA, SOBREPESO...), sem rotulo de
 * tela: texto de exibicao e' decisao de cada cliente.
 */
public record PerfilResposta(String nome, double pesoKg, double alturaCm, int idade,
                             ObjetivoTreino objetivo, NivelTreino nivel, CalculoNutricional calculo) {

    public record CalculoNutricional(double tmb, double tdee, double imc, ClassificacaoImc classificacaoImc,
                                     double metaCalorica, double proteinaG, double gorduraG,
                                     double carboidratoG) {
    }

    static PerfilResposta de(Usuario usuario, PerfilResultado resultado) {
        return new PerfilResposta(usuario.getNome(), usuario.getPesoKg(), usuario.getAlturaCm(), usuario.getIdade(),
                usuario.getObjetivo(), usuario.getNivel(),
                new CalculoNutricional(resultado.tmb(), resultado.tdee(), resultado.imc(), resultado.classificacaoImc(),
                        resultado.metaCalorica(), resultado.proteinaG(), resultado.gorduraG(),
                        resultado.carboidratoG()));
    }
}
