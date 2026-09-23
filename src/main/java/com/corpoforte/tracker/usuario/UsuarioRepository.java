package com.corpoforte.tracker.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Contas que nenhum login alcanca - na pratica, a conta local de antes
     * do login existir (Fase 6), esperando o dono reivindicar. Era
     * findByGoogleSubIsNull ate a identidade sair da tabela usuario (V12).
     */
    @Query("select u from Usuario u where not exists "
            + "(select 1 from IdentidadeExterna i where i.usuarioId = u.id)")
    List<Usuario> findSemIdentidadeExterna();
}
