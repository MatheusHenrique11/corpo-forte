package com.corpoforte.tracker.feed;

/**
 * Quem escreveu um post ou comentario. Objeto (e nao so' o nome solto no
 * post) pra poder ganhar campos novos sem mudar o formato do post. Nunca
 * carrega dado corporal.
 */
public record AutorResposta(Long id, String nome, String username, String fotoUrl) {

    static AutorResposta de(AutorView autor) {
        return new AutorResposta(autor.id(), autor.nome(), autor.username(), autor.fotoUrl());
    }
}
