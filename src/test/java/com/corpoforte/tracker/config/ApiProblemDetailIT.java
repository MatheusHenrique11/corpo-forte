package com.corpoforte.tracker.config;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.auth.EmissorTokens;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Erro da API sai sempre em ProblemDetail (application/problem+json), com
 * o "detail" em portugues - inclusive os que o proprio Spring gera antes de
 * chegar em controller nenhum (rota inexistente, metodo errado, JSON
 * quebrado). Os 401 estao em ApiSegurancaIT.
 */
@AutoConfigureMockMvc
@Transactional
class ApiProblemDetailIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private EmissorTokens emissorTokens;

    private String bearer;

    @BeforeEach
    void autenticar() {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-api-problem", "Fulana", "api-problem@exemplo.com"));
        bearer = "Bearer " + emissorTokens.emitir(usuario.getId()).accessToken();
    }

    @Test
    void rotaInexistenteDa404EmProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/rota-que-nao-existe").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Rota não encontrada na API."))
                .andExpect(jsonPath("$.instance").value("/api/v1/rota-que-nao-existe"));
    }

    @Test
    void metodoNaoSuportadoDa405ComCabecalhoAllow() throws Exception {
        mockMvc.perform(delete("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("O método DELETE não é suportado nesta rota."));
    }

    @Test
    void validacaoListaOsCamposInvalidos() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Um ou mais campos estão inválidos."))
                .andExpect(jsonPath("$.campos[0].campo").value("idToken"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("Informe o idToken do Google"));
    }

    @Test
    void jsonMalFormadoDa400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nao-e-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Corpo da requisição ausente ou mal formado."));
    }

    @Test
    void contentTypeQueNaoEJsonDa415() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("idToken=abc"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
