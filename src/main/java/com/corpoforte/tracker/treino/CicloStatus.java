package com.corpoforte.tracker.treino;

/**
 * semanaAtual e' baseada em ESFORCO (semanas em que houve treino
 * concluido); precisaReavaliar e' baseado em CALENDARIO (8 semanas desde a
 * avaliacao). Os dois podem divergir de proposito - ver PeriodizacaoService.
 */
public record CicloStatus(int semanaAtual, int totalSemanas, boolean precisaReavaliar) {
}
