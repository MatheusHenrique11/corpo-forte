package com.corpoforte.tracker.config;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.peso.RegistroPesoService;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vazamento de dado entre contas via leitura normal (nao acesso por ID -
 * isso fica em EndpointsComIdIT). Dois usuarios distintos, cada um com seu
 * proprio OidcUser/sub, confirmam que contas ficam separadas e que um GET
 * de um usuario nunca mostra dado gravado pelo outro.
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
}
