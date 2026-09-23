package com.corpoforte.tracker.peso;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RegistroPesoRepository extends JpaRepository<RegistroPeso, Long> {

    Optional<RegistroPeso> findByUsuarioIdAndData(Long usuarioId, LocalDate data);

    List<RegistroPeso> findByUsuarioIdOrderByDataDesc(Long usuarioId);

    /** Paginacao por cursor (API): a data e' unica por usuario e sozinha
     * define a posicao; o unique(usuario_id, data) serve de indice. */
    List<RegistroPeso> findByUsuarioIdOrderByDataDesc(Long usuarioId, Limit limite);

    List<RegistroPeso> findByUsuarioIdAndDataLessThanOrderByDataDesc(Long usuarioId, LocalDate data, Limit limite);
}
