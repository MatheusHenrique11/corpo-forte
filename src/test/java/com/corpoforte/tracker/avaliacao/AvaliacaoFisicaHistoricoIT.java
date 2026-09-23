package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O que a Fase 9 mudou de comportamento: refazer a avaliacao deixa de
 * apagar a anterior. Avaliacoes em datas antigas sao gravadas direto pelo
 * repository porque AvaliacaoFisicaService.salvar sempre usa a data de hoje
 * (e' o upsert do dia).
 */
@Transactional
class AvaliacaoFisicaHistoricoIT extends IntegrationTestBase {

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private AvaliacaoFisicaService avaliacaoFisicaService;

    @Autowired
    private AvaliacaoFisicaRepository avaliacaoFisicaRepository;

    private final OidcUser principal =
            OidcTestUsers.principal("sub-historico", "Usuaria Teste", "historico@exemplo.com");

    @Test
    void avaliacaoAntigaEhPreservadaEAMaisRecenteEhAAtual() {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);

        // ciclo passado: 10 barras
        avaliacaoFisicaRepository.save(new AvaliacaoFisica(
                usuario.getId(), 10, 15, 40, 20, 30, 50, LocalDate.now().minusWeeks(8)));
        // ciclo novo, hoje: 15 barras
        avaliacaoFisicaService.salvar(usuario.getId(), 15, 15, 40, 20, 30, 50);

        List<AvaliacaoFisica> historico = avaliacaoFisicaService.listarHistorico(usuario.getId());

        assertThat(historico).hasSize(2);
        assertThat(historico.get(0).getRepsPuxarVertical()).isEqualTo(15); // mais recente primeiro
        assertThat(historico.get(1).getRepsPuxarVertical()).isEqualTo(10);
        assertThat(avaliacaoFisicaService.obterMaisRecenteDoUsuario(usuario.getId()).orElseThrow()
                .getRepsPuxarVertical()).isEqualTo(15);
    }

    @Test
    void refazerNoMesmoDiaCorrigeAMedicaoEmVezDeCriarUmaFalsa() {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);

        avaliacaoFisicaService.salvar(usuario.getId(), 10, 15, 40, 20, 30, 50);
        // digitou 1 no lugar de 10 e corrigiu logo em seguida
        avaliacaoFisicaService.salvar(usuario.getId(), 12, 15, 40, 20, 30, 50);

        List<AvaliacaoFisica> historico = avaliacaoFisicaService.listarHistorico(usuario.getId());

        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getRepsPuxarVertical()).isEqualTo(12);
    }

    @Test
    void historicoDeUmUsuarioNaoApareceParaOOutro() {
        Usuario a = usuarioAtualService.obterUsuarioAtual(principal);
        Usuario b = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-historico-b", "Usuario B", "historico-b@exemplo.com"));

        avaliacaoFisicaService.salvar(a.getId(), 10, 15, 40, 20, 30, 50);

        assertThat(avaliacaoFisicaService.listarHistorico(b.getId())).isEmpty();
        assertThat(avaliacaoFisicaService.obterMaisRecenteDoUsuario(b.getId())).isEmpty();
    }
}
