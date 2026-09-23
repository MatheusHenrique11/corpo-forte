package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.social.Seguimento;
import com.corpoforte.tracker.social.SeguimentoRepository;
import com.corpoforte.tracker.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Feed "Seguindo" e pagina do post (Fase 13). */
@AutoConfigureMockMvc
@Transactional
class FeedSeguindoApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SeguimentoRepository seguimentoRepository;

    private Usuario eu;
    private Usuario sigo;
    private Usuario naoSigo;
    private final LocalDateTime base = LocalDateTime.of(2026, 9, 1, 12, 0);

    @BeforeEach
    void criarContas() {
        eu = contaComOnboarding(OidcTestUsers.principal("sub-feed-eu", "Eu", "feed-eu@exemplo.com"));
        sigo = contaComOnboarding(OidcTestUsers.principal("sub-feed-sigo", "Sigo", "feed-sigo@exemplo.com"));
        naoSigo = contaComOnboarding(OidcTestUsers.principal("sub-feed-nao-sigo", "Nao Sigo", "nao-sigo@exemplo.com"));
        seguimentoRepository.save(new Seguimento(eu.getId(), sigo.getId(), base));
    }

    @Test
    void seguindoTemOsMeusPostsEOsDeQuemEuSigoESoIsso() throws Exception {
        postRepository.save(new Post(eu.getId(), "meu post", base.plusMinutes(1)));
        postRepository.save(new Post(sigo.getId(), "de quem eu sigo", base.plusMinutes(2)));
        postRepository.save(new Post(naoSigo.getId(), "de quem eu nao sigo", base.plusMinutes(3)));

        assertThat(textos(feed("/api/v1/feed/seguindo", null)))
                .containsExactly("de quem eu sigo", "meu post");
        // o Descobrir continua global
        assertThat(textos(feed("/api/v1/feed/descobrir", null)))
                .containsExactly("de quem eu nao sigo", "de quem eu sigo", "meu post");
    }

    @Test
    void deixarDeSeguirTiraOsPostsDoSeguindo() throws Exception {
        postRepository.save(new Post(sigo.getId(), "de quem eu sigo", base.plusMinutes(2)));

        mockMvc.perform(delete("/api/v1/usuarios/sub_feed_sigo/seguimento").header(HttpHeaders.AUTHORIZATION, bearer(eu)))
                .andExpect(status().isNoContent());

        assertThat(textos(feed("/api/v1/feed/seguindo", null))).isEmpty();
    }

    @Test
    void seguindoPaginaPeloMesmoCursorDoFeed() throws Exception {
        for (int i = 0; i < 21; i++) {
            postRepository.save(new Post(sigo.getId(), "post " + i, base.plusMinutes(i)));
        }

        String pagina1 = feed("/api/v1/feed/seguindo", null);
        String pagina2 = feed("/api/v1/feed/seguindo", JsonPath.read(pagina1, "$.proximoCursor"));

        assertThat(textos(pagina1)).hasSize(20).first().isEqualTo("post 20");
        assertThat(textos(pagina2)).containsExactly("post 0");
    }

    /** Pagina do post: mesmo formato do feed, de qualquer autor. */
    @Test
    void paginaDoPostNoFormatoDoFeed() throws Exception {
        Post post = postRepository.save(new Post(naoSigo.getId(), "post avulso", base));

        mockMvc.perform(get("/api/v1/posts/" + post.getId()).header(HttpHeaders.AUTHORIZATION, bearer(eu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("post avulso"))
                .andExpect(jsonPath("$.autor.username").value("sub_feed_nao_sigo"))
                .andExpect(jsonPath("$.podeApagar").value(false))
                .andExpect(jsonPath("$.totalComentarios").value(0));
        mockMvc.perform(get("/api/v1/posts/999999").header(HttpHeaders.AUTHORIZATION, bearer(eu)))
                .andExpect(status().isNotFound());
    }

    private String feed(String rota, String cursor) throws Exception {
        var requisicao = get(rota).header(HttpHeaders.AUTHORIZATION, bearer(eu));
        if (cursor != null) {
            requisicao.param("cursor", cursor);
        }
        return mockMvc.perform(requisicao).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static List<String> textos(String pagina) {
        return JsonPath.read(pagina, "$.itens[*].texto");
    }
}
