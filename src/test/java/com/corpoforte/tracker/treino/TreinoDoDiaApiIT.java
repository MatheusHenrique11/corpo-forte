package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaService;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class TreinoDoDiaApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private AvaliacaoFisicaService avaliacaoFisicaService;

    private Usuario usuario;

    @BeforeEach
    void criarUsuario() {
        usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-treino-api", "Fulana", "treino-api@exemplo.com"));
    }

    /** O cliente decide a tela pelo "type", nao pelo texto. */
    @Test
    void semAvaliacaoRespondeProblemaTipado() throws Exception {
        mockMvc.perform(get("/api/v1/treino-do-dia").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:corpo-forte:problema:avaliacao-pendente"))
                .andExpect(jsonPath("$.status").value(409));
    }

    /**
     * Conta nova: iniciante e sem equipamento. Os dois padroes de puxar nao
     * tem exercicio sem equipamento no catalogo, entao ficam de fora
     * (Fase 5) - os outros quatro entram.
     */
    @Test
    void comAvaliacaoGeraOTreinoDeHoje() throws Exception {
        avaliacaoFisicaService.salvar(usuario.getId(), 10, 10, 10, 10, 10, 10);

        mockMvc.perform(get("/api/v1/treino-do-dia").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.itens", hasSize(4)))
                .andExpect(jsonPath("$.itens[0].exercicio.id", notNullValue()))
                .andExpect(jsonPath("$.itens[0].exercicio.nome", notNullValue()))
                .andExpect(jsonPath("$.movimentosSemOpcao", containsInAnyOrder("PUXAR_VERTICAL", "PUXAR_HORIZONTAL")))
                .andExpect(jsonPath("$.ciclo.semanaAtual").value(1))
                .andExpect(jsonPath("$.ciclo.totalSemanas").value(8));
    }

    /** PUT e DELETE sao idempotentes: repetir a requisicao (rede instavel
     * no celular) nao desfaz o que o usuario marcou, como o toggle faria. */
    @Test
    void marcarEDesmarcarSaoIdempotentes() throws Exception {
        avaliacaoFisicaService.salvar(usuario.getId(), 10, 10, 10, 10, 10, 10);
        Integer itemId = JsonPath.read(treino(), "$.itens[0].id");
        String rota = "/api/v1/treino-do-dia/itens/" + itemId + "/conclusao";

        mockMvc.perform(put(rota).header(HttpHeaders.AUTHORIZATION, bearer(usuario))).andExpect(status().isNoContent());
        mockMvc.perform(put(rota).header(HttpHeaders.AUTHORIZATION, bearer(usuario))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/treino-do-dia").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(jsonPath("$.itens[0].concluido").value(true));

        mockMvc.perform(delete(rota).header(HttpHeaders.AUTHORIZATION, bearer(usuario))).andExpect(status().isNoContent());
        mockMvc.perform(delete(rota).header(HttpHeaders.AUTHORIZATION, bearer(usuario))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/treino-do-dia").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(jsonPath("$.itens[0].concluido").value(false));
    }

    private String treino() throws Exception {
        return mockMvc.perform(get("/api/v1/treino-do-dia").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
