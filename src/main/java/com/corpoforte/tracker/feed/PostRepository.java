package com.corpoforte.tracker.feed;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByOrderByCriadoEmDesc();

    /**
     * Feed paginado por cursor (keyset): primeira pagina e "o que vem
     * depois de (criadoEm, id)". O id desempata posts criados no mesmo
     * instante - sem ele, um empate na fronteira da pagina pularia ou
     * repetiria post. Atendidas pelo indice (criado_em desc, id desc) da V14.
     */
    @Query("select p from Post p order by p.criadoEm desc, p.id desc")
    List<Post> buscarMaisRecentes(Limit limite);

    @Query("select p from Post p where p.criadoEm < :criadoEm or (p.criadoEm = :criadoEm and p.id < :id) "
            + "order by p.criadoEm desc, p.id desc")
    List<Post> buscarAnterioresA(@Param("criadoEm") LocalDateTime criadoEm, @Param("id") Long id, Limit limite);

    /** Mesmo keyset, so' os posts de um autor (perfil publico). Indice
     * (usuario_id, criado_em desc, id desc) da V15. */
    @Query("select p from Post p where p.usuarioId = :autorId order by p.criadoEm desc, p.id desc")
    List<Post> buscarMaisRecentesDoAutor(@Param("autorId") Long autorId, Limit limite);

    @Query("select p from Post p where p.usuarioId = :autorId "
            + "and (p.criadoEm < :criadoEm or (p.criadoEm = :criadoEm and p.id < :id)) "
            + "order by p.criadoEm desc, p.id desc")
    List<Post> buscarDoAutorAnterioresA(@Param("autorId") Long autorId, @Param("criadoEm") LocalDateTime criadoEm,
                                        @Param("id") Long id, Limit limite);

    long countByUsuarioId(Long usuarioId);

    /**
     * Feed "Seguindo": os meus posts e os de quem eu sigo, no mesmo keyset
     * do feed global. Subquery em vez de buscar os ids seguidos antes: quem
     * segue milhares de contas geraria um "in (...)" com milhares de
     * parametros. E' a unica consulta do feed que conhece o Seguimento (do
     * pacote social); o social nao conhece post.
     */
    @Query("select p from Post p where (p.usuarioId = :eu or p.usuarioId in "
            + "(select s.seguidoId from Seguimento s where s.seguidorId = :eu)) "
            + "order by p.criadoEm desc, p.id desc")
    List<Post> buscarSeguindo(@Param("eu") Long usuarioId, Limit limite);

    @Query("select p from Post p where (p.usuarioId = :eu or p.usuarioId in "
            + "(select s.seguidoId from Seguimento s where s.seguidorId = :eu)) "
            + "and (p.criadoEm < :criadoEm or (p.criadoEm = :criadoEm and p.id < :id)) "
            + "order by p.criadoEm desc, p.id desc")
    List<Post> buscarSeguindoAnterioresA(@Param("eu") Long usuarioId, @Param("criadoEm") LocalDateTime criadoEm,
                                         @Param("id") Long id, Limit limite);
}
