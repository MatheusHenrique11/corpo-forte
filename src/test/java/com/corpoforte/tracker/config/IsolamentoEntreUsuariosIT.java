package com.corpoforte.tracker.config;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaService;
import com.corpoforte.tracker.peso.RegistroPesoService;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vazamento de dado entre contas via leitura normal (nao acesso por ID -
 * isso fica em EndpointsComIdIT). Dois usuarios distintos, cada um com seu
 * proprio OidcUser/sub, confirmam que contas ficam separadas e que um GET
 * de um usuario nunca mostra dado gravado pelo outro.
 *
 * Fase 11: a API e' outra porta de entrada pro mesmo dado, entao ganhou a
 * versao dela de cada teste - com access token em vez de sessao.
 */
@AutoConfigureMockMvc
@Transactional
class IsolamentoEntreUsuariosIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private RegistroPesoService registroPesoService;

    @Autowired
    private AvaliacaoFisicaService avaliacaoFisicaService;

    private final OidcUser usuarioA = OidcTestUsers.principal("sub-isolamento-a", "Usuaria A", "a@exemplo.com");
    private final OidcUser usuarioB = OidcTestUsers.principal("sub-isolamento-b", "Usuario B", "b@exemplo.com");

    @Test
    void doisLoginsDiferentesCriamContasDistintas() {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        Usuario b = usuarioAtualService.obterUsuarioAtual(usuarioB);

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }

    @Test
    void registroDePesoDeUmUsuarioNaoApareceParaOOutro() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        usuarioAtualService.obterUsuarioAtual(usuarioB);

        registroPesoService.salvar(a.getId(), LocalDate.of(2026, 5, 1), 77.0);

        mockMvc.perform(get("/peso").with(oidcLogin().oidcUser(usuarioB)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("77.0"))));
    }

    @Test
    void apiPesoDeUmUsuarioNaoApareceParaOOutro() throws Exception {
        Usuario a = contaComOnboarding(usuarioA);
        Usuario b = contaComOnboarding(usuarioB);
        registroPesoService.registrar(a, LocalDate.of(2026, 5, 1), 77.0);

        mockMvc.perform(get("/api/v1/pesos").header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(0)));
        mockMvc.perform(get("/api/v1/pesos/tendencia").header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/perfil").header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(content().string(not(containsString("77.0"))));
    }

    @Test
    void apiAvaliacaoETreinoDeUmUsuarioNaoAparecemParaOOutro() throws Exception {
        Usuario a = contaComOnboarding(usuarioA);
        Usuario b = contaComOnboarding(usuarioB);
        avaliacaoFisicaService.salvar(a.getId(), 10, 10, 10, 10, 10, 10);

        mockMvc.perform(get("/api/v1/avaliacoes").header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(jsonPath("$.itens", hasSize(0)));
        // sem avaliacao propria, B nao ganha o treino gerado a partir da de A
        mockMvc.perform(get("/api/v1/treino-do-dia").header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isConflict());
    }

    /**
     * O cursor e' opaco mas nao e' segredo nem permissao: B montar um
     * cursor com uma posicao que so' existe no historico de A continua
     * devolvendo so' o historico de B.
     */
    @Test
    void apiCursorForjadoComAPosicaoDeOutraContaNaoMostraNadaDela() throws Exception {
        Usuario a = contaComOnboarding(usuarioA);
        Usuario b = contaComOnboarding(usuarioB);
        registroPesoService.registrar(a, LocalDate.of(2026, 5, 1), 77.0);
        String cursorForjado = Cursor.apos(LocalDate.of(2026, 5, 2), 0).codificar();

        mockMvc.perform(get("/api/v1/pesos").param("cursor", cursorForjado)
                        .header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(0)));
    }
}
