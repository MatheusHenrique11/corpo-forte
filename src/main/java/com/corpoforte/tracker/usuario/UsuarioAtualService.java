package com.corpoforte.tracker.usuario;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Ate a Fase 6 (login com Google) o sistema roda com um unico usuario
 * local. Essa regra temporaria fica concentrada aqui: quando o login
 * chegar, so este metodo muda (passa a ler o usuario autenticado do
 * SecurityContext em vez do primeiro registro da tabela); o resto da
 * aplicacao nao precisa saber da diferenca.
 */
@Service
public class UsuarioAtualService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAtualService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obterOuCriarPadrao() {
        return usuarioRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> usuarioRepository.save(new Usuario(
                        "Meu Perfil",
                        100,
                        178,
                        27,
                        ObjetivoTreino.PERDA_GORDURA,
                        NivelTreino.INICIANTE,
                        LocalDate.now()
                )));
    }

    public Usuario salvar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }
}
