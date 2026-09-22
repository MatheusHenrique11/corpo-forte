package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void marcarEquipamentosAtualizaOConjuntoDoUsuario() throws Exception {
        usuarioAtualService.obterOuCriarPadrao();

        mockMvc.perform(post("/equipamentos")
                        .param("equipamentos", "BARRA_FIXA", "ANEIS"))
                .andExpect(status().is3xxRedirection());

        Usuario usuario = usuarioAtualService.obterOuCriarPadrao();
        assertThat(usuario.getEquipamentosDisponiveis())
                .containsExactlyInAnyOrder(Equipamento.BARRA_FIXA, Equipamento.ANEIS);
    }

    @Test
    void desmarcarTudoEsvaziaOConjuntoEmVezDeManterOAnterior() throws Exception {
        usuarioAtualService.obterOuCriarPadrao();

        mockMvc.perform(post("/equipamentos").param("equipamentos", "BARRA_FIXA"))
                .andExpect(status().is3xxRedirection());

        // segundo envio sem nenhum equipamento marcado (form nao manda o
        // param "equipamentos" quando nada esta marcado)
        mockMvc.perform(post("/equipamentos"))
                .andExpect(status().is3xxRedirection());

        Usuario usuario = usuarioAtualService.obterOuCriarPadrao();
        assertThat(usuario.getEquipamentosDisponiveis()).isEmpty();
    }
}
