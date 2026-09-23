package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Post e' a primeira entidade intencionalmente COMPARTILHADA entre
 * usuarios (ver Post.java) - o oposto do que IsolamentoEntreUsuariosIT
 * (Fase 6) prova pro resto do sistema. Esse teste existe pra deixar esse
 * comportamento explicito e protegido: se um dia alguem "corrigir" o feed
 * pra so mostrar posts do proprio usuario achando que e' um bug de
 * isolamento, este teste quebra e avisa que era intencional.
 */
@AutoConfigureMockMvc
@Transactional
class PostFeedIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    private final OidcUser usuarioA = OidcTestUsers.principal("sub-feed-a", "Usuaria A", "feed-a@exemplo.com");
    private final OidcUser usuarioB = OidcTestUsers.principal("sub-feed-b", "Usuario B", "feed-b@exemplo.com");

    @Test
    void postDeUmUsuarioApareceNoFeedDeOutro() throws Exception {
        usuarioAtualService.obterUsuarioAtual(usuarioA);
        usuarioAtualService.obterUsuarioAtual(usuarioB);

        mockMvc.perform(post("/feed").with(oidcLogin().oidcUser(usuarioA)).with(csrf())
                        .param("texto", "Bati meu recorde de barra fixa hoje!"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/feed").with(oidcLogin().oidcUser(usuarioB)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bati meu recorde de barra fixa hoje!")))
                .andExpect(content().string(containsString("Usuaria A")));
    }

    @Test
    void postEmBrancoNaoPassaNaValidacaoENaoEhPersistido() throws Exception {
        usuarioAtualService.obterUsuarioAtual(usuarioA);

        mockMvc.perform(post("/feed").with(oidcLogin().oidcUser(usuarioA)).with(csrf())
                        .param("texto", "   "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escreva algo antes de postar")));

        mockMvc.perform(get("/feed").with(oidcLogin().oidcUser(usuarioA)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ninguém postou nada ainda")));
    }
}
