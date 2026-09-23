package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.usuario.Usuario;

/** Quem escreveu um post ou comentario, como aparece pra quem le. Nunca
 * carrega dado corporal. username e' null pra conta sem onboarding. */
public record AutorView(Long id, String nome, String username, String fotoUrl) {

    static AutorView de(Usuario usuario) {
        return new AutorView(usuario.getId(), usuario.getNome(), usuario.getUsername(), usuario.getFotoUrl());
    }
}
