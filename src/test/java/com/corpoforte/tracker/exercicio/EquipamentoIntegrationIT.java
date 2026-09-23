package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Equipamento;
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
 * Cobre o acoplamento entre o Controller de equipamentos e Usuario, mesmo
 * motivo do RegistroPesoIntegrationIT da Fase 3: a logica de atualizar
 * Usuario.equipamentosDisponiveis mora no Controller, entao so aparece
 * testando atraves dele.
 */
@AutoConfigureMockMvc
@Transactional
class EquipamentoIntegrationIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    private final OidcUser principal = OidcTestUsers.principal("sub-equipamento", "Usuaria Teste", "teste@exemplo.com");

    @Test
    void marcarEquipamentosAtualizaOConjuntoDoUsuario() throws Exception {
        usuarioAtualService.obterUsuarioAtual(principal);

        mockMvc.perform(post("/equipamentos").with(oidcLogin().oidcUser(principal)).with(csrf())
                        .param("equipamentos", "BARRA_FIXA", "ANEIS"))
                .andExpect(status().is3xxRedirection());

        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        assertThat(usuario.getEquipamentosDisponiveis())
                .containsExactlyInAnyOrder(Equipamento.BARRA_FIXA, Equipamento.ANEIS);
    }

    @Test
    void desmarcarTudoEsvaziaOConjuntoEmVezDeManterOAnterior() throws Exception {
        usuarioAtualService.obterUsuarioAtual(principal);

        mockMvc.perform(post("/equipamentos").with(oidcLogin().oidcUser(principal)).with(csrf())
                        .param("equipamentos", "BARRA_FIXA"))
                .andExpect(status().is3xxRedirection());

        // segundo envio sem nenhum equipamento marcado (form nao manda o
        // param "equipamentos" quando nada esta marcado)
        mockMvc.perform(post("/equipamentos").with(oidcLogin().oidcUser(principal)).with(csrf()))
                .andExpect(status().is3xxRedirection());

        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        assertThat(usuario.getEquipamentosDisponiveis()).isEmpty();
    }
}
