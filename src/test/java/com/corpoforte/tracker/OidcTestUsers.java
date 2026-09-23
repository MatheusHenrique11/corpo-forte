package com.corpoforte.tracker;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Constroi o mesmo OidcUser tanto pra chamar UsuarioAtualService direto no
 * setup de um teste quanto pra simular a mesma sessao via
 * MockMvcRequestBuilders.oidcLogin().oidcUser(...) - garante que os dois
 * caminhos resolvem pro mesmo Usuario (mesmo "sub"), sem precisar de
 * handshake real com o Google.
 */
public final class OidcTestUsers {

    private OidcTestUsers() {
    }

    /**
     * email_verified=true por padrao (caso comum de uma conta Google real).
     * Use principal(sub, nome, email, emailVerified) pra testar o caminho
     * de e-mail nao verificado explicitamente.
     */
    public static OidcUser principal(String sub, String nome, String email) {
        return principal(sub, nome, email, true);
    }

    public static OidcUser principal(String sub, String nome, String email, boolean emailVerified) {
        OidcIdToken idToken = new OidcIdToken(
                "token-de-teste-" + sub,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of(IdTokenClaimNames.SUB, sub, "name", nome, "email", email,
                        "email_verified", emailVerified));

        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken);
    }
}
