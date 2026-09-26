package com.corpoforte.tracker.social;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.feed.Comentario;
import com.corpoforte.tracker.feed.ComentarioRepository;
import com.corpoforte.tracker.feed.Post;
import com.corpoforte.tracker.feed.PostRepository;
import com.corpoforte.tracker.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bloqueio (Fase 14). O efeito sobre posts, curtir e comentar esta na
 * VisibilidadeMatrizIT; aqui fica o resto: seguir, perfil nos dois
 * sentidos, listas, busca, comentarios e desbloquear.
 */
@AutoConfigureMockMvc
@Transactional
class BloqueioApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeguimentoRepository seguimentoRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    private Usuario ana;
    private Usuario bruno;

    @BeforeEach
    void criarContas() {
        ana = contaComOnboarding(OidcTestUsers.principal("sub-bloq-ana", "Ana", "bloq-ana@exemplo.com"));
        bruno = contaComOnboarding(OidcTestUsers.principal("sub-bloq-bruno", "Bruno", "bloq-bruno@exemplo.com"));
    }

    @Test
    void bloquearDesfazOSeguirNosDoisSentidosEImpedeSeguirDeNovo() throws Exception {
        seguimentoRepository.save(new Seguimento(ana.getId(), bruno.getId(), LocalDateTime.now()));
        seguimentoRepository.save(new Seguimento(bruno.getId(), ana.getId(), LocalDateTime.now()));

        bloqueio("PUT", ana, "sub_bloq_bruno").andExpect(status().isNoContent());
        bloqueio("PUT", ana, "sub_bloq_bruno").andExpect(status().isNoContent());

        assertThat(seguimentoRepository.countBySeguidorId(ana.getId())).isZero();
        assertThat(seguimentoRepository.countBySeguidorId(bruno.getId())).isZero();
        // nem o bloqueado, nem quem bloqueou (mutuo em efeito)
        mockMvc.perform(put("/api/v1/usuarios/sub_bloq_ana/seguimento").header(HttpHeaders.AUTHORIZATION, bearer(bruno)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/usuarios/sub_bloq_bruno/seguimento").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isNotFound());
    }

    /** O perfil some nos dois sentidos, com o mesmo 404 de conta
     * inexistente, e volta depois de desbloquear. */
    @Test
    void perfilSomeNosDoisSentidosEVoltaAoDesbloquear() throws Exception {
        bloqueio("PUT", ana, "sub_bloq_bruno").andExpect(status().isNoContent());

        perfil(bruno, "sub_bloq_ana").andExpect(status().isNotFound());
        perfil(ana, "sub_bloq_bruno").andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/usuarios/sub_bloq_ana/seguidores").header(HttpHeaders.AUTHORIZATION, bearer(bruno)))
                .andExpect(status().isNotFound());

        bloqueio("DELETE", ana, "sub_bloq_bruno").andExpect(status().isNoContent());

        perfil(bruno, "sub_bloq_ana").andExpect(status().isOk());
        perfil(ana, "sub_bloq_bruno").andExpect(status().isOk());
    }

    /** Desbloquear so' desfaz o proprio bloqueio: se o outro lado tambem
     * bloqueou, o bloqueio dele continua valendo. */
    @Test
    void desbloquearNaoDesfazOBloqueioDoOutroLado() throws Exception {
        bloqueio("PUT", ana, "sub_bloq_bruno").andExpect(status().isNoContent());
        bloqueio("PUT", bruno, "sub_bloq_ana").andExpect(status().isNoContent());

        bloqueio("DELETE", ana, "sub_bloq_bruno").andExpect(status().isNoContent());

        perfil(ana, "sub_bloq_bruno").andExpect(status().isNotFound());
    }

    @Test
    void bloquearAPropriaContaDa400EInexistente404() throws Exception {
        bloqueio("PUT", ana, "sub_bloq_ana").andExpect(status().isBadRequest());
        bloqueio("PUT", ana, "ninguem_tem").andExpect(status().isNotFound());
    }

    @Test
    void listaDeBloqueiosMostraQuemAContaBloqueou() throws Exception {
        bloqueio("PUT", ana, "sub_bloq_bruno").andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/bloqueios").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].username").value("sub_bloq_bruno"))
                .andExpect(jsonPath("$.itens[0].seguidoPorMim").value(false));
        mockMvc.perform(get("/api/v1/bloqueios").header(HttpHeaders.AUTHORIZATION, bearer(bruno)))
                .andExpect(jsonPath("$.itens").isEmpty());
    }

    /** Conta com bloqueio some da busca e das listas de seguidores de
     * terceiros, pra quem tem o bloqueio. */
    @Test
    void contaBloqueadaSomeDaBuscaEDasListasDeTerceiros() throws Exception {
        Usuario carla = contaComOnboarding(OidcTestUsers.principal("sub-bloq-carla", "Carla", "bloq-carla@exemplo.com"));
        seguimentoRepository.save(new Seguimento(bruno.getId(), carla.getId(), LocalDateTime.now()));
        bloqueio("PUT", ana, "sub_bloq_bruno").andExpect(status().isNoContent());

        assertThat(usernames(get("/api/v1/usuarios").param("busca", "sub_bloq"), ana))
                .containsExactly("sub_bloq_ana", "sub_bloq_carla");
        assertThat(usernames(get("/api/v1/usuarios/sub_bloq_carla/seguidores"), ana)).isEmpty();
        // quem nao tem bloqueio nenhum continua vendo o Bruno na lista
        assertThat(usernames(get("/api/v1/usuarios/sub_bloq_carla/seguidores"), carla)).containsExactly("sub_bloq_bruno");
    }

    /**
     * Mutuo em efeito tambem nos comentarios: num post publico de uma
     * terceira pessoa, o comentario do Bruno some pra Ana (e o total
     * acompanha), e continua visivel pra dona do post.
     */
    @Test
    void comentarioDeQuemTemBloqueioSomeParaQuemBloqueou() throws Exception {
        Usuario carla = contaComOnboarding(OidcTestUsers.principal("sub-bloq-carla", "Carla", "bloq-carla@exemplo.com"));
        Post post = postRepository.save(new Post(carla.getId(), "post da Carla", LocalDateTime.now()));
        comentarioRepository.save(new Comentario(post.getId(), bruno.getId(), "do Bruno", LocalDateTime.now()));
        comentarioRepository.save(new Comentario(post.getId(), carla.getId(), "da Carla", LocalDateTime.now()));
        bloqueio("PUT", ana, "sub_bloq_bruno").andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/posts/" + post.getId()).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.totalComentarios").value(1))
                .andExpect(jsonPath("$.comentariosRecentes[*].texto", contains("da Carla")));
        mockMvc.perform(get("/api/v1/posts/" + post.getId() + "/comentarios").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.itens.length()").value(1));
        mockMvc.perform(get("/api/v1/posts/" + post.getId()).header(HttpHeaders.AUTHORIZATION, bearer(carla)))
                .andExpect(jsonPath("$.totalComentarios").value(2));
    }

    private ResultActions bloqueio(String verbo, Usuario quem, String username) throws Exception {
        String rota = "/api/v1/usuarios/" + username + "/bloqueio";
        return mockMvc.perform((verbo.equals("PUT") ? put(rota) : delete(rota))
                .header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    private ResultActions perfil(Usuario quemVe, String username) throws Exception {
        return mockMvc.perform(get("/api/v1/usuarios/" + username).header(HttpHeaders.AUTHORIZATION, bearer(quemVe)));
    }

    private List<String> usernames(MockHttpServletRequestBuilder requisicao,
                                   Usuario quemVe) throws Exception {
        String corpo = mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, bearer(quemVe)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.itens[*].username");
    }
}
