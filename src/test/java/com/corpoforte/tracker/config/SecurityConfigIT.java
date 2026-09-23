package com.corpoforte.tracker.config;

import com.corpoforte.tracker.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirma a regra mais basica da Fase 6: nenhuma rota responde sem login.
 * Sem este teste, uma mudanca futura no SecurityConfig (ex.: adicionar uma
 * rota como permitAll por engano) nao teria nenhum aviso automatizado.
 *
 * Fase 10: as ferramentas do perfil dev (token pra sessao web, OpenAPI)
 * nao existem fora dele - o lado "dentro do dev" fica em TokenDevIT.
 */
@AutoConfigureMockMvc
class SecurityConfigIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rotaProtegidaSemLoginRedirecionaEmVezDeResponder200() throws Exception {
        mockMvc.perform(get("/perfil"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void tokenDaApiPelaSessaoWebNaoExisteForaDoPerfilDev() throws Exception {
        mockMvc.perform(get("/dev/token-api").with(oidcLogin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void especificacaoOpenApiNaoEPublicadaForaDoPerfilDev() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }
}
