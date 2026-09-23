package com.corpoforte.tracker.avaliacao;

import java.time.LocalDate;
import java.util.List;

/** Resultado de AvaliacaoFisicaService.compararUltimas: as duas datas
 * comparadas e a diferenca por movimento. */
public record ComparacaoAvaliacoes(LocalDate dataAtual, LocalDate dataAnterior, List<ComparacaoItem> itens) {
}
