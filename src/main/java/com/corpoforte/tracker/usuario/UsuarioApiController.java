package com.corpoforte.tracker.usuario;

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

    public UsuarioApiController(UsuarioAtualService usuarioAtualService) {
        this.usuarioAtualService = usuarioAtualService;
    }

    @Operation(summary = "Conta dona do access token")
    @GetMapping("/api/v1/me")
    public UsuarioAtualResposta me(@AuthenticationPrincipal Jwt accessToken) {
        return UsuarioAtualResposta.de(usuarioAtualService.obterUsuarioAtual(accessToken));
    }
}
