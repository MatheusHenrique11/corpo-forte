package com.corpoforte.tracker.usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Ate a Fase 5 o sistema rodava com um unico usuario local
 * (obterOuCriarPadrao, removido nesta fase). A partir da Fase 6, o usuario
 * atual vem do login com Google: busca por googleSub e, se nao achar,
 * reivindica a conta local (se sobrar exatamente uma sem googleSub, o
 * e-mail que logou bater com app.owner-email e vier com email_verified) ou
 * cria uma conta nova - ver Usuario.vincularConta(...).
 */
@Service
public class UsuarioAtualService {

    private final UsuarioRepository usuarioRepository;
    private final String ownerEmail;

    public UsuarioAtualService(UsuarioRepository usuarioRepository,
                                @Value("${app.owner-email:}") String ownerEmail) {
        this.usuarioRepository = usuarioRepository;
        this.ownerEmail = ownerEmail;
    }

    public Usuario obterUsuarioAtual(OidcUser principal) {
        return usuarioRepository.findByGoogleSub(principal.getSubject())
                .orElseGet(() -> reivindicarOuCriarComRetry(principal));
    }

    /**
     * Dois primeiros logins do mesmo Google chegando ao mesmo tempo passam
     * os dois pelo findByGoogleSub sem achar nada (nenhum ainda commitou),
     * e o segundo save() esbarra na constraint unique de verdade
     * (UsuarioRepositoryIT) e lança DataIntegrityViolationException. Em vez
     * de deixar virar 500, busca de novo por googleSub: a outra requisição
     * acabou de criar/reivindicar exatamente essa linha.
     */
    private Usuario reivindicarOuCriarComRetry(OidcUser principal) {
        try {
            return reivindicarOuCriar(principal);
        } catch (DataIntegrityViolationException e) {
            return usuarioRepository.findByGoogleSub(principal.getSubject()).orElseThrow(() -> e);
        }
    }

    public Usuario salvar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    private Usuario reivindicarOuCriar(OidcUser principal) {
        Usuario usuario = contaOrfaReivindicavel(principal)
                .orElseGet(() -> new Usuario(
                        principal.getFullName(),
                        100,
                        178,
                        27,
                        ObjetivoTreino.PERDA_GORDURA,
                        NivelTreino.INICIANTE));

        usuario.vincularConta(principal.getSubject(), principal.getEmail());

        return usuarioRepository.save(usuario);
    }

    /**
     * So reivindica a conta local existente pro dono configurado
     * explicitamente (app.owner-email) - sem isso, o primeiro Google que
     * logasse no sistema (ex.: alguem testando, ou um ataque) herdava a
     * conta com todo o historico ja acumulado. email_verified=false (Google
     * permite login sem verificar e-mail em alguns fluxos) tambem bloqueia:
     * nao da pra confiar num e-mail que o proprio Google nao confirmou.
     * app.owner-email vazio (padrao) desativa a reivindicacao inteiramente.
     */
    private Optional<Usuario> contaOrfaReivindicavel(OidcUser principal) {
        if (ownerEmail.isBlank()) {
            return Optional.empty();
        }
        boolean emailVerificado = Boolean.TRUE.equals(principal.getEmailVerified());
        boolean emailEhDoDono = ownerEmail.equalsIgnoreCase(principal.getEmail());
        if (!emailVerificado || !emailEhDoDono) {
            return Optional.empty();
        }

        List<Usuario> semGoogleSub = usuarioRepository.findByGoogleSubIsNull();
        return semGoogleSub.size() == 1 ? Optional.of(semGoogleSub.get(0)) : Optional.empty();
    }
}
