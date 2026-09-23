package com.corpoforte.tracker.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByGoogleSub(String googleSub);

    List<Usuario> findByGoogleSubIsNull();
}
