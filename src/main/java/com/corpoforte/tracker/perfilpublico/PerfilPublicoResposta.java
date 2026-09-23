package com.corpoforte.tracker.perfilpublico;

import com.corpoforte.tracker.social.ContagemSocial;
import com.corpoforte.tracker.usuario.Usuario;

/**
 * O que qualquer conta ve de outra. Nenhum dado corporal: peso, altura,
 * idade, objetivo, nivel e avaliacao ficam de fora de proposito - sao da
 * propria pessoa, nunca da comunidade. contagens e' objeto pra ganhar
 * outras contagens sem mudar o formato. seguidoPorMim depende de quem ve:
 * e' o estado do botao de seguir.
 */
public record PerfilPublicoResposta(Long id, String username, String nome, String fotoUrl, String bio,
                                    Contagens contagens, boolean seguidoPorMim) {

    public record Contagens(long posts, long seguidores, long seguindo) {
    }

    static PerfilPublicoResposta de(Usuario usuario, long posts, ContagemSocial social, boolean seguidoPorMim) {
        return new PerfilPublicoResposta(usuario.getId(), usuario.getUsername(), usuario.getNome(),
                usuario.getFotoUrl(), usuario.getBio(),
                new Contagens(posts, social.seguidores(), social.seguindo()), seguidoPorMim);
    }
}
