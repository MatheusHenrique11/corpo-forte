package com.corpoforte.tracker.usuario;

/**
 * Resposta de GET /api/v1/me. Record proprio em vez da entidade Usuario:
 * serializar a entidade exporia peso, altura e idade, e qualquer coluna
 * nova entraria na resposta sem ninguem decidir. Campo novo aqui e'
 * sempre uma escolha explicita.
 */
public record UsuarioAtualResposta(Long id, String nome, String email) {

    static UsuarioAtualResposta de(Usuario usuario) {
        return new UsuarioAtualResposta(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
