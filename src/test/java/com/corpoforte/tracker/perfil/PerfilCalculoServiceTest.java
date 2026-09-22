package com.corpoforte.tracker.perfil;

import com.corpoforte.tracker.usuario.ObjetivoTreino;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Casos de referencia: mesmos numeros que o prototipo HTML ja validado
 * produzia para peso=100kg / altura=178cm / idade=27 / perda de gordura.
 * Isso funciona como teste de regressao entre o prototipo e o backend.
 */
class PerfilCalculoServiceTest {

    private final PerfilCalculoService service = new PerfilCalculoService();

    @Test
    void calculaTmbTdeeImcEMacrosParaPerdaDeGordura() {
        PerfilResultado resultado = service.calcular(100, 178, 27, ObjetivoTreino.PERDA_GORDURA);

        assertThat(resultado.tmb()).isCloseTo(1982.5, within(0.01));
        assertThat(resultado.tdee()).isCloseTo(3072.875, within(0.01));
        assertThat(resultado.imc()).isCloseTo(31.56, within(0.01));
        assertThat(resultado.classificacaoImc()).isEqualTo(ClassificacaoImc.OBESIDADE_GRAU_I);
        assertThat(resultado.metaCalorica()).isCloseTo(2572.875, within(0.01));
        assertThat(resultado.proteinaG()).isCloseTo(200.0, within(0.01));
        assertThat(resultado.gorduraG()).isCloseTo(80.0, within(0.01));
        assertThat(resultado.carboidratoG()).isCloseTo(263.22, within(0.01));
    }

    @Test
    void usaSuperavitEMacrosDiferentesParaGanhoDeMassa() {
        PerfilResultado resultado = service.calcular(70, 175, 25, ObjetivoTreino.GANHO_MASSA);

        double tmbEsperado = 10 * 70 + 6.25 * 175 - 5 * 25 + 5;
        double tdeeEsperado = tmbEsperado * 1.55;

        assertThat(resultado.tmb()).isCloseTo(tmbEsperado, within(0.01));
        assertThat(resultado.tdee()).isCloseTo(tdeeEsperado, within(0.01));
        assertThat(resultado.metaCalorica()).isCloseTo(tdeeEsperado + 300, within(0.01));
        assertThat(resultado.proteinaG()).isCloseTo(1.8 * 70, within(0.01));
        assertThat(resultado.gorduraG()).isCloseTo(1.0 * 70, within(0.01));
    }

    @Test
    void carboidratoNuncaFicaNegativoMesmoComMetaBaixa() {
        PerfilResultado resultado = service.calcular(150, 150, 60, ObjetivoTreino.PERDA_GORDURA);

        assertThat(resultado.carboidratoG()).isGreaterThanOrEqualTo(0);
    }
}
