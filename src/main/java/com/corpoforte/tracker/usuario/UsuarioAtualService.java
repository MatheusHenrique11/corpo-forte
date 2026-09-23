package com.corpoforte.tracker.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Ate a Fase 5 o sistema rodava com um unico usuario local
 * (obterOuCriarPadrao, removido na Fase 6). Desde entao o usuario atual vem
 * do login: busca pela IdentidadeExterna (provedor + sub) e, se nao achar,
 * reivindica a conta local (se sobrar exatamente uma sem login nenhum, o
 * e-mail que logou bater com app.owner-email e vier verificado) ou cria uma
 * conta nova.
 *
 * Desde a Fase 10 existem duas portas de entrada - sessao web (OidcUser) e
 * API (ID token trocado por token proprio) - e as duas passam por
 * obterOuCriar(DadosLogin). Nao existe um segundo caminho de login que
 * possa divergir deste.
 */
@Service
public class UsuarioAtualService {

    private static final int TAMANHO_MAXIMO_NOME = 120;

    private final UsuarioRepository usuarioRepository;
    private final IdentidadeExternaRepository identidadeExternaRepository;
    private final TransactionOperations transacao;
    private final String ownerEmail;

    @Autowired
    public UsuarioAtualService(UsuarioRepository usuarioRepository,
                                IdentidadeExternaRepository identidadeExternaRepository,
                                PlatformTransactionManager transactionManager,
                                @Value("${app.owner-email:}") String ownerEmail) {
        this(usuarioRepository, identidadeExternaRepository, new TransactionTemplate(transactionManager), ownerEmail);
    }

    /** Pra teste unitario: TransactionOperations.withoutTransaction(). */
    UsuarioAtualService(UsuarioRepository usuarioRepository,
                        IdentidadeExternaRepository identidadeExternaRepository,
                        TransactionOperations transacao, String ownerEmail) {
        this.usuarioRepository = usuarioRepository;
        this.identidadeExternaRepository = identidadeExternaRepository;
        this.transacao = transacao;
        this.ownerEmail = ownerEmail;
    }

    /** Sessao web (telas Thymeleaf). */
    public Usuario obterUsuarioAtual(OidcUser principal) {
        return obterOuCriar(DadosLogin.deGoogle(principal));
    }

    /**
     * API: o "sub" do access token proprio e' o id do Usuario (ver
     * EmissorTokens). Conta que nao existe mais (apagada depois de o token
     * ser emitido) da 401, nao 500 - o token deixou de representar alguem.
     */
    public Usuario obterUsuarioAtual(Jwt accessToken) {
        return usuarioRepository.findById(Long.valueOf(accessToken.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
    }

    public Usuario obterOuCriar(DadosLogin login) {
        return buscarPorIdentidade(login).orElseGet(() -> reivindicarOuCriarComRetry(login));
    }

    public Usuario salvar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    private Optional<Usuario> buscarPorIdentidade(DadosLogin login) {
        return identidadeExternaRepository.findByProvedorAndSub(login.provedor(), login.sub())
                .flatMap(identidade -> usuarioRepository.findById(identidade.getUsuarioId()));
    }

    /**
     * Dois primeiros logins do mesmo provedor/sub chegando ao mesmo tempo
     * passam os dois pelo buscarPorIdentidade sem achar nada, e o segundo
     * esbarra no unique(provedor, sub) de verdade
     * (IdentidadeExternaRepositoryIT). Em vez de deixar virar 500, busca de
     * novo: a outra requisicao acabou de criar exatamente essa identidade.
     *
     * A transacao e' o que torna isso seguro desde que o login grava DUAS
     * linhas (Usuario + IdentidadeExterna, Fase 10): sem ela, a requisicao
     * perdedora deixaria um Usuario sem identidade nenhuma no banco - que
     * findSemIdentidadeExterna trataria como conta orfa reivindicavel.
     */
    private Usuario reivindicarOuCriarComRetry(DadosLogin login) {
        try {
            return transacao.execute(status -> reivindicarOuCriar(login));
        } catch (DataIntegrityViolationException e) {
            return buscarPorIdentidade(login).orElseThrow(() -> e);
        }
    }

    private Usuario reivindicarOuCriar(DadosLogin login) {
        Usuario usuario = contaOrfaReivindicavel(login)
                .orElseGet(() -> new Usuario(
                        nomeDaContaNova(login),
                        100,
                        178,
                        27,
                        ObjetivoTreino.PERDA_GORDURA,
                        NivelTreino.INICIANTE));

        usuario.vincularEmail(login.email());
        Usuario salvo = usuarioRepository.save(usuario);

        identidadeExternaRepository.save(
                new IdentidadeExterna(salvo.getId(), login.provedor(), login.sub(), LocalDateTime.now()));

        return salvo;
    }

    /**
     * usuario.nome e' not null varchar(120), mas o "name" do provedor nao e'
     * garantido: pela API, o cliente escolhe os escopos, e um login sem
     * "profile" chega sem nome - o que faria o primeiro login dar 500. O
     * usuario corrige o nome depois no perfil.
     */
    private static String nomeDaContaNova(DadosLogin login) {
        String nome = login.nome();
        if (nome == null || nome.isBlank()) {
            nome = login.email() != null && login.email().contains("@")
                    ? login.email().substring(0, login.email().indexOf('@'))
                    : "Atleta";
        }
        return nome.length() > TAMANHO_MAXIMO_NOME ? nome.substring(0, TAMANHO_MAXIMO_NOME) : nome;
    }

    /**
     * So reivindica a conta local existente pro dono configurado
     * explicitamente (app.owner-email) - sem isso, o primeiro login do
     * sistema (ex.: alguem testando, ou um ataque) herdava a conta com todo
     * o historico ja acumulado. E-mail nao verificado tambem bloqueia: nao
     * da pra confiar num e-mail que o proprio provedor nao confirmou.
     * app.owner-email vazio (padrao) desativa a reivindicacao inteiramente.
     */
    private Optional<Usuario> contaOrfaReivindicavel(DadosLogin login) {
        if (ownerEmail.isBlank()) {
            return Optional.empty();
        }
        boolean emailEhDoDono = ownerEmail.equalsIgnoreCase(login.email());
        if (!login.emailVerificado() || !emailEhDoDono) {
            return Optional.empty();
        }

        List<Usuario> semLogin = usuarioRepository.findSemIdentidadeExterna();
        return semLogin.size() == 1 ? Optional.of(semLogin.get(0)) : Optional.empty();
    }
}
