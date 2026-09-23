package com.corpoforte.tracker.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Troca so' a fonte da chave publica do verificador (JWKS real do Google ->
 * chave de GoogleIdTokenDeTeste). Importado pelo IntegrationTestBase pra
 * todos os testes de integracao dividirem o mesmo contexto Spring.
 */
@TestConfiguration
public class GoogleDeTesteConfig {

    @Bean
    @Primary
    VerificadorIdTokenGoogle verificadorIdTokenGoogleDeTeste(
            @Value("${app.auth.google.client-ids:}") List<String> clientIds) {
        return new VerificadorIdTokenGoogle(GoogleIdTokenDeTeste.decoderComChaveDoGoogle(), clientIds);
    }
}
