package com.corpoforte.tracker.feed;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /**
     * Comentarios de um post paginados por cursor, do mais antigo pro mais
     * novo (conversa). Mesmo keyset (criadoEm, id) do feed, no sentido
     * crescente; indice (post_id, criado_em, id) da V14.
     */
    @Query("select c from Comentario c where c.postId = :postId order by c.criadoEm asc, c.id asc")
    List<Comentario> buscarDoPost(@Param("postId") Long postId, Limit limite);

    @Query("select c from Comentario c where c.postId = :postId "
            + "and (c.criadoEm > :criadoEm or (c.criadoEm = :criadoEm and c.id > :id)) "
            + "order by c.criadoEm asc, c.id asc")
    List<Comentario> buscarDoPostApos(@Param("postId") Long postId, @Param("criadoEm") LocalDateTime criadoEm,
                                      @Param("id") Long id, Limit limite);

    /**
     * Os N comentarios mais recentes DE CADA post da pagina, numa consulta
     * so' - "N por grupo" nao cabe em JPQL, dai o SQL nativo com
     * row_number(). Devolve em ordem de conversa (mais antigo primeiro)
     * dentro de cada post. Sem isso, a alternativa seria uma consulta por
     * post (N+1) ou carregar todos os comentarios so' pra descartar a
     * maioria.
     */
    @Query(nativeQuery = true, value = """
            select id, post_id, usuario_id, texto, criado_em
            from (select c.*, row_number() over (partition by c.post_id
                                                 order by c.criado_em desc, c.id desc) as posicao
                  from comentario c
                  where c.post_id in (:postIds)) recentes
            where posicao <= :quantidade
            order by post_id, criado_em, id
            """)
    List<Comentario> buscarRecentesPorPost(@Param("postIds") Collection<Long> postIds,
                                           @Param("quantidade") int quantidade);

    @Query("select c.postId as postId, count(c) as total from Comentario c "
            + "where c.postId in :postIds group by c.postId")
    List<ContagemPorPost> contarPorPost(@Param("postIds") Collection<Long> postIds);
}
