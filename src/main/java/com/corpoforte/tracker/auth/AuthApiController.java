package com.corpoforte.tracker.auth;

import com.corpoforte.tracker.usuario.DadosLogin;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unicas rotas da API que nao exigem access token (SecurityConfig): sao as
 * que o emitem. O login social acontece no cliente, com o SDK do provedor;
 * o back-end so' recebe o token do provedor, confere e troca por tokens
 * proprios.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
@SecurityRequirements
public class AuthApiController {

    private final VerificadorIdTokenGoogle verificadorIdTokenGoogle;
    private final UsuarioAtualService usuarioAtualService;
    private final RefreshTokenService refreshTokenService;
    private final EmissorTokens emissorTokens;

    public AuthApiController(VerificadorIdTokenGoogle verificadorIdTokenGoogle,
                             UsuarioAtualService usuarioAtualService,
                             RefreshTokenService refreshTokenService,
                             EmissorTokens emissorTokens) {
        this.verificadorIdTokenGoogle = verificadorIdTokenGoogle;
        this.usuarioAtualService = usuarioAtualService;
        this.refreshTokenService = refreshTokenService;
        this.emissorTokens = emissorTokens;
    }

    @Operation(summary = "Troca o ID token do Google Sign-In por tokens do Corpo Forte",
            description = "Primeiro login cria a conta. Mesma conta da sessão web para o mesmo login Google.")
    @PostMapping("/google")
    public TokensResposta loginGoogle(@Valid @RequestBody LoginGoogleRequisicao requisicao) {
        DadosLogin login = verificadorIdTokenGoogle.verificar(requisicao.idToken());
        Usuario usuario = usuarioAtualService.obterOuCriar(login);
        return emissorTokens.emitir(usuario.getId());
    }

    @Operation(summary = "Renova a sessão",
            description = "O refresh token enviado deixa de valer; use o novo que vem na resposta. "
                    + "Reusar um refresh token já trocado encerra todas as sessões da conta.")
    @PostMapping("/refresh")
    public TokensResposta renovar(@Valid @RequestBody RefreshTokenRequisicao requisicao) {
        Long usuarioId = refreshTokenService.consumir(requisicao.refreshToken());
        return emissorTokens.emitir(usuarioId);
    }

    @Operation(summary = "Encerra a sessão deste refresh token",
            description = "Idempotente: token desconhecido ou já revogado também responde 204.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(@Valid @RequestBody RefreshTokenRequisicao requisicao) {
        refreshTokenService.revogar(requisicao.refreshToken());
    }
}
