package com.corpoforte.tracker.social;

import com.corpoforte.tracker.usuario.Usuario;

import java.util.List;
import java.util.Set;

/**
 * Uma conta numa lista (seguidores, seguindo, busca): so' a identidade
 * publica, nunca dado corporal. seguidoPorMim depende de quem pede - e' o
 * estado do botao de seguir ao lado de cada conta.
 */
public record UsuarioResumoResposta(Long id, String username, String nome, String fotoUrl, boolean seguidoPorMim) {

    public static List<UsuarioResumoResposta> de(List<Usuario> usuarios, Set<Long> seguidosPorQuemVe) {
        return usuarios.stream()
                .map(usuario -> new UsuarioResumoResposta(usuario.getId(), usuario.getUsername(), usuario.getNome(),
                        usuario.getFotoUrl(), seguidosPorQuemVe.contains(usuario.getId())))
                .toList();
    }
}
