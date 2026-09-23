package com.corpoforte.tracker.auth;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.IdentidadeExternaRepository;
import com.corpoforte.tracker.usuario.NivelTreino;
import com.corpoforte.tracker.usuario.ObjetivoTreino;
import com.corpoforte.tracker.usuario.Provedor;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida da sessao da API (Fase 10): login com ID token do Google,
 * refresh rotativo, deteccao de reuso e logout.
 *
 * SEM @Transactional, de proposito, ao contrario dos outros testes de
 * integracao: RefreshTokenService depende de cada chamada ao repository
 * commitar sozinha - a revogacao de todas as sessoes precisa sobreviver ao
 * 401 que vem logo depois. Numa transacao de teste envolvendo tudo, esse
 * comportamento nem seria observavel. Em troca, o teste apaga as contas que
 * criou (o cascade leva identidade e refresh tokens), porque os outros
 * testes contam linhas do banco inteiro.
 *
 * Nenhum POST aqui usa .with(csrf()): a API nao tem CSRF, e se alguem
 * ligar por engano, todos estes testes quebram com 403.
 */
@AutoConfigureMockMvc
class AuthApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private IdentidadeExternaRepository identidadeExternaRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    private final Set<String> subsUsados = new HashSet<>();
    private final Set<Long> usuariosCriados = new HashSet<>();

    @AfterEach
    void apagarContasCriadas() {
        subsUsados.forEach(sub -> identidadeExternaRepository.findByProvedorAndSub(Provedor.GOOGLE, sub)
                .ifPresent(identidade -> usuariosCriados.add(identidade.getUsuarioId())));
        usuarioRepository.deleteAllById(usuariosCriados);
    }

    @Test
    void loginDevolveParDeTokensEOAccessTokenAutenticaAApi() throws Exception {
        TokensResposta tokens = loginGoogle("sub-api-login", "api-login@exemplo.com");

        assertThat(tokens.tipo()).isEqualTo("Bearer");
        assertThat(tokens.expiraEmSegundos()).isEqualTo(900);
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();

        me(tokens.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome de sub-api-login"))
                .andExpect(jsonPath("$.email").value("api-login@exemplo.com"))
                .andExpect(jsonPath("$.fotoUrl").value(GoogleIdTokenDeTeste.fotoDe("sub-api-login")))
                // dado corporal nunca sai na API sem uma decisao explicita
                .andExpect(jsonPath("$.pesoKg").doesNotExist())
                .andExpect(jsonPath("$.alturaCm").doesNotExist())
                .andExpect(jsonPath("$.idade").doesNotExist());
    }

    @Test
    void loginPelaApiCaiNaMesmaContaDaSessaoWeb() throws Exception {
        subsUsados.add("sub-duas-portas-api");
        Usuario pelaWeb = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-duas-portas-api", "Fulana", "duas-portas@exemplo.com"));

        TokensResposta tokens = loginGoogle("sub-duas-portas-api", "duas-portas@exemplo.com");

        me(tokens.accessToken()).andExpect(jsonPath("$.id").value(pelaWeb.getId()));
    }

    @Test
    void loginPelaApiDoDonoConfiguradoReivindicaAContaOrfa() throws Exception {
        Usuario orfa = usuarioRepository.save(new Usuario(
                "Meu Perfil", 82.0, 178, 30, ObjetivoTreino.PERDA_GORDURA, NivelTreino.INTERMEDIARIO));
        usuariosCriados.add(orfa.getId());

        TokensResposta tokens = loginGoogle("sub-dona-pela-api", OWNER_EMAIL);

        me(tokens.accessToken()).andExpect(jsonPath("$.id").value(orfa.getId()));
    }

    @Test
    void idTokenDeOutroAplicativoRecebe401ENaoCriaConta() throws Exception {
        String deOutroApp = GoogleIdTokenDeTeste.assinar(GoogleIdTokenDeTeste.claims("sub-outro-app", "outro@exemplo.com")
                .audience("outro-app.apps.googleusercontent.com").build());

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idToken", deOutroApp))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Token do Google inválido"));

        assertThat(identidadeExternaRepository.findByProvedorAndSub(Provedor.GOOGLE, "sub-outro-app")).isEmpty();
    }

    @Test
    void refreshTrocaOParInteiroENovoAccessTokenFunciona() throws Exception {
        TokensResposta primeiro = loginGoogle("sub-api-refresh", "api-refresh@exemplo.com");

        TokensResposta renovado = renovar(primeiro.refreshToken());

        assertThat(renovado.refreshToken()).isNotEqualTo(primeiro.refreshToken());
        me(renovado.accessToken()).andExpect(status().isOk());
    }

    /**
     * Refresh token ja trocado aparecendo de novo = copia roubada (o dono
     * legitimo ja esta com o sucessor). A resposta derruba TODAS as sessoes
     * da conta, inclusive a de outro aparelho - o dono loga de novo, quem
     * roubou perde o acesso.
     */
    @Test
    void refreshReusadoEncerraTodasAsSessoesDaConta() throws Exception {
        TokensResposta celular = loginGoogle("sub-api-roubo", "api-roubo@exemplo.com");
        TokensResposta notebook = loginGoogle("sub-api-roubo", "api-roubo@exemplo.com");
        TokensResposta celularRenovado = renovar(celular.refreshToken());

        renovacaoRecusada(celular.refreshToken());

        renovacaoRecusada(celularRenovado.refreshToken());
        renovacaoRecusada(notebook.refreshToken());
        Long usuarioId = identidadeExternaRepository.findByProvedorAndSub(Provedor.GOOGLE, "sub-api-roubo")
                .orElseThrow().getUsuarioId();
        assertThat(refreshTokenRepository.findByUsuarioId(usuarioId)).allMatch(RefreshToken::revogado);
    }

    @Test
    void logoutEncerraSoAquelaSessaoEEIdempotente() throws Exception {
        TokensResposta celular = loginGoogle("sub-api-logout", "api-logout@exemplo.com");
        TokensResposta notebook = loginGoogle("sub-api-logout", "api-logout@exemplo.com");

        sair(celular.refreshToken()).andExpect(status().isNoContent());

        renovacaoRecusada(celular.refreshToken());
        // token de logout reapresentado nao e' tratado como roubo: a outra
        // sessao da conta continua valendo
        renovar(notebook.refreshToken());
        sair(celular.refreshToken()).andExpect(status().isNoContent());
        sair("token-que-nunca-existiu").andExpect(status().isNoContent());
    }

    /** Cliente costuma anexar o access token (ja expirado) ate no refresh. */
    @Test
    void refreshFuncionaMesmoComAccessTokenInvalidoNoCabecalho() throws Exception {
        TokensResposta tokens = loginGoogle("sub-api-cabecalho", "api-cabecalho@exemplo.com");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token.expirado.qualquer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", tokens.refreshToken()))))
                .andExpect(status().isOk());
    }

    private TokensResposta loginGoogle(String sub, String email) throws Exception {
        subsUsados.add(sub);
        String resposta = mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idToken", GoogleIdTokenDeTeste.idToken(sub, email)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(resposta, TokensResposta.class);
    }

    private TokensResposta renovar(String refreshToken) throws Exception {
        String resposta = pedirRefresh(refreshToken)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(resposta, TokensResposta.class);
    }

    private void renovacaoRecusada(String refreshToken) throws Exception {
        pedirRefresh(refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private ResultActions pedirRefresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("refreshToken", refreshToken))));
    }

    private ResultActions sair(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("refreshToken", refreshToken))));
    }

    private ResultActions me(String accessToken) throws Exception {
        return mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
    }

    private String json(Map<String, String> corpo) throws Exception {
        return objectMapper.writeValueAsString(corpo);
    }
}
