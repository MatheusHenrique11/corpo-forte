package com.corpoforte.tracker.usuario;

/** Preferencias de privacidade da conta. Objeto pra ganhar outras
 * preferencias sem mudar o formato. */
public record PrivacidadeResposta(Visibilidade visibilidadePadrao) {

    static PrivacidadeResposta de(Usuario usuario) {
        return new PrivacidadeResposta(usuario.getVisibilidadePadrao());
    }
}
