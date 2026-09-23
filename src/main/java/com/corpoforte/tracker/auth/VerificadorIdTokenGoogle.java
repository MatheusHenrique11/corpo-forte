package com.corpoforte.tracker.auth;

import com.corpoforte.tracker.usuario.DadosLogin;
import com.corpoforte.tracker.usuario.Provedor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * Valida o ID token que o cliente recebeu do Google Sign-In e o traduz em
 * DadosLogin - a mesma coisa que o oauth2Login faz na sessao web, so' que
 * com o token chegando pela API em vez do redirect.
 *
 * O que e' checado, e por que cada um importa:
 * - assinatura, pelas chaves publicas do Google (JWKS, buscadas e
 *   cacheadas pelo Nimbus na primeira validacao, nao no boot);
 * - exp/nbf (JwtTimestampValidator);
 * - emissor: o Google usa as duas formas, com e sem "https://";
 * - audience: o token tem que ter sido emitido PARA um cliente nosso. Sem
 *   isso, um ID token que qualquer outro site/app obteve do mesmo usuario
 *   no Google serviria pra entrar na conta dele aqui.
 *
 * Lista de client IDs vazia recusa tudo: sem configuracao, o login pela
 * API fica desligado, nunca aberto.
 */
@Component
public class VerificadorIdTokenGoogle {

    private static final Set<String> EMISSORES = Set.of("https://accounts.google.com", "accounts.google.com");

    private final NimbusJwtDecoder decoder;

    @Autowired
    public VerificadorIdTokenGoogle(@Value("${app.auth.google.jwk-set-uri}") String jwkSetUri,
                                    @Value("${app.auth.google.client-ids:}") List<String> clientIds) {
        this(NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build(), clientIds);
    }

    /** Pra teste unitario: decoder com chave publica de teste no lugar do
     * JWKS real, e os mesmos validadores de producao. */
    VerificadorIdTokenGoogle(NimbusJwtDecoder decoder, List<String> clientIds) {
        List<String> clientIdsValidos = clientIds.stream().filter(id -> !id.isBlank()).toList();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), emissorDoGoogle(), audienceEntre(clientIdsValidos)));
        this.decoder = decoder;
    }

    public DadosLogin verificar(String idToken) {
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do Google inválido");
        }

        // email_verified chega como boolean, mas tokens antigos do Google ja
        // mandaram como string "true"
        Object emailVerified = jwt.getClaims().get("email_verified");
        boolean verificado = Boolean.TRUE.equals(emailVerified) || "true".equals(emailVerified);

        return new DadosLogin(Provedor.GOOGLE, jwt.getSubject(), jwt.getClaimAsString("email"),
                verificado, jwt.getClaimAsString("name"));
    }

    /**
     * Le o "iss" como texto, nao com jwt.getIssuer(): getIssuer() converte
     * pra URL, e "accounts.google.com" (sem esquema) nao e' URL - lancaria
     * IllegalArgumentException, que nao e' JwtException, e um login
     * legitimo viraria 500.
     */
    private static OAuth2TokenValidator<Jwt> emissorDoGoogle() {
        return jwt -> EMISSORES.contains(jwt.getClaimAsString(JwtClaimNames.ISS))
                ? OAuth2TokenValidatorResult.success()
                : falha("emissor nao e' o Google");
    }

    private static OAuth2TokenValidator<Jwt> audienceEntre(List<String> clientIds) {
        return jwt -> jwt.getAudience() != null && jwt.getAudience().stream().anyMatch(clientIds::contains)
                ? OAuth2TokenValidatorResult.success()
                : falha("token nao foi emitido para este aplicativo");
    }

    private static OAuth2TokenValidatorResult falha(String descricao) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", descricao, null));
    }
}
