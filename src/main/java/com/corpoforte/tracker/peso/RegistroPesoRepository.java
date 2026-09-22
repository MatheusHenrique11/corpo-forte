package com.corpoforte.tracker.peso;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RegistroPesoRepository extends JpaRepository<RegistroPeso, Long> {

    Optional<RegistroPeso> findByUsuarioIdAndData(Long usuarioId, LocalDate data);

    List<RegistroPeso> findByUsuarioIdOrderByDataDesc(Long usuarioId);
}
