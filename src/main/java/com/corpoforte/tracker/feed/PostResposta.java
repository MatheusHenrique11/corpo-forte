package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.api.Instantes;
import com.corpoforte.tracker.usuario.Visibilidade;

import java.time.Instant;
import java.util.List;

/**
 * Post no feed. comentariosRecentes: os ultimos comentarios (no maximo 3,
 * do mais antigo pro mais novo); os demais vem de
 * GET /api/v1/posts/{id}/comentarios. curtidoPorMim e podeApagar dependem
 * de quem pede. fotos: ate 4, na ordem em que foram enviadas (FotoResposta).
 */
public record PostResposta(Long id, AutorResposta autor, String texto, Instant criadoEm, Visibilidade visibilidade,
                           long curtidas, boolean curtidoPorMim, boolean podeApagar, long totalComentarios,
                           List<ComentarioResposta> comentariosRecentes, List<FotoResposta> fotos) {

    public static PostResposta de(PostView post) {
        return new PostResposta(post.id(), AutorResposta.de(post.autor()), post.texto(),
                Instantes.emUtc(post.criadoEm()), post.visibilidade(), post.curtidas(), post.curtidoPorMim(), post.podeApagar(),
                post.totalComentarios(), post.comentarios().stream().map(ComentarioResposta::de).toList(),
                post.fotos().stream().map(FotoResposta::de).toList());
    }
}
