package com.corpoforte.tracker.social;

/**
 * "Nao existe bloqueio, em nenhum dos dois sentidos, entre quem ve e esta
 * conta" - a unica definicao dessa regra no projeto. Toda consulta que
 * lista post, comentario ou conta pra alguem ver inclui um destes
 * predicados; nenhuma escreve o seu proprio.
 *
 * Sao pedacos de JPQL (constantes, porque @Query so' aceita constante),
 * todos montados das mesmas tres partes: so' muda qual coluna e' "a outra
 * conta". A consulta que usa precisa do parametro :quemVe e do alias
 * indicado no nome (p = Post, c = Comentario, u = Usuario, s = Seguimento).
 *
 * Mutuo em efeito: quem bloqueou tambem deixa de ver quem foi bloqueado.
 */
public final class RegraDeBloqueio {

    private static final String INICIO =
            "not exists (select bl.id from Bloqueio bl where (bl.bloqueadorId = :quemVe and bl.bloqueadoId = ";
    private static final String MEIO = ") or (bl.bloqueadoId = :quemVe and bl.bloqueadorId = ";
    private static final String FIM = "))";

    public static final String SEM_BLOQUEIO_COM_AUTOR_DO_POST = INICIO + "p.usuarioId" + MEIO + "p.usuarioId" + FIM;

    public static final String SEM_BLOQUEIO_COM_AUTOR_DO_COMENTARIO =
            INICIO + "c.usuarioId" + MEIO + "c.usuarioId" + FIM;

    public static final String SEM_BLOQUEIO_COM_A_CONTA = INICIO + "u.id" + MEIO + "u.id" + FIM;

    public static final String SEM_BLOQUEIO_COM_O_SEGUIDOR = INICIO + "s.seguidorId" + MEIO + "s.seguidorId" + FIM;

    public static final String SEM_BLOQUEIO_COM_O_SEGUIDO = INICIO + "s.seguidoId" + MEIO + "s.seguidoId" + FIM;

    /**
     * A mesma regra em SQL, pra unica consulta que nao cabe em JPQL (os N
     * comentarios mais recentes por post, com window function). Alias c =
     * tabela comentario.
     */
    public static final String SQL_SEM_BLOQUEIO_COM_AUTOR_DO_COMENTARIO =
            "not exists (select 1 from bloqueio bl where (bl.bloqueador_id = :quemVe and bl.bloqueado_id = c.usuario_id)"
                    + " or (bl.bloqueado_id = :quemVe and bl.bloqueador_id = c.usuario_id))";

    private RegraDeBloqueio() {
    }
}
