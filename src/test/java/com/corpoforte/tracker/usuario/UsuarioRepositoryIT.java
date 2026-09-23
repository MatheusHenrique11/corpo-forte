package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confirma que "um google_sub por conta" e' constraint de verdade no banco
 * (V1__create_usuario.sql: "google_sub varchar(120) unique"), nao so uma
 * suposicao lendo o SQL - mesmo padrao ja usado pra registro_peso (Fase 3)
 * e treino_do_dia (Fase 5). Sem isso, duas contas concorrentes reivindicando
 * a mesma conta local orfa ao mesmo tempo poderiam ambas conseguir salvar.
 */
@Transactional
class UsuarioRepositoryIT extends IntegrationTestBase {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void bancoRejeitaDoisUsuariosComOMesmoGoogleSub() {
        Usuario primeiro = new Usuario("Primeira Conta", 70, 170, 25,
                ObjetivoTreino.PERDA_GORDURA, NivelTreino.INICIANTE, LocalDate.now());
        primeiro.vincularConta("sub-duplicado", "primeira@exemplo.com");
        usuarioRepository.saveAndFlush(primeiro);

        Usuario segundo = new Usuario("Segunda Conta", 80, 180, 30,
                ObjetivoTreino.GANHO_MASSA, NivelTreino.AVANCADO, LocalDate.now());
        segundo.vincularConta("sub-duplicado", "segunda@exemplo.com");

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(segundo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
