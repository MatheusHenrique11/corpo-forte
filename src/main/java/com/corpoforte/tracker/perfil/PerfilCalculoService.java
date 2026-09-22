package com.corpoforte.tracker.perfil;

import com.corpoforte.tracker.usuario.ObjetivoTreino;
import org.springframework.stereotype.Service;

/**
 * Formulas de TMB/TDEE/IMC/macros, portadas do prototipo HTML ja validado
 * pelo usuario (Mifflin-St Jeor + fator de atividade 1.55 + deficit/
 * superavit fixo). Fica isolado do Controller de proposito: sem Spring MVC
 * nem banco no meio, da pra testar so chamando calcular(...) direto.
 */
@Service
public class PerfilCalculoService {

    private static final double FATOR_ATIVIDADE_MODERADA = 1.55;
    private static final double DEFICIT_PERDA_GORDURA_KCAL = 500;
    private static final double SUPERAVIT_GANHO_MASSA_KCAL = 300;

    public PerfilResultado calcular(double pesoKg, double alturaCm, int idade, ObjetivoTreino objetivo) {
        double alturaM = alturaCm / 100.0;

        double tmb = 10 * pesoKg + 6.25 * alturaCm - 5 * idade + 5;
        double tdee = tmb * FATOR_ATIVIDADE_MODERADA;
        double imc = pesoKg / (alturaM * alturaM);

        double metaCalorica;
        String rotuloMeta;
        double proteinaGPorKg;
        double gorduraGPorKg;

        if (objetivo == ObjetivoTreino.PERDA_GORDURA) {
            metaCalorica = tdee - DEFICIT_PERDA_GORDURA_KCAL;
            rotuloMeta = "Deficit ~500 kcal (perda de gordura)";
            proteinaGPorKg = 2.0;
            gorduraGPorKg = 0.8;
        } else {
            metaCalorica = tdee + SUPERAVIT_GANHO_MASSA_KCAL;
            rotuloMeta = "Superavit ~300 kcal (ganho de massa)";
            proteinaGPorKg = 1.8;
            gorduraGPorKg = 1.0;
        }

        double proteinaG = proteinaGPorKg * pesoKg;
        double gorduraG = gorduraGPorKg * pesoKg;
        double kcalProteinaEGordura = proteinaG * 4 + gorduraG * 9;
        double carboidratoG = Math.max(0, (metaCalorica - kcalProteinaEGordura) / 4);

        return new PerfilResultado(
                tmb, tdee, imc, ClassificacaoImc.classificar(imc),
                metaCalorica, rotuloMeta, proteinaG, gorduraG, carboidratoG
        );
    }
}
