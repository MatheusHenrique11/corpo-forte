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
}
