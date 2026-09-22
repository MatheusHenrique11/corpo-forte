package com.corpoforte.tracker.exercicio;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Leitura do catalogo (dado de referencia, populado via migration - ver
 * Exercicio). Mesmo papel que UsuarioAtualService/AvaliacaoFisicaService/
 * RegistroPesoService tem pras suas entidades: o Controller nao toca o
 * repository direto.
 */
@Service
public class ExercicioService {

    private final ExercicioRepository exercicioRepository;

    public ExercicioService(ExercicioRepository exercicioRepository) {
        this.exercicioRepository = exercicioRepository;
    }

    public List<Exercicio> listarTodos() {
        return exercicioRepository.findAll();
    }
}
