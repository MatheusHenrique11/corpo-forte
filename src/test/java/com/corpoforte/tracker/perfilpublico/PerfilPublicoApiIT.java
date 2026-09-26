package com.corpoforte.tracker.perfilpublico;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.feed.Post;
import com.corpoforte.tracker.feed.PostRepository;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class PerfilPublicoApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario autora;
    private Usuario leitor;

    @BeforeEach
    void criarContas() {
        autora = contaComOnboarding(OidcTestUsers.principal("sub-autora", "Autora", "autora@exemplo.com"));
        leitor = contaComOnboarding(OidcTestUsers.principal("sub-leitor", "Leitor", "leitor@exemplo.com"));
    }

    /**
     * Dado corporal e' sempre privado (decisao transversal 2): o perfil
     * publico mostra quem a pessoa e' na comunidade, nunca peso, altura,
     * idade, objetivo, nivel nem e-mail.
     */
    @Test
    void perfilPublicoMostraAComunidadeENuncaDadoCorporal() throws Exception {
        editar("sub_autora", "Calistenia todo dia").andExpect(status().isOk());
        postRepository.save(new Post(autora.getId(), "um", LocalDateTime.now()));
        postRepository.save(new Post(autora.getId(), "dois", LocalDateTime.now()));

        mockMvc.perform(get("/api/v1/usuarios/sub_autora").header(HttpHeaders.AUTHORIZATION, bearer(leitor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(autora.getId()))
                .andExpect(jsonPath("$.username").value("sub_autora"))
                .andExpect(jsonPath("$.nome").value("Autora"))
                .andExpect(jsonPath("$.bio").value("Calistenia todo dia"))
                .andExpect(jsonPath("$.contagens.posts").value(2))
                // chaves JSON em qualquer nivel, nao texto solto (um campo
                // "visibilidade" casaria com "idade")
                .andExpect(jsonPath("$..pesoKg").isEmpty())
                .andExpect(jsonPath("$..alturaCm").isEmpty())
                .andExpect(jsonPath("$..idade").isEmpty())
                .andExpect(jsonPath("$..objetivo").isEmpty())
                .andExpect(jsonPath("$..nivel").isEmpty())
                .andExpect(jsonPath("$..email").isEmpty())
                .andExpect(content().string(not(containsString("autora@exemplo.com"))));
    }

    @Test
    void buscaPorUsernameNaoDiferenciaMaiusculas() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/SUB_AUTORA").header(HttpHeaders.AUTHORIZATION, bearer(leitor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sub_autora"));
    }

    @Test
    void usernameInexistenteDa404() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/ninguem_tem_esse").header(HttpHeaders.AUTHORIZATION, bearer(leitor)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/usuarios/ninguem_tem_esse/posts").header(HttpHeaders.AUTHORIZATION, bearer(leitor)))
                .andExpect(status().isNotFound());
    }

    @Test
    void postsDoPerfilSaoSoDaquelaContaEPaginados() throws Exception {
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 12, 0);
        for (int i = 0; i < 21; i++) {
            postRepository.save(new Post(autora.getId(), "post da autora " + i, base.plusMinutes(i)));
        }
        postRepository.save(new Post(leitor.getId(), "post do leitor", base.plusDays(1)));

        String pagina1 = posts(null);
        String pagina2 = posts(JsonPath.read(pagina1, "$.proximoCursor"));

        assertThat(JsonPath.<List<String>>read(pagina1, "$.itens[*].texto"))
                .hasSize(20).first().isEqualTo("post da autora 20");
        assertThat(JsonPath.<List<String>>read(pagina2, "$.itens[*].texto")).containsExactly("post da autora 0");
        mockMvc.perform(get("/api/v1/usuarios/sub_autora/posts").header(HttpHeaders.AUTHORIZATION, bearer(leitor)))
                .andExpect(jsonPath("$.itens[*].autor.username", everyItem(is("sub_autora"))));
    }

    @Test
    void trocarUsernameMudaOEnderecoDoPerfil() throws Exception {
        editar("autora.nova", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("autora.nova"));

        mockMvc.perform(get("/api/v1/usuarios/autora.nova").header(HttpHeaders.AUTHORIZATION, bearer(leitor)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/usuarios/sub_autora").header(HttpHeaders.AUTHORIZATION, bearer(leitor)))
                .andExpect(status().isNotFound());
    }

    @Test
    void usernameDeOutraContaResponde409ENaoMudaNada() throws Exception {
        editar("sub_leitor", "bio nova")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:corpo-forte:problema:username-indisponivel"));

        assertThat(usuarioRepository.findById(autora.getId()).orElseThrow().getUsername()).isEqualTo("sub_autora");
    }

    @Test
    void bioAcimaDe160CaracteresEUsernameReservadoDao400() throws Exception {
        editar("admin", "a".repeat(161))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(2));
    }

    /** O feed passa a mostrar quem postou pelo username tambem. */
    @Test
    void autorNoFeedTrazUsername() throws Exception {
        postRepository.save(new Post(autora.getId(), "post", LocalDateTime.now()));

        mockMvc.perform(get("/api/v1/feed/descobrir").header(HttpHeaders.AUTHORIZATION, bearer(leitor)))
                .andExpect(jsonPath("$.itens[0].autor.username").value("sub_autora"));
    }

    private ResultActions editar(String username, String bio) throws Exception {
        String corpo = bio == null
                ? "{\"username\": \"%s\"}".formatted(username)
                : "{\"username\": \"%s\", \"bio\": \"%s\"}".formatted(username, bio);
        return mockMvc.perform(put("/api/v1/perfil/publico")
                .header(HttpHeaders.AUTHORIZATION, bearer(autora))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private String posts(String cursor) throws Exception {
        var requisicao = get("/api/v1/usuarios/sub_autora/posts").header(HttpHeaders.AUTHORIZATION, bearer(leitor));
        if (cursor != null) {
            requisicao.param("cursor", cursor);
        }
        return mockMvc.perform(requisicao).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
