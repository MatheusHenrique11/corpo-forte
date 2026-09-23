package com.corpoforte.tracker.config;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.auth.EmissorTokens;
import com.corpoforte.tracker.auth.JwtConfig;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O que a chain da API (Fase 10) aceita e recusa como credencial. Todo
 * access token que nao foi emitido por este back-end, para esta conta e
 * dentro da validade, recebe 401 em ProblemDetail - inclusive um cookie de
 * sessao web valido, que a API ignora de proposito.
 */
@AutoConfigureMockMvc
@Transactional
class ApiSegurancaIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmissorTokens emissorTokens;

    @Autowired
    private SecretKey chaveAccessToken;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    private final OidcUser principal = OidcTestUsers.principal("sub-api-seguranca", "Fulana", "api-seguranca@exemplo.com");
    private Usuario usuario;

    @BeforeEach
    void criarUsuario() {
        usuario = usuarioAtualService.obterUsuarioAtual(principal);
    }

    @Test
    void tokenEmitidoPeloBackEndAutentica() throws Exception {
        me(emissorTokens.emitir(usuario.getId()).accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getId()));
    }

    @Test
    void semTokenRecebe401EmProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Token de acesso ausente"))
                .andExpect(jsonPath("$.instance").value("/api/v1/me"));
    }

    /** Mesmo formato e mesmos claims, mas assinado com outro segredo: e'
     * o que alguem sem acesso ao APP_JWT_SECRET conseguiria produzir. */
    @Test
    void tokenAssinadoComOutroSegredoRecebe401() throws Exception {
        byte[] outroSegredo = new byte[32];
        new SecureRandom().nextBytes(outroSegredo);

        String forjado = assinar(new SecretKeySpec(outroSegredo, "HmacSHA256"),
                claimsValidos().build());

        recusado(forjado);
    }

    @Test
    void tokenExpiradoRecebe401() throws Exception {
        Instant umaHoraAtras = Instant.now().minusSeconds(3600);

        recusado(assinar(chaveAccessToken, claimsValidos()
                .issuedAt(umaHoraAtras.minusSeconds(900))
                .expiresAt(umaHoraAtras)
                .build()));
    }

    @Test
    void tokenDeOutroEmissorComAMesmaChaveRecebe401() throws Exception {
        recusado(assinar(chaveAccessToken, claimsValidos().issuer("outro-sistema").build()));
    }

    /** Token legitimo com o "sub" trocado pelo id de outra conta: a
     * assinatura deixa de bater e o token inteiro e' recusado. */
    @Test
    void tokenAdulteradoParaOutraContaRecebe401() throws Exception {
        String[] partes = emissorTokens.emitir(usuario.getId()).accessToken().split("\\.");
        Map<String, Object> payload = objectMapper.readValue(
                Base64.getUrlDecoder().decode(partes[1]), new TypeReference<>() {
                });
        payload.put("sub", String.valueOf(usuario.getId() + 1));
        String payloadAdulterado = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));

        recusado(partes[0] + "." + payloadAdulterado + "." + partes[2]);
    }

    /** Token valido, assinatura certa, mas a conta nao existe mais: 401
     * (o token deixou de representar alguem), nao 500. */
    @Test
    void tokenDeContaApagadaRecebe401() throws Exception {
        String accessToken = emissorTokens.emitir(usuario.getId()).accessToken();
        usuarioRepository.delete(usuario);
        entityManager.flush();
        entityManager.clear();

        me(accessToken)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /**
     * A mesma sessao que abre /perfil nao abre a API. Se abrisse, um site
     * terceiro poderia chamar a API no navegador de quem esta logado,
     * carregando o cookie junto - exatamente o CSRF que a chain da API
     * desliga por nao usar cookie.
     */
    @Test
    void sessaoWebValidaNaoAutenticaAApi() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google")));

        mockMvc.perform(get("/perfil").session(sessao))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/me").session(sessao))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsLiberaAOrigemConfigurada() throws Exception {
        mockMvc.perform(options("/api/v1/me")
                        .header(HttpHeaders.ORIGIN, ORIGEM_WEB_PERMITIDA)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_WEB_PERMITIDA));
    }

    @Test
    void corsRecusaOrigemNaoConfigurada() throws Exception {
        mockMvc.perform(options("/api/v1/me")
                        .header(HttpHeaders.ORIGIN, "https://site-qualquer.exemplo")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private JwtClaimsSet.Builder claimsValidos() {
        Instant agora = Instant.now();
        return JwtClaimsSet.builder()
                .issuer(JwtConfig.EMISSOR)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(900));
    }

    private static String assinar(SecretKey chave, JwtClaimsSet claims) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private void recusado(String accessToken) throws Exception {
        me(accessToken)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Token de acesso inválido ou expirado"));
    }

    private ResultActions me(String accessToken) throws Exception {
        return mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
    }
}
