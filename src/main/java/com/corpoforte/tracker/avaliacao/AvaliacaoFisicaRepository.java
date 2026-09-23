package com.corpoforte.tracker.avaliacao;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvaliacaoFisicaRepository extends JpaRepository<AvaliacaoFisica, Long> {

    /**
     * Nao existe mais findByUsuarioId devolvendo Optional: com historico
     * (Fase 9) isso estouraria em runtime
     * (IncorrectResultSizeDataAccessException) assim que o usuario tivesse
     * a segunda avaliacao - erro que nao apareceria em compilacao.
     */
    Optional<AvaliacaoFisica> findByUsuarioIdAndDataAvaliacao(Long usuarioId, LocalDate dataAvaliacao);

    List<AvaliacaoFisica> findByUsuarioIdOrderByDataAvaliacaoDesc(Long usuarioId);

    /** Paginacao por cursor (API): data_avaliacao e' unica por usuario, entao
     * sozinha ja define a posicao - o unique(usuario_id, data_avaliacao) e' o
     * indice que atende as duas consultas. */
    List<AvaliacaoFisica> findByUsuarioIdOrderByDataAvaliacaoDesc(Long usuarioId, Limit limite);

    List<AvaliacaoFisica> findByUsuarioIdAndDataAvaliacaoLessThanOrderByDataAvaliacaoDesc(
            Long usuarioId, LocalDate dataAvaliacao, Limit limite);
}
