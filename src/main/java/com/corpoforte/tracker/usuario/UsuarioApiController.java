package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.api.PermitidoSemOnboarding;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Conta")
public class UsuarioApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final FotoDePerfil fotoDePerfil;

    public UsuarioApiController(UsuarioAtualService usuarioAtualService, FotoDePerfil fotoDePerfil) {
        this.usuarioAtualService = usuarioAtualService;
        this.fotoDePerfil = fotoDePerfil;
    }

    @Operation(summary = "Conta dona do access token")
    @GetMapping("/api/v1/me")
    @PermitidoSemOnboarding
    public UsuarioAtualResposta me(@AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return UsuarioAtualResposta.de(usuario, fotoDePerfil.url(usuario));
    }
}
