package com.corpoforte.tracker.auth;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ferramentas do perfil dev (Fase 10): o token emitido pra sessao web e a
 * especificacao OpenAPI. E' a unica classe de teste com perfil proprio, e
 * por isso sobe um contexto Spring a mais. O outro lado - nada disso
 * existe fora do dev - fica em SecurityConfigIT.
 */
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@Transactional
class TokenDevIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void tokenDevAutenticaAApiComoOUsuarioDaSessaoWeb() throws Exception {
        OidcUser principal = OidcTestUsers.principal("sub-token-dev", "Fulana", "token-dev@exemplo.com");
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);

        String resposta = mockMvc.perform(get("/dev/token-api").with(oidcLogin().oidcUser(principal)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TokensResposta tokens = objectMapper.readValue(resposta, TokensResposta.class);

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getId()));
    }

    @Test
    void tokenDevExigeSessaoWeb() throws Exception {
        mockMvc.perform(get("/dev/token-api"))
                .andExpect(status().is3xxRedirection());
    }

    /** A especificacao e' o contrato dos clientes: so' a API entra, as
     * rotas de autenticacao se declaram publicas e o resto exige o bearer. */
    @Test
    void especificacaoOpenApiDescreveSoAApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/google'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/google'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/me'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/feed/descobrir'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}/curtida'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/treino-do-dia/itens/{itemId}/conclusao'].delete").exists())
                .andExpect(jsonPath("$.security[0].accessToken").exists())
                .andExpect(jsonPath("$.components.securitySchemes.accessToken.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/perfil']").doesNotExist())
                .andExpect(jsonPath("$.paths['/dev/token-api']").doesNotExist());
    }

    @Test
    void swaggerUiAbreSemLogin() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
