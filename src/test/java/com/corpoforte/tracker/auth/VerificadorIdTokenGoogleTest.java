package com.corpoforte.tracker.auth;

import com.corpoforte.tracker.usuario.DadosLogin;
import com.corpoforte.tracker.usuario.Provedor;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static com.corpoforte.tracker.auth.GoogleIdTokenDeTeste.CLIENT_ID_SEGUNDO_CLIENTE;
import static com.corpoforte.tracker.auth.GoogleIdTokenDeTeste.CLIENT_ID_WEB;
import static com.corpoforte.tracker.auth.GoogleIdTokenDeTeste.assinar;
import static com.corpoforte.tracker.auth.GoogleIdTokenDeTeste.claims;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cada validacao do ID token do Google, isolada. So' a chave publica e' de
 * teste (GoogleIdTokenDeTeste); os validadores sao os de producao.
 */
class VerificadorIdTokenGoogleTest {

    private final VerificadorIdTokenGoogle verificador = verificadorAceitando(List.of(CLIENT_ID_WEB, CLIENT_ID_SEGUNDO_CLIENTE));

    @Test
    void idTokenValidoViraDadosLoginDoGoogle() {
        DadosLogin login = verificador.verificar(assinar(claims("sub-1", "fulana@exemplo.com").build()));

        assertThat(login).isEqualTo(new DadosLogin(Provedor.GOOGLE, "sub-1", "fulana@exemplo.com", true,
                "Nome de sub-1", GoogleIdTokenDeTeste.fotoDe("sub-1")));
    }

    @Test
    void aceitaQualquerUmDosClientIdsConfigurados() {
        String doSegundoCliente = assinar(claims("sub-1", "fulana@exemplo.com").audience(CLIENT_ID_SEGUNDO_CLIENTE).build());

        assertThat(verificador.verificar(doSegundoCliente).sub()).isEqualTo("sub-1");
    }

    /** O Google emite com as duas formas de "iss". */
    @Test
    void aceitaEmissorDoGoogleSemHttps() {
        String semHttps = assinar(claims("sub-1", "fulana@exemplo.com").issuer("accounts.google.com").build());

        assertThat(verificador.verificar(semHttps).sub()).isEqualTo("sub-1");
    }

    /**
     * O caso que mais importa: um ID token legitimo do Google, da mesma
     * pessoa, mas emitido pra OUTRO site/app. Aceitar isso deixaria
     * qualquer servico que usa "Entrar com Google" entrar na conta dela
     * aqui.
     */
    @Test
    void recusaIdTokenEmitidoParaOutroAplicativo() {
        String deOutroApp = assinar(claims("sub-1", "fulana@exemplo.com")
                .audience("outro-app.apps.googleusercontent.com").build());

        assertRecusado(deOutroApp);
    }

    @Test
    void recusaEmissorQueNaoEOGoogle() {
        assertRecusado(assinar(claims("sub-1", "fulana@exemplo.com").issuer("https://emissor-falso.exemplo").build()));
    }

    @Test
    void recusaIdTokenExpirado() {
        Instant duasHorasAtras = Instant.now().minusSeconds(7200);
        String expirado = assinar(claims("sub-1", "fulana@exemplo.com")
                .issueTime(Date.from(duasHorasAtras.minusSeconds(3600)))
                .expirationTime(Date.from(duasHorasAtras))
                .build());

        assertRecusado(expirado);
    }

    @Test
    void recusaIdTokenAssinadoPorChaveQueNaoEDoGoogle() {
        String forjado = assinar(claims("sub-1", "fulana@exemplo.com").build(), GoogleIdTokenDeTeste.novaChave());

        assertRecusado(forjado);
    }

    @Test
    void recusaTextoQueNemEJwt() {
        assertRecusado("nao-e-um-jwt");
    }

    /** Sem APP_GOOGLE_CLIENT_IDS o login pela API fica fechado, nunca aberto. */
    @Test
    void semClientIdConfiguradoRecusaTudo() {
        VerificadorIdTokenGoogle semConfiguracao = verificadorAceitando(List.of(""));

        assertThatThrownBy(() -> semConfiguracao.verificar(assinar(claims("sub-1", "fulana@exemplo.com").build())))
                .isInstanceOf(ResponseStatusException.class);
    }

    /** email_verified e' o que libera a reivindicacao da conta orfa - lido
     * nos dois formatos que o Google ja usou, e falso quando ausente. */
    @Test
    void emailVerifiedEmTextoOuAusente() {
        JWTClaimsSet comTexto = claims("sub-1", "fulana@exemplo.com").claim("email_verified", "true").build();
        JWTClaimsSet semClaim = claims("sub-2", "fulano@exemplo.com").claim("email_verified", null).build();

        assertThat(verificador.verificar(assinar(comTexto)).emailVerificado()).isTrue();
        assertThat(verificador.verificar(assinar(semClaim)).emailVerificado()).isFalse();
    }

    private void assertRecusado(String idToken) {
        assertThatThrownBy(() -> verificador.verificar(idToken))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private static VerificadorIdTokenGoogle verificadorAceitando(List<String> clientIds) {
        return new VerificadorIdTokenGoogle(GoogleIdTokenDeTeste.decoderComChaveDoGoogle(), clientIds);
    }
}
