package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * "Uma avaliacao por dia por usuario" tem que ser constraint de banco, nao
 * so o upsert do service - mesmo padrao ja usado pra registro_peso (Fase 3),
 * treino_do_dia (Fase 5) e usuario.google_sub (Fase 6). Insere direto pelo
 * repository, contornando AvaliacaoFisicaService.salvar de proposito.
 */
@Transactional
class AvaliacaoFisicaRepositoryIT extends IntegrationTestBase {

    @Autowired
    private AvaliacaoFisicaRepository avaliacaoFisicaRepository;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Test
    void bancoRejeitaDuasAvaliacoesNaMesmaDataParaOMesmoUsuario() {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-avaliacao-constraint", "Usuaria Teste", "constraint@exemplo.com"));
        LocalDate data = LocalDate.of(2026, 5, 4);

        avaliacaoFisicaRepository.saveAndFlush(
                new AvaliacaoFisica(usuario.getId(), 10, 15, 40, 20, 30, 50, data));

        assertThatThrownBy(() -> avaliacaoFisicaRepository.saveAndFlush(
                new AvaliacaoFisica(usuario.getId(), 11, 15, 40, 20, 30, 50, data)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duasAvaliacoesEmDatasDiferentesConvivemNoHistorico() {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-avaliacao-datas", "Usuaria Teste", "datas@exemplo.com"));

        avaliacaoFisicaRepository.saveAndFlush(
                new AvaliacaoFisica(usuario.getId(), 10, 15, 40, 20, 30, 50, LocalDate.of(2026, 5, 4)));
        avaliacaoFisicaRepository.saveAndFlush(
                new AvaliacaoFisica(usuario.getId(), 15, 15, 40, 20, 30, 50, LocalDate.of(2026, 6, 29)));

        assertThat(avaliacaoFisicaRepository.findByUsuarioIdOrderByDataAvaliacaoDesc(usuario.getId()))
                .hasSize(2);
    }
}
