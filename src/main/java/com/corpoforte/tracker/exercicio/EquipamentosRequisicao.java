package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.usuario.Equipamento;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/** Conjunto completo do que o usuario tem: substitui o anterior (lista
 * vazia = nenhum equipamento). */
public record EquipamentosRequisicao(
        @NotNull(message = "Informe a lista de equipamentos (pode ser vazia)")
        Set<@NotNull(message = "Equipamento não pode ser nulo") Equipamento> equipamentos) {
}
