package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.social.RegraDeBloqueio;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Comentario de conta com bloqueio com quem ve some pra quem ve (bloqueio
 * mutuo em efeito, Fase 14) - toda leitura recebe :quemVe e aplica
 * RegraDeBloqueio. A visibilidade do POST quem garante e' quem chama: os
 * comentarios so' sao buscados pra posts que ja passaram pela regra.
 */
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    String SEM_BLOQUEIO = RegraDeBloqueio.SEM_BLOQUEIO_COM_AUTOR_DO_COMENTARIO;

    /**
     * Todos os comentarios dos posts exibidos na tela web numa consulta so'
     * (nao um SELECT por post). Ordem crescente: comentario e' conversa,
     * le-se do mais antigo pro mais novo, ao contrario do feed de posts.
     */
    @Query("select c from Comentario c where c.postId in :postIds and " + SEM_BLOQUEIO
            + " order by c.criadoEm asc, c.id asc")
    List<Comentario> buscarDosPosts(@Param("postIds") Collection<Long> postIds, @Param("quemVe") Long quemVe);

    /**
     * Comentarios de um post paginados por cursor, do mais antigo pro mais
     * novo (conversa). Mesmo keyset (criadoEm, id) do feed, no sentido
     * crescente; indice (post_id, criado_em, id) da V14.
     */
    @Query("select c from Comentario c where c.postId = :postId and " + SEM_BLOQUEIO
            + " order by c.criadoEm asc, c.id asc")
    List<Comentario> buscarDoPost(@Param("postId") Long postId, @Param("quemVe") Long quemVe, Limit limite);

    @Query("select c from Comentario c where c.postId = :postId and " + SEM_BLOQUEIO
            + " and (c.criadoEm > :criadoEm or (c.criadoEm = :criadoEm and c.id > :id))"
            + " order by c.criadoEm asc, c.id asc")
    List<Comentario> buscarDoPostApos(@Param("postId") Long postId, @Param("quemVe") Long quemVe,
                                      @Param("criadoEm") LocalDateTime criadoEm, @Param("id") Long id,
                                      Limit limite);

    /**
     * Os N comentarios mais recentes DE CADA post da pagina, numa consulta
     * so' - "N por grupo" nao cabe em JPQL, dai o SQL nativo com
     * row_number() (e a versao SQL da regra de bloqueio). Devolve em ordem
     * de conversa (mais antigo primeiro) dentro de cada post.
     */
    @Query(nativeQuery = true, value = "select id, post_id, usuario_id, texto, criado_em"
            + " from (select c.*, row_number() over (partition by c.post_id"
            + "                                      order by c.criado_em desc, c.id desc) as posicao"
            + "       from comentario c"
            + "       where c.post_id in (:postIds) and " + RegraDeBloqueio.SQL_SEM_BLOQUEIO_COM_AUTOR_DO_COMENTARIO
            + " ) recentes"
            + " where posicao <= :quantidade"
            + " order by post_id, criado_em, id")
    List<Comentario> buscarRecentesPorPost(@Param("postIds") Collection<Long> postIds,
                                           @Param("quantidade") int quantidade, @Param("quemVe") Long quemVe);

    /** Total de comentarios por post contando so' os que quem ve enxerga -
     * o numero bate com a lista. */
    @Query("select c.postId as postId, count(c) as total from Comentario c "
            + "where c.postId in :postIds and " + SEM_BLOQUEIO + " group by c.postId")
    List<ContagemPorPost> contarPorPost(@Param("postIds") Collection<Long> postIds, @Param("quemVe") Long quemVe);
}
