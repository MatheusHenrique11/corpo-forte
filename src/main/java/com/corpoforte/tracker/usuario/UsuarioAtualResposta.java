package com.corpoforte.tracker.usuario;

/**
 * Resposta de GET /api/v1/me. Record proprio em vez da entidade Usuario:
 * serializar a entidade exporia peso, altura e idade, e qualquer coluna
 * nova entraria na resposta sem ninguem decidir. Campo novo aqui e'
 * sempre uma escolha explicita.
 *
 * onboardingConcluido e' o que o cliente olha logo depois do login pra
 * decidir entre o cadastro inicial e o app; username vem null ate la.
 */
public record UsuarioAtualResposta(Long id, String nome, String email, String username, String fotoUrl,
                                   boolean onboardingConcluido) {

    /** fotoUrl ja resolvida por FotoDePerfil (a enviada, assinada, ou a do Google). */
    public static UsuarioAtualResposta de(Usuario usuario, String fotoUrl) {
        return new UsuarioAtualResposta(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getUsername(), fotoUrl, usuario.isOnboardingConcluido());
    }
}
