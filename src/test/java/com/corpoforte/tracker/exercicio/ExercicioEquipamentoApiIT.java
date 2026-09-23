package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ExercicioEquipamentoApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private Usuario usuario;

    @BeforeEach
    void criarUsuario() {
        usuario = contaComOnboarding(
                OidcTestUsers.principal("sub-exercicio-api", "Fulana", "exercicio-api@exemplo.com"));
    }

    /** Catalogo inteiro numa pagina, no mesmo envelope das listas
     * paginadas, sem cursor. */
    @Test
    void catalogoCompletoNoEnvelopePadrao() throws Exception {
        mockMvc.perform(get("/api/v1/exercicios").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(54)))
                .andExpect(jsonPath("$.proximoCursor").doesNotExist());
    }

    @Test
    void filtraPorNivelEMovimento() throws Exception {
        mockMvc.perform(get("/api/v1/exercicios")
                        .param("nivel", "INICIANTE").param("movimento", "PUXAR_VERTICAL")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(3)))
                .andExpect(jsonPath("$.itens[*].nivel", everyItem(is("INICIANTE"))));
    }

    /** Sem equipamento marcado, "compativel" e' so' peso corporal. */
    @Test
    void apenasCompativelUsaOsEquipamentosDoUsuario() throws Exception {
        mockMvc.perform(get("/api/v1/exercicios").param("apenasCompativel", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].equipamentoNecessario", everyItem(is("NENHUM"))));
    }

    @Test
    void filtroComValorInexistenteDa400() throws Exception {
        mockMvc.perform(get("/api/v1/exercicios").param("nivel", "LENDARIO")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor LENDARIO inválido para nivel."));
    }

    /** NENHUM nao e' algo que se possui (Usuario.atualizarEquipamentos). */
    @Test
    void substituirEquipamentosDescartaNenhum() throws Exception {
        mockMvc.perform(put("/api/v1/equipamentos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipamentos\": [\"BARRA_FIXA\", \"NENHUM\", \"ANEIS\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipamentos", hasSize(2)))
                .andExpect(jsonPath("$.equipamentos[0]").value("BARRA_FIXA"))
                .andExpect(jsonPath("$.equipamentos[1]").value("ANEIS"));

        mockMvc.perform(get("/api/v1/equipamentos").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(jsonPath("$.equipamentos", hasSize(2)))
                .andExpect(jsonPath("$.equipamentos", not(hasItem("NENHUM"))));

        // e o catalogo compativel passa a incluir exercicio de barra
        mockMvc.perform(get("/api/v1/exercicios").param("apenasCompativel", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(jsonPath("$.itens[*].equipamentoNecessario", hasItem("BARRA_FIXA")));
    }

    @Test
    void listaVaziaTiraTodosOsEquipamentosEAusenteDa400() throws Exception {
        mockMvc.perform(put("/api/v1/equipamentos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipamentos\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipamentos", hasSize(0)));

        mockMvc.perform(put("/api/v1/equipamentos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("equipamentos"));
    }
}
