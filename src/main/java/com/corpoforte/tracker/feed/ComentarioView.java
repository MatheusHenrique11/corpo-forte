package com.corpoforte.tracker.feed;

import java.time.LocalDateTime;

/**
 * podeApagar depende de quem esta olhando (autor do comentario ou autor do
 * post), igual PostView.curtidoPorMim - calculado pela mesma regra que
 * PostService.apagarComentario aplica, pra tela nunca oferecer um botao
 * que o servidor recusaria.
 */
public record ComentarioView(Long id, String autorNome, String texto, LocalDateTime criadoEm,
                             boolean podeApagar) {
}
