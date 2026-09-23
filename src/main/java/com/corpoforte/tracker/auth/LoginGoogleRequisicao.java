package com.corpoforte.tracker.auth;

import jakarta.validation.constraints.NotBlank;

/** idToken: o "credential" que o Google Sign-In entrega ao cliente. */
public record LoginGoogleRequisicao(@NotBlank(message = "Informe o idToken do Google") String idToken) {
}
