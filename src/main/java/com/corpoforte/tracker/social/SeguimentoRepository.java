package com.corpoforte.tracker.social;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeguimentoRepository extends JpaRepository<Seguimento, Long> {

    Optional<Seguimento> findBySeguidorIdAndSeguidoId(Long seguidorId, Long seguidoId);

    long countBySeguidoId(Long seguidoId);

    long countBySeguidorId(Long seguidorId);

    /** Quais das contas listadas o usuario ja segue - uma consulta pra
     * lista inteira, nao uma por conta. */
    @Query("select s.seguidoId from Seguimento s where s.seguidorId = :seguidorId and s.seguidoId in :ids")
    List<Long> quaisSegue(@Param("seguidorId") Long seguidorId, @Param("ids") Collection<Long> ids);

    /** Seguidores de uma conta, quem seguiu por ultimo primeiro (keyset
     * (criadoEm, id), indice idx_seguimento_seguido da V16). */
    @Query("select s from Seguimento s where s.seguidoId = :seguidoId order by s.criadoEm desc, s.id desc")
    List<Seguimento> buscarSeguidores(@Param("seguidoId") Long seguidoId, Limit limite);

    @Query("select s from Seguimento s where s.seguidoId = :seguidoId "
            + "and (s.criadoEm < :criadoEm or (s.criadoEm = :criadoEm and s.id < :id)) "
            + "order by s.criadoEm desc, s.id desc")
    List<Seguimento> buscarSeguidoresApos(@Param("seguidoId") Long seguidoId,
                                          @Param("criadoEm") LocalDateTime criadoEm, @Param("id") Long id,
                                          Limit limite);

    /** Quem uma conta segue, mesma ordem (indice idx_seguimento_seguidor). */
    @Query("select s from Seguimento s where s.seguidorId = :seguidorId order by s.criadoEm desc, s.id desc")
    List<Seguimento> buscarSeguindo(@Param("seguidorId") Long seguidorId, Limit limite);

    @Query("select s from Seguimento s where s.seguidorId = :seguidorId "
            + "and (s.criadoEm < :criadoEm or (s.criadoEm = :criadoEm and s.id < :id)) "
            + "order by s.criadoEm desc, s.id desc")
    List<Seguimento> buscarSeguindoApos(@Param("seguidorId") Long seguidorId,
                                        @Param("criadoEm") LocalDateTime criadoEm, @Param("id") Long id,
                                        Limit limite);
}
