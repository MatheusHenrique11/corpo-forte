package com.corpoforte.tracker.usuario;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Privacidade")
public class PrivacidadeApiController {

    private final UsuarioAtualService usuarioAtualService;

    public PrivacidadeApiController(UsuarioAtualService usuarioAtualService) {
        this.usuarioAtualService = usuarioAtualService;
    }

    @Operation(summary = "Preferências de privacidade da conta")
    @GetMapping("/api/v1/privacidade")
    public PrivacidadeResposta obter(@AuthenticationPrincipal Jwt accessToken) {
        return PrivacidadeResposta.de(usuarioAtualService.obterUsuarioAtual(accessToken));
    }

    @Operation(summary = "Troca a visibilidade padrão dos próximos posts",
            description = "Não muda os posts que já existem: cada post guarda a visibilidade com que foi publicado.")
    @PutMapping("/api/v1/privacidade")
    public PrivacidadeResposta atualizar(@Valid @RequestBody PrivacidadeRequisicao requisicao,
                                         @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        usuario.definirVisibilidadePadrao(requisicao.visibilidadePadrao());
        return PrivacidadeResposta.de(usuarioAtualService.salvar(usuario));
    }
}
