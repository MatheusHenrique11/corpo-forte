package com.corpoforte.tracker.perfil;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class PerfilApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    private Usuario usuario;

    @BeforeEach
    void criarUsuario() {
        usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-perfil-api", "Fulana", "perfil-api@exemplo.com"));
    }

    /** Conta nova nasce com 100 kg, 178 cm, 27 anos e perda de gordura: os
     * mesmos numeros do teste de regressao da Fase 1 (TMB 1982,5). */
    @Test
    void perfilTrazOCalculoDaFase1() throws Exception {
        mockMvc.perform(get("/api/v1/perfil").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fulana"))
                .andExpect(jsonPath("$.pesoKg").value(100.0))
                .andExpect(jsonPath("$.objetivo").value("PERDA_GORDURA"))
                .andExpect(jsonPath("$.calculo.tmb").value(closeTo(1982.5, 0.01)))
                .andExpect(jsonPath("$.calculo.classificacaoImc").value("OBESIDADE_GRAU_I"));
    }

    /** Mesma Bean Validation do formulario da tela (PerfilForm). */
    @Test
    void atualizarRecusaValoresForaDaFaixaListandoOsCampos() throws Exception {
        mockMvc.perform(put("/api/v1/perfil")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "", "alturaCm": 50, "idade": 30,
                                 "objetivo": "GANHO_MASSA", "nivel": "AVANCADO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", containsInAnyOrder("nome", "alturaCm")));
    }

    /**
     * Peso tem uma fonte so' desde a Fase 3: o registro de peso. Mandar
     * pesoKg no PUT do perfil nao muda nada - o campo nem existe no form.
     */
    @Test
    void atualizarPerfilNaoMexeNoPeso() throws Exception {
        mockMvc.perform(put("/api/v1/perfil")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Fulana Silva", "alturaCm": 165, "idade": 31,
                                 "objetivo": "GANHO_MASSA", "nivel": "INTERMEDIARIO", "pesoKg": 60}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fulana Silva"))
                .andExpect(jsonPath("$.alturaCm").value(165.0))
                .andExpect(jsonPath("$.nivel").value("INTERMEDIARIO"))
                .andExpect(jsonPath("$.pesoKg").value(100.0));
    }
}
