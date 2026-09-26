package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.social.BloqueioService;
import com.corpoforte.tracker.social.Seguimento;
import com.corpoforte.tracker.social.SeguimentoRepository;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.Visibilidade;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * A matriz da regra de visibilidade (Fase 14): cada visibilidade de post x
 * cada relacao de quem ve com o autor x cada ponto de entrada. Todos os
 * pontos passam por RegraDeVisibilidade; esta matriz e' o que garante que
 * nenhum deles ficou de fora ou ganhou uma regra propria.
 *
 *                autor   seguidor   nao segue   bloqueado
 *   PUBLICO       sim      sim         sim         nao
 *   SEGUIDORES    sim      sim         nao         nao
 *   SOMENTE_EU    sim      nao         nao         nao
 *
 * "Ver" vale igual pra: aparecer no feed (Descobrir e, pra quem segue,
 * Seguindo), aparecer nos posts do perfil, abrir a pagina do post, curtir
 * e comentar. Nao ver = sumir das listas e 404 nos acessos por id.
 */
@AutoConfigureMockMvc
@Transactional
class VisibilidadeMatrizIT extends IntegrationTestBase {

    enum Relacao { AUTOR, SEGUIDOR, NAO_SEGUIDOR, BLOQUEADO }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SeguimentoRepository seguimentoRepository;

    @Autowired
    private BloqueioService bloqueioService;

    static Stream<Arguments> matriz() {
        return Arrays.stream(Visibilidade.values()).flatMap(visibilidade -> Arrays.stream(Relacao.values())
                .map(relacao -> Arguments.of(visibilidade, relacao, podeVer(visibilidade, relacao))));
    }

    private static boolean podeVer(Visibilidade visibilidade, Relacao relacao) {
        return switch (relacao) {
            case AUTOR -> true;
            case SEGUIDOR -> visibilidade != Visibilidade.SOMENTE_EU;
            case NAO_SEGUIDOR -> visibilidade == Visibilidade.PUBLICO;
            case BLOQUEADO -> false;
        };
    }

    @ParameterizedTest(name = "post {0} visto por {1}: {2}")
    @MethodSource("matriz")
    void cadaPontoDeEntradaSegueAMesmaRegra(Visibilidade visibilidade, Relacao relacao, boolean podeVer)
            throws Exception {
        Usuario autor = contaComOnboarding(OidcTestUsers.principal("sub-matriz-autor", "Autor", "autor@exemplo.com"));
        Usuario quemVe = relacao == Relacao.AUTOR ? autor
                : contaComOnboarding(OidcTestUsers.principal("sub-matriz-leitor", "Leitor", "leitor@exemplo.com"));
        if (relacao == Relacao.SEGUIDOR) {
            seguimentoRepository.save(new Seguimento(quemVe.getId(), autor.getId(), LocalDateTime.now()));
        }
        if (relacao == Relacao.BLOQUEADO) {
            bloqueioService.bloquear(autor.getId(), quemVe.getId());
        }
        Long postId = postRepository.save(new Post(autor.getId(), "post " + visibilidade, LocalDateTime.now(),
                visibilidade)).getId();
        String bearer = bearer(quemVe);

        // feed
        assertThat(ids(get("/api/v1/feed/descobrir"), bearer)).as("Descobrir")
                .isEqualTo(podeVer ? List.of(postId) : List.of());
        if (relacao == Relacao.AUTOR || relacao == Relacao.SEGUIDOR) {
            assertThat(ids(get("/api/v1/feed/seguindo"), bearer)).as("Seguindo")
                    .isEqualTo(podeVer ? List.of(postId) : List.of());
        }

        // perfil: com bloqueio o perfil inteiro some
        int statusPerfil = status(get("/api/v1/usuarios/" + autor.getUsername()), bearer);
        assertThat(statusPerfil).as("perfil").isEqualTo(relacao == Relacao.BLOQUEADO ? 404 : 200);
        if (statusPerfil == 200) {
            assertThat(ids(get("/api/v1/usuarios/" + autor.getUsername() + "/posts"), bearer)).as("posts do perfil")
                    .isEqualTo(podeVer ? List.of(postId) : List.of());
            assertThat(JsonPath.<Integer>read(corpo(get("/api/v1/usuarios/" + autor.getUsername()), bearer),
                    "$.contagens.posts")).as("contagem do perfil").isEqualTo(podeVer ? 1 : 0);
        }

        // acesso por id: invisivel e inexistente sao o mesmo 404
        int esperado = podeVer ? 200 : 404;
        assertThat(status(get("/api/v1/posts/" + postId), bearer)).as("pagina do post").isEqualTo(esperado);
        assertThat(status(get("/api/v1/posts/" + postId + "/comentarios"), bearer)).as("comentarios")
                .isEqualTo(esperado);
        assertThat(status(put("/api/v1/posts/" + postId + "/curtida"), bearer)).as("curtir")
                .isEqualTo(podeVer ? 204 : 404);
        assertThat(status(post("/api/v1/posts/" + postId + "/comentarios")
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\": \"oi\"}"), bearer)).as("comentar")
                .isEqualTo(podeVer ? 201 : 404);
    }

    private List<Long> ids(MockHttpServletRequestBuilder requisicao,
                           String bearer) throws Exception {
        return JsonPath.<List<Number>>read(corpo(requisicao, bearer), "$.itens[*].id").stream()
                .map(Number::longValue).toList();
    }

    private String corpo(MockHttpServletRequestBuilder requisicao,
                         String bearer) throws Exception {
        return mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, bearer))
                .andReturn().getResponse().getContentAsString();
    }

    private int status(MockHttpServletRequestBuilder requisicao,
                       String bearer) throws Exception {
        return mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, bearer)).andReturn().getResponse().getStatus();
    }
}
