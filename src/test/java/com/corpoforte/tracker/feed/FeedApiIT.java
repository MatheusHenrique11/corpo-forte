package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feed pela API. Os testes de acesso cruzado por id (apagar post alheio,
 * curtir post alheio...) ficam em EndpointsComIdIT, junto com os da tela.
 */
@AutoConfigureMockMvc
@Transactional
class FeedApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario autora;

    @BeforeEach
    void criarUsuario() {
        autora = contaComOnboarding(
                OidcTestUsers.principal("sub-feed-api", "Autora", "feed-api@exemplo.com"));
    }

    @Test
    void publicarDevolve201ComOPostNoFormatoDoFeed() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(autora))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"Primeira muscle-up!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("Primeira muscle-up!"))
                .andExpect(jsonPath("$.autor.id").value(autora.getId()))
                .andExpect(jsonPath("$.autor.nome").value("Autora"))
                .andExpect(jsonPath("$.podeApagar").value(true))
                .andExpect(jsonPath("$.curtidas").value(0))
                .andExpect(jsonPath("$.totalComentarios").value(0))
                // instante sempre em UTC, nunca horario local sem fuso
                .andExpect(jsonPath("$.criadoEm", endsWith("Z")));
    }

    /** O criadoEm devolvido na criacao e' o mesmo que o feed mostra depois
     * (o banco guarda microssegundos; o relogio do Java, nanossegundos). */
    @Test
    void criadoEmDaCriacaoBateComOLidoDoFeed() throws Exception {
        String criado = mockMvc.perform(post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(autora))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"post\"}"))
                .andReturn().getResponse().getContentAsString();
        entityManager.flush();
        entityManager.clear();

        assertThat(JsonPath.<String>read(feed(null), "$.itens[0].criadoEm"))
                .isEqualTo(JsonPath.<String>read(criado, "$.criadoEm"));
    }

    @Test
    void publicarValidaComOMesmoFormDaTela() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(autora))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("texto"));
    }

    /**
     * Pior caso do keyset: 21 posts com o MESMO criado_em. Sem o id como
     * desempate no cursor, a fronteira entre as paginas repetiria ou
     * pularia post. A juncao das duas paginas tem que ser exatamente os 21,
     * em ordem de id decrescente.
     */
    @Test
    void feedPaginadoNaoRepeteNemPulaPostsComOMesmoHorario() throws Exception {
        LocalDateTime mesmoInstante = LocalDateTime.of(2026, 9, 1, 12, 0);
        List<Long> criados = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            criados.add(postRepository.save(new Post(autora.getId(), "post " + i, mesmoInstante)).getId());
        }

        String pagina1 = feed(null);
        String pagina2 = feed(JsonPath.read(pagina1, "$.proximoCursor"));

        List<Long> vistos = new ArrayList<>(ids(pagina1));
        assertThat(vistos).hasSize(20);
        vistos.addAll(ids(pagina2));
        assertThat(vistos).containsExactlyElementsOf(criados.stream().sorted(Comparator.reverseOrder()).toList());
        assertThat((Object) JsonPath.read(pagina2, "$.proximoCursor")).isNull();
    }

    /**
     * Cada post traz so' os 3 comentarios mais recentes (em ordem de
     * conversa) e o total; o resto vem da rota de comentarios. Dois posts
     * pra provar que o limite e' POR post, nao da pagina.
     */
    @Test
    void cadaPostTrazOsTresComentariosMaisRecentesEOTotal() throws Exception {
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 12, 0);
        Post comCinco = postRepository.save(new Post(autora.getId(), "cinco comentarios", base));
        Post comUm = postRepository.save(new Post(autora.getId(), "um comentario", base.plusMinutes(1)));
        for (int i = 1; i <= 5; i++) {
            comentarioRepository.save(new Comentario(comCinco.getId(), autora.getId(), "c" + i, base.plusSeconds(i)));
        }
        comentarioRepository.save(new Comentario(comUm.getId(), autora.getId(), "unico", base.plusMinutes(2)));

        String pagina = feed(null);

        assertThat(JsonPath.<List<String>>read(pagina, "$.itens[1].comentariosRecentes[*].texto"))
                .containsExactly("c3", "c4", "c5");
        assertThat(JsonPath.<Integer>read(pagina, "$.itens[1].totalComentarios")).isEqualTo(5);
        assertThat(JsonPath.<List<String>>read(pagina, "$.itens[0].comentariosRecentes[*].texto"))
                .containsExactly("unico");
    }

    @Test
    void comentariosDoPostPaginadosDoMaisAntigoProMaisNovo() throws Exception {
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 12, 0);
        Post post = postRepository.save(new Post(autora.getId(), "conversa longa", base));
        for (int i = 1; i <= 22; i++) {
            comentarioRepository.save(new Comentario(post.getId(), autora.getId(), "c" + i, base.plusSeconds(i)));
        }
        String rota = "/api/v1/posts/" + post.getId() + "/comentarios";

        String pagina1 = mockMvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer(autora)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String pagina2 = mockMvc.perform(get(rota).param("cursor", (String) JsonPath.read(pagina1, "$.proximoCursor"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(autora)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<List<String>>read(pagina1, "$.itens[*].texto")).hasSize(20).first().isEqualTo("c1");
        assertThat(JsonPath.<List<String>>read(pagina2, "$.itens[*].texto")).containsExactly("c21", "c22");
    }

    @Test
    void comentarDevolve201EOComentarioApareceNoPost() throws Exception {
        Post post = postRepository.save(new Post(autora.getId(), "post", LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/comentarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(autora))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"Mandou bem\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("Mandou bem"))
                .andExpect(jsonPath("$.autor.nome").value("Autora"))
                .andExpect(jsonPath("$.podeApagar").value(true));

        mockMvc.perform(get("/api/v1/feed/descobrir").header(HttpHeaders.AUTHORIZATION, bearer(autora)))
                .andExpect(jsonPath("$.itens[0].totalComentarios").value(1));
    }

    @Test
    void curtirEDescurtirSaoIdempotentes() throws Exception {
        Post post = postRepository.save(new Post(autora.getId(), "post", LocalDateTime.now()));
        String rota = "/api/v1/posts/" + post.getId() + "/curtida";

        mockMvc.perform(put(rota).header(HttpHeaders.AUTHORIZATION, bearer(autora))).andExpect(status().isNoContent());
        mockMvc.perform(put(rota).header(HttpHeaders.AUTHORIZATION, bearer(autora))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/feed/descobrir").header(HttpHeaders.AUTHORIZATION, bearer(autora)))
                .andExpect(jsonPath("$.itens[0].curtidas").value(1))
                .andExpect(jsonPath("$.itens[0].curtidoPorMim").value(true));

        mockMvc.perform(delete(rota).header(HttpHeaders.AUTHORIZATION, bearer(autora))).andExpect(status().isNoContent());
        mockMvc.perform(delete(rota).header(HttpHeaders.AUTHORIZATION, bearer(autora))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/feed/descobrir").header(HttpHeaders.AUTHORIZATION, bearer(autora)))
                .andExpect(jsonPath("$.itens[0].curtidas").value(0))
                .andExpect(jsonPath("$.itens[0].curtidoPorMim").value(false));
    }

    @Test
    void apagarPostComDelete() throws Exception {
        Post post = postRepository.save(new Post(autora.getId(), "vai sumir", LocalDateTime.now()));

        mockMvc.perform(delete("/api/v1/posts/" + post.getId()).header(HttpHeaders.AUTHORIZATION, bearer(autora)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/feed/descobrir").header(HttpHeaders.AUTHORIZATION, bearer(autora)))
                .andExpect(jsonPath("$.itens", hasSize(0)));
    }

    /** O feed e' de todo mundo; o dado corporal do autor nunca e'. */
    @Test
    void feedNaoExpoeDadoCorporalDoAutor() throws Exception {
        postRepository.save(new Post(autora.getId(), "post", LocalDateTime.now()));

        mockMvc.perform(get("/api/v1/feed/descobrir").header(HttpHeaders.AUTHORIZATION, bearer(autora)))
                .andExpect(status().isOk())
                // nenhuma chave com esses nomes em nenhum nivel do JSON (checar
                // texto solto daria falso positivo: "visibilidade" contem "idade")
                .andExpect(jsonPath("$..pesoKg").isEmpty())
                .andExpect(jsonPath("$..alturaCm").isEmpty())
                .andExpect(jsonPath("$..idade").isEmpty());
    }

    private String feed(String cursor) throws Exception {
        var requisicao = get("/api/v1/feed/descobrir").header(HttpHeaders.AUTHORIZATION, bearer(autora));
        if (cursor != null) {
            requisicao.param("cursor", cursor);
        }
        return mockMvc.perform(requisicao).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static List<Long> ids(String pagina) {
        return JsonPath.<List<Number>>read(pagina, "$.itens[*].id").stream().map(Number::longValue).toList();
    }
}
