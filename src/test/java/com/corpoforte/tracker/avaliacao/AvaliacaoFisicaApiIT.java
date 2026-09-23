package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class AvaliacaoFisicaApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private AvaliacaoFisicaRepository avaliacaoFisicaRepository;

    private Usuario usuario;

    @BeforeEach
    void criarUsuario() {
        usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-avaliacao-api", "Fulana", "avaliacao-api@exemplo.com"));
    }

    /** 15 repeticoes -> B = 90, C = 36, D = 4,5 arredondado pra 5 (Fase 2). */
    @Test
    void registrarDevolveOVolumeCalculadoPorMovimento() throws Exception {
        mockMvc.perform(post("/api/v1/avaliacoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repsPuxarVertical": 15, "repsEmpurrarVertical": 15, "repsPernasBilateral": 15,
                                 "repsPuxarHorizontal": 15, "repsEmpurrarHorizontal": 15, "repsPernasUnilateral": 15}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.itens", hasSize(6)))
                .andExpect(jsonPath("$.itens[0].movimento").value("PUXAR_VERTICAL"))
                .andExpect(jsonPath("$.itens[0].volumeTotalTreino").value(90))
                .andExpect(jsonPath("$.itens[0].volumeInicial").value(36))
                .andExpect(jsonPath("$.itens[0].incremento").value(5));
    }

    @Test
    void registrarValidaComOMesmoFormDaTela() throws Exception {
        mockMvc.perform(post("/api/v1/avaliacoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repsPuxarVertical\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos", hasSize(6)));
    }

    @Test
    void historicoVemPaginadoPorCursorSemRepetirNemPular() throws Exception {
        LocalDate primeira = LocalDate.of(2025, 1, 1);
        for (int dia = 0; dia < 22; dia++) {
            avaliacaoFisicaRepository.save(new AvaliacaoFisica(usuario.getId(), 10, 10, 10, 10, 10, 10,
                    primeira.plusDays(dia)));
        }

        String pagina1 = historico(null);
        String cursor = JsonPath.read(pagina1, "$.proximoCursor");
        String pagina2 = historico(cursor);

        List<String> datas = new ArrayList<>(JsonPath.<List<String>>read(pagina1, "$.itens[*].data"));
        assertThat(datas).hasSize(20).first().isEqualTo("2025-01-22");
        datas.addAll(JsonPath.read(pagina2, "$.itens[*].data"));
        assertThat(datas).hasSize(22).doesNotHaveDuplicates().last().isEqualTo("2025-01-01");
        assertThat((Object) JsonPath.read(pagina2, "$.proximoCursor")).isNull();
    }

    @Test
    void comparacaoPrecisaDeDuasAvaliacoes() throws Exception {
        avaliacaoFisicaRepository.save(new AvaliacaoFisica(usuario.getId(), 10, 10, 10, 10, 10, 10,
                LocalDate.of(2026, 1, 1)));

        mockMvc.perform(get("/api/v1/avaliacoes/comparacao").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:corpo-forte:problema:avaliacoes-insuficientes"));

        avaliacaoFisicaRepository.save(new AvaliacaoFisica(usuario.getId(), 15, 10, 10, 10, 10, 10,
                LocalDate.of(2026, 3, 1)));

        mockMvc.perform(get("/api/v1/avaliacoes/comparacao").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataAtual").value("2026-03-01"))
                .andExpect(jsonPath("$.dataAnterior").value("2026-01-01"))
                .andExpect(jsonPath("$.itens[0].movimento").value("PUXAR_VERTICAL"))
                .andExpect(jsonPath("$.itens[0].repsAnterior").value(10))
                .andExpect(jsonPath("$.itens[0].repsAtual").value(15))
                .andExpect(jsonPath("$.itens[0].diferenca").value(5))
                .andExpect(jsonPath("$.itens[1].diferenca").value(0));
    }

    @Test
    void cursorAdulteradoDa400() throws Exception {
        mockMvc.perform(get("/api/v1/avaliacoes").param("cursor", "lixo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cursor inválido"));
    }

    private String historico(String cursor) throws Exception {
        var requisicao = get("/api/v1/avaliacoes").header(HttpHeaders.AUTHORIZATION, bearer(usuario));
        if (cursor != null) {
            requisicao.param("cursor", cursor);
        }
        return mockMvc.perform(requisicao)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
