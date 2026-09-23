package com.corpoforte.tracker.config;

import com.corpoforte.tracker.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirma a regra mais basica da Fase 6: nenhuma rota responde sem login.
 * Sem este teste, uma mudanca futura no SecurityConfig (ex.: adicionar uma
 * rota como permitAll por engano) nao teria nenhum aviso automatizado.
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
}
