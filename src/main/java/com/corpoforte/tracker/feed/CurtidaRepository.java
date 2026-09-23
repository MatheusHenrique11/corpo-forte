package com.corpoforte.tracker.feed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CurtidaRepository extends JpaRepository<Curtida, Long> {

    Optional<Curtida> findByPostIdAndUsuarioId(Long postId, Long usuarioId);

    /** Quais dos posts exibidos o usuario atual ja curtiu (no maximo uma
     * linha por post na tela, entao carregar as entidades aqui e' barato). */
    List<Curtida> findByUsuarioIdAndPostIdIn(Long usuarioId, Collection<Long> postIds);

    /**
     * Primeiro @Query do projeto - todo o resto e' derived query. Motivo:
     * contagem agrupada nao existe em derived query (countByPostId conta um
     * post por chamada, ou seja, N+1), e a alternativa sem @Query seria
     * carregar TODAS as linhas de curtida dos posts exibidos so' pra contar
     * em memoria. Diferente do catalogo de exercicios (54 linhas fixas,
     * Fase 4/5), curtida cresce sem teto por post, entao contar no banco
     * e' a escolha certa e nao otimizacao prematura.
     */
    @Query("select c.postId as postId, count(c) as total from Curtida c where c.postId in :postIds group by c.postId")
    List<ContagemPorPost> contarPorPost(@Param("postIds") Collection<Long> postIds);
}
