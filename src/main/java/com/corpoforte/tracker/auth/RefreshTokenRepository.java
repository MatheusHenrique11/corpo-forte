package com.corpoforte.tracker.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUsuarioId(Long usuarioId);

    /**
     * "Revoga se ainda estiver ativo", num UPDATE so': devolve 1 pra quem
     * revogou e 0 pra quem chegou depois. E' isso que impede duas
     * requisicoes de refresh com o mesmo token de receberem, as duas, um
     * par novo - um find seguido de save deixaria as duas passarem.
     *
     * clearAutomatically: o UPDATE em JPQL nao passa pelo contexto de
     * persistencia, e sem limpar, um findByTokenHash seguinte na mesma
     * transacao devolveria a entidade velha da memoria (revogadoEm null).
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken r set r.revogadoEm = :agora where r.id = :id and r.revogadoEm is null")
    int revogarSeAtivo(@Param("id") Long id, @Param("agora") LocalDateTime agora);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken r set r.revogadoEm = :agora where r.usuarioId = :usuarioId and r.revogadoEm is null")
    int revogarTodosDoUsuario(@Param("usuarioId") Long usuarioId, @Param("agora") LocalDateTime agora);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken r where r.tokenHash = :tokenHash")
    int apagarPorHash(@Param("tokenHash") String tokenHash);
}
