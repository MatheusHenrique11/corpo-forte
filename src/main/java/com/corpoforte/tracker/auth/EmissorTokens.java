package com.corpoforte.tracker.auth;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Emite o par access + refresh pra um usuario ja autenticado (login
 * social verificado, refresh valido ou, em dev, sessao web).
 *
 * O access token carrega so' o id do usuario no "sub" - nada de nome,
 * e-mail ou papel. Qualquer dado a mais ficaria congelado no token por 15
 * minutos depois de mudar no banco; o id nao muda nunca.
 */
@Service
public class EmissorTokens {

    static final Duration VALIDADE_ACCESS_TOKEN = Duration.ofMinutes(15);

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenService refreshTokenService;

    public EmissorTokens(JwtEncoder jwtEncoder, RefreshTokenService refreshTokenService) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    public TokensResposta emitir(Long usuarioId) {
        return new TokensResposta(accessToken(usuarioId), "Bearer",
                VALIDADE_ACCESS_TOKEN.toSeconds(), refreshTokenService.emitir(usuarioId));
    }

    private String accessToken(Long usuarioId) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.EMISSOR)
                .subject(usuarioId.toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(VALIDADE_ACCESS_TOKEN))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
