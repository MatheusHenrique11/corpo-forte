package com.corpoforte.tracker.feed;

/**
 * Linha de uma contagem agrupada por post (curtidas, comentarios). Era
 * interna ao CurtidaRepository (Fase 7b); virou top-level quando a
 * contagem de comentarios passou a usar a mesma forma.
 */
public interface ContagemPorPost {

    Long getPostId();

    long getTotal();
}
