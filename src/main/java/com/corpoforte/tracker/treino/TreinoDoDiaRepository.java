package com.corpoforte.tracker.treino;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TreinoDoDiaRepository extends JpaRepository<TreinoDoDia, Long> {

    Optional<TreinoDoDia> findByUsuarioIdAndData(Long usuarioId, LocalDate data);
}
