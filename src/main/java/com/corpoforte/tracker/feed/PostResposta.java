package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.api.Instantes;

import java.time.Instant;
import java.util.List;

/**
 * Post no feed. comentariosRecentes: os ultimos comentarios (no maximo 3,
 * do mais antigo pro mais novo); os demais vem de
 * GET /api/v1/posts/{id}/comentarios. curtidoPorMim e podeApagar dependem
 * de quem pede.
 */
public record PostResposta(Long id, AutorResposta autor, String texto, Instant criadoEm, long curtidas,
                           boolean curtidoPorMim, boolean podeApagar, long totalComentarios,
                           List<ComentarioResposta> comentariosRecentes) {

    static PostResposta de(PostView post) {
        return new PostResposta(post.id(), new AutorResposta(post.autorId(), post.autorNome()), post.texto(),
                Instantes.emUtc(post.criadoEm()), post.curtidas(), post.curtidoPorMim(), post.podeApagar(),
                post.totalComentarios(), post.comentarios().stream().map(ComentarioResposta::de).toList());
    }
}
