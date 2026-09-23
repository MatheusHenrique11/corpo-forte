package com.corpoforte.tracker.feed;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    /**
     * Todos os comentarios dos posts exibidos numa consulta so' (nao um
     * SELECT por post) - mesmo cuidado com N+1 que PostService.listarFeed
     * ja tinha pros autores. Ordem crescente: comentario e' conversa, le-se
     * do mais antigo pro mais novo, ao contrario do feed de posts.
     */
    List<Comentario> findByPostIdInOrderByCriadoEmAsc(Collection<Long> postIds);
}
