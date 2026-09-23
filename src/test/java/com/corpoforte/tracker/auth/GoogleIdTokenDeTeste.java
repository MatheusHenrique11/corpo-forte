package com.corpoforte.tracker.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.Date;

/**
 * Faz o papel do Google nos testes: assina ID tokens com uma chave RSA
 * gerada aqui, e o VerificadorIdTokenGoogle de teste confere com a chave
 * publica correspondente em vez de buscar o JWKS real. Todo o resto da
 * validacao (emissor, audience, expiracao) e' o codigo de producao.
 */
public final class GoogleIdTokenDeTeste {

    public static final String CLIENT_ID_WEB = "cliente-web-teste.apps.googleusercontent.com";
    public static final String CLIENT_ID_SEGUNDO_CLIENTE = "segundo-cliente-teste.apps.googleusercontent.com";

    private static final RSAKey CHAVE_DO_GOOGLE = novaChave();

    private GoogleIdTokenDeTeste() {
    }

    /** Claims de um ID token valido emitido pro cliente web; o teste altera
     * o que quiser antes de assinar. */
    public static JWTClaimsSet.Builder claims(String sub, String email) {
        Instant agora = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID_WEB)
                .subject(sub)
                .claim("email", email)
                .claim("email_verified", true)
                .claim("name", "Nome de " + sub)
                .issueTime(Date.from(agora))
                .expirationTime(Date.from(agora.plusSeconds(3600)));
    }

    public static String idToken(String sub, String email) {
        return assinar(claims(sub, email).build());
    }

    public static String assinar(JWTClaimsSet claims) {
        return assinar(claims, CHAVE_DO_GOOGLE);
    }

    public static String assinar(JWTClaimsSet claims, RSAKey chave) {
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(chave.getKeyID()).build(), claims);
            jwt.sign(new RSASSASigner(chave));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Chave que o verificador de teste NAO conhece - simula token forjado. */
    public static RSAKey novaChave() {
        try {
            return new RSAKeyGenerator(2048).keyID("chave-" + System.nanoTime()).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    static NimbusJwtDecoder decoderComChaveDoGoogle() {
        try {
            return NimbusJwtDecoder.withPublicKey(CHAVE_DO_GOOGLE.toRSAPublicKey()).build();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
