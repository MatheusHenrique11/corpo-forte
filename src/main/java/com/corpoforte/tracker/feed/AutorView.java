package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.usuario.Usuario;

/** Quem escreveu um post ou comentario, como aparece pra quem le. Nunca
 * carrega dado corporal. username e' null pra conta sem onboarding. */
public record AutorView(Long id, String nome, String username, String fotoUrl) {

    /** fotoUrl ja resolvida (FotoDePerfil): a propria, assinada, ou a do Google. */
    static AutorView de(Usuario usuario, String fotoUrl) {
        return new AutorView(usuario.getId(), usuario.getNome(), usuario.getUsername(), fotoUrl);
    }
}
