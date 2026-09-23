package com.corpoforte.tracker.auth;

import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * So' no perfil dev: emite tokens da API pro usuario ja logado na sessao
 * web, pra testar a API pelo Swagger UI (botao "Authorize") ou por curl sem
 * precisar de um cliente com Google Sign-In.
 *
 * Fora de /api de proposito: e' a chain WEB (sessao + login Google) que
 * autentica esta rota. GET, e nao POST, pra bastar abrir no navegador; um
 * site terceiro que dispare esse GET nao consegue ler a resposta (CORS), e
 * a unica coisa que ele causaria e' uma sessao a mais pro proprio usuario.
 */
@RestController
@Profile("dev")
public class TokenDevController {

    private final UsuarioAtualService usuarioAtualService;
    private final EmissorTokens emissorTokens;

    public TokenDevController(UsuarioAtualService usuarioAtualService, EmissorTokens emissorTokens) {
        this.usuarioAtualService = usuarioAtualService;
        this.emissorTokens = emissorTokens;
    }

    @GetMapping("/dev/token-api")
    public TokensResposta emitir(@AuthenticationPrincipal OidcUser principal) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        return emissorTokens.emitir(usuario.getId());
    }
}
