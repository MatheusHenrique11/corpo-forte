package com.corpoforte.tracker.avaliacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AvaliacaoFisicaRepository extends JpaRepository<AvaliacaoFisica, Long> {

    Optional<AvaliacaoFisica> findByUsuarioId(Long usuarioId);
}
