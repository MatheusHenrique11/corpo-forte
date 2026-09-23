package com.corpoforte.tracker.perfilpublico;

import com.corpoforte.tracker.usuario.UsernameValido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Substitui os dois: bio ausente ou null apaga a bio. */
public record PerfilPublicoRequisicao(
        @NotBlank(message = "Escolha um nome de usuário") @UsernameValido String username,
        @Size(max = 160, message = "Máximo de 160 caracteres") String bio) {
}
