package com.corpoforte.tracker.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de /auth/refresh e /auth/logout. O refresh token vai no corpo, nao
 * no cabecalho Authorization: ali mora o access token, e misturar os dois
 * faria o filtro de bearer tentar validar o refresh como se fosse JWT.
 */
public record RefreshTokenRequisicao(@NotBlank(message = "Informe o refreshToken") String refreshToken) {
}
