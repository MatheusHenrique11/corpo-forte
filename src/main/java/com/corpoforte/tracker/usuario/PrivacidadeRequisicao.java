package com.corpoforte.tracker.usuario;

import jakarta.validation.constraints.NotNull;

public record PrivacidadeRequisicao(
        @NotNull(message = "Escolha a visibilidade padrão dos posts") Visibilidade visibilidadePadrao) {
}
