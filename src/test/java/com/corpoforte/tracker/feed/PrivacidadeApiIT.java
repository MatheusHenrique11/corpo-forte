package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.Visibilidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Visibilidade padrao da conta e a regra valendo tambem nas telas web. */
@AutoConfigureMockMvc
@Transactional
class PrivacidadeApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    private Usuario autora;

    @BeforeEach
    void criarConta() {
        autora = contaComOnboarding(OidcTestUsers.principal("sub-priv-autora", "Autora", "priv-autora@exemplo.com"));
    }

    @Test
    void contaComecaPublicaEOPostSemVisibilidadeUsaOPadrao() throws Exception {
        mockMvc.perform(get("/api/v1/privacidade").header(HttpHeaders.AUTHORIZATION, bearer(autora)))
                .andExpect(jsonPath("$.visibilidadePadrao").value("PUBLICO"));

        mockMvc.perform(put("/api/v1/privacidade").header(HttpHeaders.AUTHORIZATION, bearer(autora))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visibilidadePadrao\": \"SEGUIDORES\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibilidadePadrao").value("SEGUIDORES"));

        publicar("{\"texto\": \"sem escolher\"}").andExpect(jsonPath("$.visibilidade").value("SEGUIDORES"));
        publicar("{\"texto\": \"escolhido\", \"visibilidade\": \"SOMENTE_EU\"}")
                .andExpect(jsonPath("$.visibilidade").value("SOMENTE_EU"));
    }

    /** Trocar o padrao vale pros proximos posts, nao reescreve os antigos. */
    @Test
    void trocarOPadraoNaoMudaPostsQueJaExistem() throws Exception {
        publicar("{\"texto\": \"antigo\"}").andExpect(jsonPath("$.visibilidade").value("PUBLICO"));

        mockMvc.perform(put("/api/v1/privacidade").header(HttpHeaders.AUTHORIZATION, bearer(autora))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visibilidadePadrao\": \"SOMENTE_EU\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/feed/descobrir").header(HttpHeaders.AUTHORIZATION, bearer(autora)))
                .andExpect(jsonPath("$.itens[0].visibilidade").value("PUBLICO"));
    }

    @Test
    void padraoAusenteDa400() throws Exception {
        mockMvc.perform(put("/api/v1/privacidade").header(HttpHeaders.AUTHORIZATION, bearer(autora))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("visibilidadePadrao"));
    }

    /**
     * A tela web e' outra porta de entrada pro mesmo dado: o feed da tela
     * nao mostra o SOMENTE_EU de outra conta, e curtir/comentar por la da
     * o mesmo 404 da API.
     */
    @Test
    void telasWebSeguemAMesmaRegra() throws Exception {
        Post escondido = postRepository.save(new Post(autora.getId(), "texto so meu", LocalDateTime.now(),
                Visibilidade.SOMENTE_EU));
        var outra = OidcTestUsers.principal("sub-priv-outra", "Outra", "priv-outra@exemplo.com");

        mockMvc.perform(get("/feed").with(oidcLogin().oidcUser(outra)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("texto so meu"))));
        mockMvc.perform(post("/feed/posts/" + escondido.getId() + "/curtir").with(oidcLogin().oidcUser(outra)).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/feed/posts/" + escondido.getId() + "/comentarios").param("texto", "oi")
                        .with(oidcLogin().oidcUser(outra)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    private ResultActions publicar(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/posts").header(HttpHeaders.AUTHORIZATION, bearer(autora))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }
}
