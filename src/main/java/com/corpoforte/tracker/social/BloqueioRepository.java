package com.corpoforte.tracker.social;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BloqueioRepository extends JpaRepository<Bloqueio, Long> {

    Optional<Bloqueio> findByBloqueadorIdAndBloqueadoId(Long bloqueadorId, Long bloqueadoId);

    /** Os dois sentidos - a mesma pergunta que RegraDeBloqueio faz dentro
     * das consultas, pra quando a checagem e' de uma conta so'. */
    @Query("select count(bl) > 0 from Bloqueio bl where (bl.bloqueadorId = :a and bl.bloqueadoId = :b) "
            + "or (bl.bloqueadorId = :b and bl.bloqueadoId = :a)")
    boolean existeEntre(@Param("a") Long a, @Param("b") Long b);

    @Query("select bl from Bloqueio bl where bl.bloqueadorId = :bloqueadorId order by bl.criadoEm desc, bl.id desc")
    List<Bloqueio> buscarDoBloqueador(@Param("bloqueadorId") Long bloqueadorId, Limit limite);

    @Query("select bl from Bloqueio bl where bl.bloqueadorId = :bloqueadorId "
            + "and (bl.criadoEm < :criadoEm or (bl.criadoEm = :criadoEm and bl.id < :id)) "
            + "order by bl.criadoEm desc, bl.id desc")
    List<Bloqueio> buscarDoBloqueadorApos(@Param("bloqueadorId") Long bloqueadorId,
                                          @Param("criadoEm") LocalDateTime criadoEm, @Param("id") Long id,
                                          Limit limite);
}
