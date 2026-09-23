package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.api.PermitidoSemOnboarding;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Conta")
public class UsernameApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final UsernameService usernameService;

    public UsernameApiController(UsuarioAtualService usuarioAtualService, UsernameService usernameService) {
        this.usuarioAtualService = usuarioAtualService;
        this.usernameService = usernameService;
    }

    /**
     * Pro cliente validar enquanto a pessoa digita. Liberado antes do
     * onboarding porque e' justamente no onboarding que se escolhe o nome.
     * Nao reserva nada: quem garante e' a gravacao (409 se alguem pegou o
     * nome no meio tempo).
     */
    @Operation(summary = "Se o nome de usuário pode ser usado por esta conta",
            description = "O nome que já é da própria conta aparece como disponível.")
    @GetMapping("/api/v1/usernames/{username}/disponivel")
    @PermitidoSemOnboarding
    public DisponibilidadeUsernameResposta disponivel(@PathVariable String username,
                                                      @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return usernameService.motivoIndisponivel(username, usuario.getId())
                .map(motivo -> new DisponibilidadeUsernameResposta(username, false, motivo))
                .orElseGet(() -> new DisponibilidadeUsernameResposta(username, true, null));
    }
}
