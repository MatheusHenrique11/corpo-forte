package com.corpoforte.tracker.peso;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o acoplamento entre os modulos peso e usuario: registrar peso
 * precisa continuar atualizando Usuario.pesoKg (usado no TMB/TDEE da
 * Fase 1). Sem este teste, uma mudanca futura em RegistroPesoController ou
 * em RegistroPesoService pode quebrar essa sincronizacao sem nenhum aviso -
 * so apareceria manualmente, olhando a tela do /perfil.
 *
 * Passa pelo Controller de proposito (via MockMvc), nao chama o Service
 * direto: a logica de sincronizacao mora no Controller (orquestra dois
 * services), entao um teste no nivel do Service sozinho nao pegaria uma
 * regressao ali.
 */
@AutoConfigureMockMvc
@Transactional
class RegistroPesoIntegrationIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    private final OidcUser principal = OidcTestUsers.principal("sub-registro-peso", "Usuaria Teste", "teste@exemplo.com");

    @Test
    void registrarPesoAtualizaOPesoDoPerfil() throws Exception {
        usuarioAtualService.obterUsuarioAtual(principal);

        mockMvc.perform(post("/peso").with(oidcLogin().oidcUser(principal)).with(csrf())
                        .param("data", "2026-01-10").param("pesoKg", "82.5"))
                .andExpect(status().is3xxRedirection());

        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        assertThat(usuario.getPesoKg()).isEqualTo(82.5);
    }

    @Test
    void registroRetroativoNaoSobrescreveOPesoAtual() throws Exception {
        usuarioAtualService.obterUsuarioAtual(principal);

        mockMvc.perform(post("/peso").with(oidcLogin().oidcUser(principal)).with(csrf())
                        .param("data", "2026-01-15").param("pesoKg", "80.0"))
                .andExpect(status().is3xxRedirection());

        // registro de uma data ANTERIOR ao que ja foi salvo nao pode
        // virar o peso atual do perfil
        mockMvc.perform(post("/peso").with(oidcLogin().oidcUser(principal)).with(csrf())
                        .param("data", "2026-01-05").param("pesoKg", "90.0"))
                .andExpect(status().is3xxRedirection());

        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        assertThat(usuario.getPesoKg()).isEqualTo(80.0);
    }
}
