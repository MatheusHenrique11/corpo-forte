package com.corpoforte.tracker.feed;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Toda busca de post que devolve conteudo pra alguem ver passa por
 * RegraDeVisibilidade.POST_VISIVEL e recebe :quemVe. O findById herdado so'
 * e' usado onde a regra e' outra (apagar: so' o autor).
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    String VISIVEL = RegraDeVisibilidade.POST_VISIVEL;

    /** Um post, se quem ve pode ve-lo: pagina do post, curtir, comentar e
     * ler comentarios. Invisivel e inexistente sao o mesmo vazio (404). */
    @Query("select p from Post p where p.id = :postId and " + VISIVEL)
    Optional<Post> buscarVisivel(@Param("postId") Long postId, @Param("quemVe") Long quemVe);

    /** Feed da tela web (sem paginacao, como sempre foi). */
    @Query("select p from Post p where " + VISIVEL + " order by p.criadoEm desc, p.id desc")
    List<Post> buscarTodosVisiveis(@Param("quemVe") Long quemVe);

    /**
     * Feed "Descobrir" paginado por cursor (keyset): primeira pagina e "o
     * que vem depois de (criadoEm, id)". O id desempata posts criados no
     * mesmo instante - sem ele, um empate na fronteira da pagina pularia ou
     * repetiria post. Indice (criado_em desc, id desc) da V14.
     */
    @Query("select p from Post p where " + VISIVEL + " order by p.criadoEm desc, p.id desc")
    List<Post> buscarMaisRecentes(@Param("quemVe") Long quemVe, Limit limite);

    @Query("select p from Post p where " + VISIVEL
            + " and (p.criadoEm < :criadoEm or (p.criadoEm = :criadoEm and p.id < :id))"
            + " order by p.criadoEm desc, p.id desc")
    List<Post> buscarAnterioresA(@Param("quemVe") Long quemVe, @Param("criadoEm") LocalDateTime criadoEm,
                                 @Param("id") Long id, Limit limite);

    /** Posts de um autor (perfil publico), mesmo keyset. Indice
     * (usuario_id, criado_em desc, id desc) da V15. */
    @Query("select p from Post p where p.usuarioId = :autorId and " + VISIVEL
            + " order by p.criadoEm desc, p.id desc")
    List<Post> buscarMaisRecentesDoAutor(@Param("autorId") Long autorId, @Param("quemVe") Long quemVe,
                                         Limit limite);

    @Query("select p from Post p where p.usuarioId = :autorId and " + VISIVEL
            + " and (p.criadoEm < :criadoEm or (p.criadoEm = :criadoEm and p.id < :id))"
            + " order by p.criadoEm desc, p.id desc")
    List<Post> buscarDoAutorAnterioresA(@Param("autorId") Long autorId, @Param("quemVe") Long quemVe,
                                        @Param("criadoEm") LocalDateTime criadoEm, @Param("id") Long id,
                                        Limit limite);

    /** Contagem do perfil: so' o que quem ve consegue ver - contar tudo
     * revelaria que existem posts escondidos. */
    @Query("select count(p) from Post p where p.usuarioId = :autorId and " + VISIVEL)
    long contarVisiveisDoAutor(@Param("autorId") Long autorId, @Param("quemVe") Long quemVe);

    /**
     * Feed "Seguindo": os meus posts e os de quem eu sigo, no mesmo keyset
     * do feed global. Subquery em vez de buscar os ids seguidos antes: quem
     * segue milhares de contas geraria um "in (...)" com milhares de
     * parametros. Seguir nao libera o SOMENTE_EU: a regra de visibilidade
     * vale aqui tambem.
     */
    @Query("select p from Post p where (p.usuarioId = :quemVe or p.usuarioId in "
            + "(select s.seguidoId from Seguimento s where s.seguidorId = :quemVe)) and " + VISIVEL
            + " order by p.criadoEm desc, p.id desc")
    List<Post> buscarSeguindo(@Param("quemVe") Long quemVe, Limit limite);

    @Query("select p from Post p where (p.usuarioId = :quemVe or p.usuarioId in "
            + "(select s.seguidoId from Seguimento s where s.seguidorId = :quemVe)) and " + VISIVEL
            + " and (p.criadoEm < :criadoEm or (p.criadoEm = :criadoEm and p.id < :id))"
            + " order by p.criadoEm desc, p.id desc")
    List<Post> buscarSeguindoAnterioresA(@Param("quemVe") Long quemVe, @Param("criadoEm") LocalDateTime criadoEm,
                                         @Param("id") Long id, Limit limite);
}
