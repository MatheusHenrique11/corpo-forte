package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.exercicio.Exercicio;
import com.corpoforte.tracker.exercicio.MovimentoPadrao;

public record ItemGerado(MovimentoPadrao movimento, Exercicio exercicio, int series, int repeticoes) {
}
