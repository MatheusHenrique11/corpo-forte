package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 7c. O acesso cruzado (quem NAO pode apagar) e' coberto em
 * EndpointsComIdIT, junto dos outros endpoints com ID; aqui fica o caminho
 * feliz e o que so' o banco garante.
 */
@AutoConfigureMockMvc
@Transactional
class ApagarConteudoIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private CurtidaRepository curtidaRepository;

    @Autowired
    private EntityManager entityManager;

    private final OidcUser usuarioA = OidcTestUsers.principal("sub-apagar-a", "Usuaria A", "apagar-a@exemplo.com");
    private final OidcUser usuarioB = OidcTestUsers.principal("sub-apagar-b", "Usuario B", "apagar-b@exemplo.com");
    private final OidcUser usuarioC = OidcTestUsers.principal("sub-apagar-c", "Usuaria C", "apagar-c@exemplo.com");

    /**
     * Quem segura "nada fica orfao" e' o on delete cascade da V11, nao o
     * service - sem a migration, o flush abaixo estoura violacao de FK. O
     * flush/clear e' obrigatorio: o teste e' @Transactional, entao o
     * DELETE so' chega no Postgres quando a sessao descarrega, e sem o
     * clear o Hibernate responderia da memoria em vez de consultar o banco.
     */
    @Test
    void apagarPostLevaComentariosECurtidasJuntoNoBanco() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        Usuario b = usuarioAtualService.obterUsuarioAtual(usuarioB);
        Post post = postRepository.saveAndFlush(new Post(a.getId(), "Post que vai sumir", LocalDateTime.now()));
        comentarioRepository.saveAndFlush(new Comentario(post.getId(), b.getId(), "Boa!", LocalDateTime.now()));
        curtidaRepository.saveAndFlush(new Curtida(post.getId(), b.getId(), LocalDateTime.now()));
        entityManager.clear();

        mockMvc.perform(post("/feed/posts/" + post.getId() + "/apagar")
                        .with(oidcLogin().oidcUser(usuarioA)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        entityManager.flush();
        entityManager.clear();

        assertThat(postRepository.existsById(post.getId())).isFalse();
        assertThat(comentarioRepository.findAll()).noneMatch(comentario -> comentario.getPostId().equals(post.getId()));
        assertThat(curtidaRepository.contarPorPost(List.of(post.getId()))).isEmpty();
    }

    @Test
    void autorDoComentarioApagaOProprioComentarioEmPostDeOutraPessoa() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        Usuario b = usuarioAtualService.obterUsuarioAtual(usuarioB);
        Post post = postRepository.saveAndFlush(new Post(a.getId(), "Post da A", LocalDateTime.now()));
        Comentario comentario = comentarioRepository.saveAndFlush(
                new Comentario(post.getId(), b.getId(), "Comentário do B", LocalDateTime.now()));

        mockMvc.perform(post("/feed/comentarios/" + comentario.getId() + "/apagar")
                        .with(oidcLogin().oidcUser(usuarioB)).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(comentarioRepository.existsById(comentario.getId())).isFalse();
        assertThat(postRepository.existsById(post.getId())).isTrue();
    }

    /**
     * O botao so' aparece pra quem o servidor autorizaria - a tela usa a
     * mesma regra (PostService.podeApagarComentario) que o POST aplica.
     * Post da A com comentario do B, visto por tres pessoas diferentes.
     */
    @Test
    void botaoDeApagarSoApareceParaQuemPodeApagar() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        Usuario b = usuarioAtualService.obterUsuarioAtual(usuarioB);
        usuarioAtualService.obterUsuarioAtual(usuarioC);
        Post post = postRepository.saveAndFlush(new Post(a.getId(), "Post da A", LocalDateTime.now()));
        comentarioRepository.saveAndFlush(new Comentario(post.getId(), b.getId(), "Comentário do B", LocalDateTime.now()));

        // A: dona do post - apaga o post e modera o comentario do B
        assertThat(feedVistoPor(usuarioA))
                .containsOnlyOnce("title=\"Apagar post\"")
                .containsOnlyOnce("title=\"Apagar comentário\"");

        // B: apaga so' o proprio comentario, nao o post da A
        assertThat(feedVistoPor(usuarioB))
                .doesNotContain("title=\"Apagar post\"")
                .containsOnlyOnce("title=\"Apagar comentário\"");

        // C: nao tem nada dele ali
        assertThat(feedVistoPor(usuarioC))
                .doesNotContain("title=\"Apagar post\"")
                .doesNotContain("title=\"Apagar comentário\"");
    }

    private String feedVistoPor(OidcUser usuario) throws Exception {
        return mockMvc.perform(get("/feed").with(oidcLogin().oidcUser(usuario)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
