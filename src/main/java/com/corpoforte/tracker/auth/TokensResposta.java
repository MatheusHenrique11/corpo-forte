package com.corpoforte.tracker.auth;

/**
 * Par devolvido no login e em cada refresh. tipo e expiraEmSegundos seguem
 * os nomes de papel do OAuth2 (token_type/expires_in) pro cliente nao
 * precisar decodificar o JWT pra saber quando renovar.
 */
public record TokensResposta(String accessToken, String tipo, long expiraEmSegundos, String refreshToken) {
}
