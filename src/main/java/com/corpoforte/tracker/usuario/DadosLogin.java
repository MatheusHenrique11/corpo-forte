package com.corpoforte.tracker.usuario;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * O que um login social ja verificado informa sobre a pessoa, independente
 * de por onde ele chegou: sessao web (OidcUser, Fase 6) ou ID token
 * recebido pela API (Fase 10). Os dois viram este record antes de chegar
 * no UsuarioAtualService, e por isso resolvem pro mesmo Usuario pelo mesmo
 * caminho - inclusive a regra de reivindicacao da conta orfa.
 *
 * Quem constroi este record e' quem ja validou a assinatura do token; o
 * service confia no que recebe.
 */
public record DadosLogin(Provedor provedor, String sub, String email, boolean emailVerificado, String nome,
                         String fotoUrl) {

    public static DadosLogin deGoogle(OidcUser principal) {
        return new DadosLogin(Provedor.GOOGLE, principal.getSubject(), principal.getEmail(),
                Boolean.TRUE.equals(principal.getEmailVerified()), principal.getFullName(), principal.getPicture());
    }
}
