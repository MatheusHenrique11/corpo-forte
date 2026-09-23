package com.corpoforte.tracker.peso;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A sincronizacao registro de peso -> Usuario.pesoKg (Fase 3) morava no
 * controller da tela. Este teste e' o que prova que, depois de descer pro
 * service, ela vale tambem pela API.
 */
@AutoConfigureMockMvc
@Transactional
class RegistroPesoApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private RegistroPesoService registroPesoService;

    private Usuario usuario;

    @BeforeEach
    void criarUsuario() {
        usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-peso-api", "Fulana", "peso-api@exemplo.com"));
    }

    @Test
    void registroMaisRecentePorDataViraOPesoDoPerfil() throws Exception {
        registrar("{\"data\": \"" + LocalDate.now() + "\", \"pesoKg\": 80.5}").andExpect(status().isOk());
        // retroativo: nao sobrescreve o peso atual
        registrar("{\"data\": \"" + LocalDate.now().minusDays(30) + "\", \"pesoKg\": 90}").andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/perfil").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(jsonPath("$.pesoKg").value(80.5));
    }

    @Test
    void semDataRegistraHoje() throws Exception {
        registrar("{\"pesoKg\": 70}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.pesoKg").value(70.0));
    }

    @Test
    void validaComOMesmoFormDaTela() throws Exception {
        registrar("{\"data\": \"" + LocalDate.now().plusDays(1) + "\", \"pesoKg\": 10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", containsInAnyOrder("data", "pesoKg")));
    }

    @Test
    void historicoPaginadoMaisRecentePrimeiro() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        for (int dia = 0; dia < 21; dia++) {
            registroPesoService.salvar(usuario.getId(), inicio.plusDays(dia), 80 + dia * 0.1);
        }

        String pagina1 = historico(null);
        String pagina2 = historico(JsonPath.read(pagina1, "$.proximoCursor"));

        assertThat(JsonPath.<List<String>>read(pagina1, "$.itens[*].data")).hasSize(20).first().isEqualTo("2026-01-21");
        assertThat(JsonPath.<List<String>>read(pagina2, "$.itens[*].data")).containsExactly("2026-01-01");
        assertThat((Object) JsonPath.read(pagina2, "$.proximoCursor")).isNull();
    }

    @Test
    void tendenciaPrecisaDeAoMenosUmRegistro() throws Exception {
        mockMvc.perform(get("/api/v1/pesos/tendencia").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:corpo-forte:problema:registro-de-peso-pendente"));

        // duas semanas com dado (segunda a domingo): 81 -> 80
        registroPesoService.salvar(usuario.getId(), LocalDate.of(2026, 9, 7), 81.0);
        registroPesoService.salvar(usuario.getId(), LocalDate.of(2026, 9, 14), 80.0);

        mockMvc.perform(get("/api/v1/pesos/tendencia").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicioSemanaAtual").value("2026-09-14"))
                .andExpect(jsonPath("$.mediaSemanaAtual").value(80.0))
                .andExpect(jsonPath("$.mediaSemanaAnterior").value(81.0))
                .andExpect(jsonPath("$.direcao").value("DESCENDO"));
    }

    private ResultActions registrar(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/pesos")
                .header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String historico(String cursor) throws Exception {
        var requisicao = get("/api/v1/pesos").header(HttpHeaders.AUTHORIZATION, bearer(usuario));
        if (cursor != null) {
            requisicao.param("cursor", cursor);
        }
        return mockMvc.perform(requisicao).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
