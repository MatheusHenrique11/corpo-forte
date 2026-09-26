package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.social.RegraDeBloqueio;

/**
 * "Quem pode ver este post" - a unica definicao da regra no projeto
 * (decisao da Fase 14). Toda consulta que devolve post pra alguem ver
 * inclui POST_VISIVEL: os dois feeds, os posts de um perfil, a pagina do
 * post e a checagem antes de curtir e comentar (buscarVisivel). Nenhuma
 * consulta de post filtra visibilidade por conta propria, e nao existe uma
 * segunda versao da regra em Java - quando a pergunta e' sobre um post so',
 * ela tambem vai ao banco por aqui.
 *
 * O autor sempre ve o proprio post. Pra qualquer outra pessoa: nenhum
 * bloqueio entre ela e o autor (RegraDeBloqueio, nos dois sentidos) e
 * - PUBLICO: qualquer conta;
 * - SEGUIDORES: so' quem segue o autor;
 * - SOMENTE_EU: ninguem.
 *
 * Pedaco de JPQL constante (@Query so' aceita constante): a consulta usa o
 * alias p pro Post e o parametro :quemVe. A matriz de testes
 * (VisibilidadeMatrizIT) cobre cada combinacao pelos pontos de entrada.
 */
public final class RegraDeVisibilidade {

    private static final String VISIBILIDADE = "com.corpoforte.tracker.usuario.Visibilidade.";

    public static final String POST_VISIVEL = "(p.usuarioId = :quemVe or ("
            + RegraDeBloqueio.SEM_BLOQUEIO_COM_AUTOR_DO_POST
            + " and (p.visibilidade = " + VISIBILIDADE + "PUBLICO"
            + " or (p.visibilidade = " + VISIBILIDADE + "SEGUIDORES and exists (select sg.id from Seguimento sg"
            + " where sg.seguidorId = :quemVe and sg.seguidoId = p.usuarioId)))))";

    private RegraDeVisibilidade() {
    }
}
