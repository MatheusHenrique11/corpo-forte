package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre a migracao do usuario local pra multiusuario (Fase 6): o primeiro
 * login do e-mail configurado em app.owner-email (com email_verified=true)
 * reivindica a conta local existente (se sobrar exatamente uma sem
 * googleSub) em vez de descartar historico ja acumulado; logins seguintes
 * do mesmo Google não duplicam; um segundo Google distinto ganha conta
 * propria; e-mail diferente do dono ou nao verificado nunca reivindica,
 * mesmo com a conta orfa disponivel - cria conta nova em vez disso.
 */
@Transactional
class UsuarioAtualServiceIT extends IntegrationTestBase {

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void primeiroLoginDoDonoConfiguradoReivindicaAUnicaContaLocalOrfaExistente() {
        Usuario contaLocal = usuarioRepository.save(new Usuario(
                "Meu Perfil", 82.0, 178, 30, ObjetivoTreino.PERDA_GORDURA, NivelTreino.INTERMEDIARIO));

        OidcUser principal = OidcTestUsers.principal("sub-reivindicacao", "Nome do Google", OWNER_EMAIL);
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);

        // reivindicou a linha existente (mesmo id, historico preservado -
        // peso/nivel continuam os da conta local), nao criou uma segunda
        assertThat(usuario.getId()).isEqualTo(contaLocal.getId());
        assertThat(usuario.getPesoKg()).isEqualTo(82.0);
        assertThat(usuario.getGoogleSub()).isEqualTo("sub-reivindicacao");
        assertThat(usuario.getEmail()).isEqualTo(OWNER_EMAIL);
        assertThat(usuarioRepository.findAll()).hasSize(1);
    }

    @Test
    void emailDiferenteDoDonoNaoReivindicaContaOrfaMesmoVerificado() {
        Usuario contaLocal = usuarioRepository.save(new Usuario(
                "Meu Perfil", 82.0, 178, 30, ObjetivoTreino.PERDA_GORDURA, NivelTreino.INTERMEDIARIO));

        OidcUser intruso = OidcTestUsers.principal("sub-intruso", "Outra Pessoa", "outra-pessoa@exemplo.com");
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(intruso);

        // conta nova, criada do zero - a conta local com historico continua
        // orfa, intacta, esperando o dono de verdade logar
        assertThat(usuario.getId()).isNotEqualTo(contaLocal.getId());
        assertThat(usuarioRepository.findByGoogleSubIsNull()).containsExactly(contaLocal);
        assertThat(usuarioRepository.findAll()).hasSize(2);
    }

    @Test
    void emailDoDonoNaoVerificadoNaoReivindicaContaOrfa() {
        Usuario contaLocal = usuarioRepository.save(new Usuario(
                "Meu Perfil", 82.0, 178, 30, ObjetivoTreino.PERDA_GORDURA, NivelTreino.INTERMEDIARIO));

        OidcUser naoVerificado = OidcTestUsers.principal("sub-nao-verificado", "Nome do Google", OWNER_EMAIL, false);
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(naoVerificado);

        assertThat(usuario.getId()).isNotEqualTo(contaLocal.getId());
        assertThat(usuarioRepository.findAll()).hasSize(2);
    }

    @Test
    void logarDeNovoComOMesmoGoogleSubDevolveOMesmoUsuarioSemDuplicar() {
        OidcUser principal = OidcTestUsers.principal("sub-repetido", "Fulana", "fulana@exemplo.com");

        Usuario primeiroLogin = usuarioAtualService.obterUsuarioAtual(principal);
        Usuario segundoLogin = usuarioAtualService.obterUsuarioAtual(principal);

        assertThat(segundoLogin.getId()).isEqualTo(primeiroLogin.getId());
        assertThat(usuarioRepository.findAll()).hasSize(1);
    }

    @Test
    void semContaOrfaSobrandoUmSegundoGoogleDistintoCriaContaPropria() {
        OidcUser primeiroUsuario = OidcTestUsers.principal("sub-a", "Usuaria A", "a@exemplo.com");
        OidcUser segundoUsuario = OidcTestUsers.principal("sub-b", "Usuario B", "b@exemplo.com");

        Usuario a = usuarioAtualService.obterUsuarioAtual(primeiroUsuario);
        Usuario b = usuarioAtualService.obterUsuarioAtual(segundoUsuario);

        assertThat(a.getId()).isNotEqualTo(b.getId());
        assertThat(usuarioRepository.findAll()).hasSize(2);
    }
}
