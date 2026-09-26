package com.corpoforte.tracker.feed;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FotoPostRepository extends JpaRepository<FotoPost, Long> {

    /** Fotos de todos os posts de uma pagina numa consulta so'. */
    List<FotoPost> findByPostIdInOrderByPostIdAscPosicaoAsc(Collection<Long> postIds);

    List<FotoPost> findByPostId(Long postId);
}
