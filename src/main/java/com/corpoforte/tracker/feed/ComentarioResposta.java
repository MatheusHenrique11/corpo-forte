package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.api.Instantes;

import java.time.Instant;

/** podeApagar: quem escreveu ou o autor do post (mesma regra de
 * PostService.apagarComentario). */
public record ComentarioResposta(Long id, AutorResposta autor, String texto, Instant criadoEm, boolean podeApagar) {

    static ComentarioResposta de(ComentarioView comentario) {
        return new ComentarioResposta(comentario.id(), AutorResposta.de(comentario.autor()),
                comentario.texto(), Instantes.emUtc(comentario.criadoEm()), comentario.podeApagar());
    }
}
