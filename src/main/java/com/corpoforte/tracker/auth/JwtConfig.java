package com.corpoforte.tracker.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Access token da API: JWT HS256 assinado e validado so' por este back-end.
 * Chave simetrica (e nao par RSA + JWKS publico) porque ninguem de fora
 * precisa validar esse token - o cliente so' o carrega de volta.
 *
 * O JwtDecoder daqui e' o que o resource server da chain /api/** usa
 * (SecurityConfig). Ele NAO participa do login web: o oauth2Login valida o
 * ID token do Google pela propria fabrica de decoders.
 */
@Configuration
public class JwtConfig {

    public static final String EMISSOR = "corpo-forte";

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final int TAMANHO_MINIMO_BYTES = 32;

    /**
     * Sem APP_JWT_SECRET, gera um segredo aleatorio a cada boot em vez de
     * cair num valor padrao escrito aqui: o repositorio e' publico, e um
     * segredo padrao no codigo deixaria qualquer um forjar token num
     * servidor que esquecesse de configurar o proprio. O custo do
     * aleatorio e' so' os tokens emitidos morrerem quando a aplicacao
     * reinicia - aceitavel em dev.
     */
    @Bean
    SecretKey chaveAccessToken(@Value("${app.auth.jwt-secret:}") String segredo) {
        byte[] bytes;
        if (segredo.isBlank()) {
            log.warn("APP_JWT_SECRET nao configurado: usando segredo aleatorio - "
                    + "tokens da API deixam de valer quando a aplicacao reiniciar");
            bytes = new byte[TAMANHO_MINIMO_BYTES];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < TAMANHO_MINIMO_BYTES) {
                throw new IllegalStateException(
                        "APP_JWT_SECRET precisa de pelo menos " + TAMANHO_MINIMO_BYTES + " bytes (HS256)");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chaveAccessToken) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveAccessToken));
    }

    /** Assinatura, validade (exp/nbf) e emissor - token de outro emissor,
     * mesmo assinado com a mesma chave, e' recusado. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey chaveAccessToken) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveAccessToken)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR));
        return decoder;
    }
}
