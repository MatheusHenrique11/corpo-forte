package com.corpoforte.tracker.atividade;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    /** A atividade so' existe pra dona: de outra conta e inexistente sao o
     * mesmo vazio (404). */
    Optional<Atividade> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByTreinoDoDiaId(Long treinoDoDiaId);

    /** Diario, dia mais recente primeiro (keyset data, id; indice da V19). */
    @Query("select a from Atividade a where a.usuarioId = :usuarioId order by a.data desc, a.id desc")
    List<Atividade> buscarDoUsuario(@Param("usuarioId") Long usuarioId, Limit limite);

    @Query("select a from Atividade a where a.usuarioId = :usuarioId "
            + "and (a.data < :data or (a.data = :data and a.id < :id)) order by a.data desc, a.id desc")
    List<Atividade> buscarDoUsuarioApos(@Param("usuarioId") Long usuarioId, @Param("data") LocalDate data,
                                        @Param("id") Long id, Limit limite);
}
