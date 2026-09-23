package com.corpoforte.tracker;

import com.corpoforte.tracker.auth.EmissorTokens;
import com.corpoforte.tracker.auth.GoogleDeTesteConfig;
import com.corpoforte.tracker.auth.GoogleIdTokenDeTeste;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para testes de integracao (contexto Spring completo + banco real).
 * Sobe um Postgres via Testcontainers em vez de H2: as migrations do Flyway
 * usam recursos especificos do Postgres (bigserial, double precision) e o
 * ddl-auto e' "validate" - testar contra outro banco esconderia
 * incompatibilidade real que so apareceria em produção.
 *
 * O container e' compartilhado entre todas as classes de teste de
 * integracao: inicializado uma unica vez no bloco estatico (nao gerenciado
 * pela extensao @Testcontainers de proposito, pra nao ser derrubado depois
 * da primeira classe de teste) e encerrado pelo Ryuk do Testcontainers
 * quando a JVM termina. Isso evita pagar o custo de subir um Postgres novo
 * a cada classe de teste.
 *
 * @TestPropertySource (nao um src/test/resources/application.yml) pro
 * registro OAuth2 fake (Fase 6): um application.yml em src/test/resources
 * SUBSTITUI o principal inteiro no classpath de teste em vez de mesclar -
 * "open-in-view: false" (que evita esconder LazyInitializationException,
 * ver Fase 4) desaparecia silenciosamente nos testes. @TestPropertySource
 * so adiciona/sobrescreve as propriedades listadas, mantendo o resto do
 * application.yml principal em vigor.
 *
 * GoogleDeTesteConfig (Fase 10) entra aqui, e nao em cada teste de API,
 * pelo mesmo motivo do container compartilhado: configuracao diferente
 * por classe faria o Spring subir um contexto novo pra cada uma.
 */
@SpringBootTest
@Import(GoogleDeTesteConfig.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=teste-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=teste-client-secret",
        "spring.security.oauth2.client.registration.google.scope=openid,profile,email",
        "app.owner-email=" + IntegrationTestBase.OWNER_EMAIL,
        "app.auth.google.client-ids=" + GoogleIdTokenDeTeste.CLIENT_ID_WEB + "," + GoogleIdTokenDeTeste.CLIENT_ID_SEGUNDO_CLIENTE,
        "app.cors.origins=" + IntegrationTestBase.ORIGEM_WEB_PERMITIDA
})
public abstract class IntegrationTestBase {

    /** Unica origem liberada no CORS da API nos testes (Fase 10). */
    public static final String ORIGEM_WEB_PERMITIDA = "http://localhost:5173";

    /** E-mail configurado como app.owner-email nos testes (Fase 6) - o
     * unico que pode reivindicar uma conta local orfa. */
    public static final String OWNER_EMAIL = "dona-da-conta@exemplo.com";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    private EmissorTokens emissorTokens;

    @Autowired
    private UsuarioAtualService usuarioAtualServiceDaBase;

    @Autowired
    private UsuarioRepository usuarioRepositoryDaBase;

    /**
     * Conta pronta pra usar a API: primeiro login + onboarding concluido
     * (Fase 12), sem mexer nos dados do perfil. Username derivado do sub,
     * no formato permitido. Conta so' com login (obterUsuarioAtual) recebe
     * onboarding-pendente em quase toda rota da API.
     */
    protected Usuario contaComOnboarding(OidcUser principal) {
        Usuario usuario = usuarioAtualServiceDaBase.obterUsuarioAtual(principal);
        if (usuario.isOnboardingConcluido()) {
            return usuario;
        }
        String username = principal.getSubject().toLowerCase().replaceAll("[^a-z0-9_.]", "_");
        usuario.concluirOnboarding(usuario.getNome(), username.substring(0, Math.min(username.length(), 30)),
                usuario.getAlturaCm(), usuario.getIdade(), usuario.getObjetivo(), usuario.getNivel());
        return usuarioRepositoryDaBase.save(usuario);
    }

    /** Valor do cabecalho Authorization com um access token valido pro
     * usuario - o que um cliente da API manda em toda requisicao. */
    protected String bearer(Usuario usuario) {
        return "Bearer " + emissorTokens.emitir(usuario.getId()).accessToken();
    }
}
