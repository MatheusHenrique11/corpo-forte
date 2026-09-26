package com.corpoforte.tracker.atividade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AtividadeSerieRepository extends JpaRepository<AtividadeSerie, Long> {

    /** Series de todas as atividades de uma pagina numa consulta so'. */
    List<AtividadeSerie> findByAtividadeIdInOrderByAtividadeIdAscOrdemAsc(Collection<Long> atividadeIds);
}
