package com.corpoforte.tracker.social;

import com.corpoforte.tracker.usuario.Usuario;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Uma conta numa lista (seguidores, seguindo, busca): so' a identidade
 * publica, nunca dado corporal. seguidoPorMim depende de quem pede - e' o
 * estado do botao de seguir ao lado de cada conta.
 */
public record UsuarioResumoResposta(Long id, String username, String nome, String fotoUrl, boolean seguidoPorMim) {

    /** fotoDe: FotoDePerfil::url, chamado so' pras contas que ja estao na
     * lista - ou seja, depois da regra de bloqueio. */
    public static List<UsuarioResumoResposta> de(List<Usuario> usuarios, Set<Long> seguidosPorQuemVe,
                                                 Function<Usuario, String> fotoDe) {
        return usuarios.stream()
                .map(usuario -> new UsuarioResumoResposta(usuario.getId(), usuario.getUsername(), usuario.getNome(),
                        fotoDe.apply(usuario), seguidosPorQuemVe.contains(usuario.getId())))
                .toList();
    }
}
