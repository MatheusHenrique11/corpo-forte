package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 7b. PostFeedIT ja prova que post e' compartilhado entre contas;
 * aqui o que se prova e' a INTERACAO em cima do post de outra pessoa -
 * comentar e curtir - que e' a razao de a Fase 7a ter aberto essa excecao
 * ao isolamento da Fase 6.
 *
 * Os dois usuarios sao resolvidos por UsuarioAtualService no setup e
 * usados via oidcLogin().oidcUser(...) nas chamadas (OidcTestUsers garante
 * que os dois caminhos caem no mesmo Usuario).
 */
@AutoConfigureMockMvc
@Transactional
class ComentarioCurtidaIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private PostService postService;

    private final OidcUser usuarioA = OidcTestUsers.principal("sub-social-a", "Usuaria A", "social-a@exemplo.com");
    private final OidcUser usuarioB = OidcTestUsers.principal("sub-social-b", "Usuario B", "social-b@exemplo.com");

    @Test
    void comentarioDeUmUsuarioNoPostDeOutroApareceComONomeDeQuemComentou() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        usuarioAtualService.obterUsuarioAtual(usuarioB);
        Long postDeA = publicar(usuarioA, "Consegui minha primeira barra fixa!", a);

        mockMvc.perform(post("/feed/posts/" + postDeA + "/comentarios")
                        .with(oidcLogin().oidcUser(usuarioB)).with(csrf())
                        .param("texto", "Boa! Agora vai pras 10"))
                .andExpect(status().is3xxRedirection());

        List<PostView> feed = postService.listarFeed(a.getId());

        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).comentarios()).hasSize(1);
        assertThat(feed.get(0).comentarios().get(0).texto()).isEqualTo("Boa! Agora vai pras 10");
        assertThat(feed.get(0).comentarios().get(0).autorNome()).isEqualTo("Usuario B");
    }

    /**
     * Renderiza o feed de verdade com comentario E curtida presentes: os
     * ramos do template que so' existem nesse estado (th:each dos
     * comentarios, coracao preenchido de curtidoPorMim, plural de
     * "comentarios") nao sao exercitados por nenhum outro teste - os
     * demais POSTam e conferem o resultado pelo PostService, sem passar
     * pelo Thymeleaf. Sem isto, um erro de expressao no template so'
     * apareceria pro usuario (foi assim que a Fase 4 descobriu o
     * LazyInitializationException, testando manual).
     */
    @Test
    void feedRenderizaPostComComentarioECurtida() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        usuarioAtualService.obterUsuarioAtual(usuarioB);
        Long postDeA = publicar(usuarioA, "Fechei o ciclo de 8 semanas", a);

        curtir(usuarioB, postDeA);
        mockMvc.perform(post("/feed/posts/" + postDeA + "/comentarios")
                        .with(oidcLogin().oidcUser(usuarioB)).with(csrf())
                        .param("texto", "Monstro!"))
                .andExpect(status().is3xxRedirection());

        // o estado do botao e' conferido pela classe/title, nao pelo
        // caractere do coracao: a entidade HTML do template (&#9829;) e'
        // decodificada pelo parser do Thymeleaf antes do SpEL, entao o que
        // sai na resposta e' o caractere literal - detalhe de encoding que
        // nao e' o que este teste quer travar.
        mockMvc.perform(get("/feed").with(oidcLogin().oidcUser(usuarioB)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Monstro!")))
                .andExpect(content().string(containsString("Usuario B")))
                .andExpect(content().string(containsString("1 comentário")))
                .andExpect(content().string(containsString("class=\"curtir curtido\" title=\"Descurtir\"")));

        // mesmo post, outra conta: mesma contagem, botao no estado "curtir"
        mockMvc.perform(get("/feed").with(oidcLogin().oidcUser(usuarioA)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Monstro!")))
                .andExpect(content().string(containsString("title=\"Curtir\"")))
                .andExpect(content().string(not(containsString("class=\"curtir curtido\""))));
    }

    @Test
    void curtirEDepoisCurtirDeNovoDescurte() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        Usuario b = usuarioAtualService.obterUsuarioAtual(usuarioB);
        Long postDeA = publicar(usuarioA, "Treino de perna concluído", a);

        curtir(usuarioB, postDeA);

        assertThat(postService.listarFeed(b.getId()).get(0).curtidas()).isEqualTo(1);
        assertThat(postService.listarFeed(b.getId()).get(0).curtidoPorMim()).isTrue();
        // quem nao curtiu ve a mesma contagem, mas curtidoPorMim proprio
        assertThat(postService.listarFeed(a.getId()).get(0).curtidas()).isEqualTo(1);
        assertThat(postService.listarFeed(a.getId()).get(0).curtidoPorMim()).isFalse();

        curtir(usuarioB, postDeA);

        assertThat(postService.listarFeed(b.getId()).get(0).curtidas()).isZero();
        assertThat(postService.listarFeed(b.getId()).get(0).curtidoPorMim()).isFalse();
    }

    @Test
    void comentarioEmBrancoNaoPersisteEOErroSaiSoNoPostEmQueOUsuarioErrou() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        publicar(usuarioA, "Primeiro post", a);
        Long segundoPost = publicar(usuarioA, "Segundo post", a);

        String html = mockMvc.perform(post("/feed/posts/" + segundoPost + "/comentarios")
                        .with(oidcLogin().oidcUser(usuarioA)).with(csrf())
                        .param("texto", "   "))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // dois posts na tela, uma caixa de comentario em cada: a mensagem
        // nao pode aparecer embaixo das duas
        assertThat(html).containsOnlyOnce("Escreva algo antes de comentar");
        assertThat(postService.listarFeed(a.getId()))
                .allSatisfy(post -> assertThat(post.comentarios()).isEmpty());
    }

    private Long publicar(OidcUser autor, String texto, Usuario leitor) throws Exception {
        mockMvc.perform(post("/feed").with(oidcLogin().oidcUser(autor)).with(csrf())
                        .param("texto", texto))
                .andExpect(status().is3xxRedirection());

        return postService.listarFeed(leitor.getId()).stream()
                .filter(post -> post.texto().equals(texto))
                .findFirst().orElseThrow().id();
    }

    private void curtir(OidcUser usuario, Long postId) throws Exception {
        mockMvc.perform(post("/feed/posts/" + postId + "/curtir")
                        .with(oidcLogin().oidcUser(usuario)).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
