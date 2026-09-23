package com.corpoforte.tracker.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IdentidadeExternaRepository extends JpaRepository<IdentidadeExterna, Long> {

    Optional<IdentidadeExterna> findByProvedorAndSub(Provedor provedor, String sub);

    List<IdentidadeExterna> findByUsuarioId(Long usuarioId);
}
