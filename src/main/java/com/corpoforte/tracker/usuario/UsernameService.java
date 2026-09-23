package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.api.ProblemaApi;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Username unico. A checagem antes de gravar existe pra dar uma resposta
 * clara (409 username-indisponivel); quem segura a corrida entre duas
 * pessoas escolhendo o mesmo nome ao mesmo tempo e' o indice unico em
 * lower(username) (V15), traduzido pro mesmo 409.
 */
@Service
public class UsernameService {

    private final UsuarioRepository usuarioRepository;

    public UsernameService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Vazio = disponivel. O username que ja e' da propria conta conta
     * como disponivel (quem edita o perfil e mantem o nome). */
    public Optional<MotivoUsernameIndisponivel> motivoIndisponivel(String candidato, Long usuarioIdAtual) {
        return RegraUsername.problemaSemConsultarBanco(candidato)
                .or(() -> usuarioRepository.findIdPorUsername(candidato)
                        .filter(dono -> !dono.equals(usuarioIdAtual))
                        .map(dono -> MotivoUsernameIndisponivel.EM_USO));
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findPorUsername(username);
    }

    /** 409 se o nome ja e' de outra conta. Chamado ANTES de mexer na conta:
     * validar depois deixaria a entidade alterada por um pedido recusado. */
    public void exigirLivre(String candidato, Long usuarioIdAtual) {
        if (motivoIndisponivel(candidato, usuarioIdAtual)
                .filter(motivo -> motivo == MotivoUsernameIndisponivel.EM_USO).isPresent()) {
            throw ProblemaApi.usernameIndisponivel();
        }
    }

    /**
     * Grava a conta com o username que ela acabou de receber. Mesmo depois
     * do exigirLivre, outra pessoa pode ter gravado o mesmo nome no meio
     * tempo: saveAndFlush faz a violacao do indice aparecer aqui dentro,
     * onde da pra traduzir, e nao no commit, depois que o metodo ja voltou.
     */
    public Usuario salvarComUsernameUnico(Usuario usuario) {
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw ProblemaApi.usernameIndisponivel();
        }
    }
}
