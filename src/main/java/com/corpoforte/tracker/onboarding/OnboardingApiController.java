package com.corpoforte.tracker.onboarding;

import com.corpoforte.tracker.api.PermitidoSemOnboarding;
import com.corpoforte.tracker.usuario.UsuarioAtualResposta;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Onboarding")
public class OnboardingApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final OnboardingService onboardingService;

    public OnboardingApiController(UsuarioAtualService usuarioAtualService, OnboardingService onboardingService) {
        this.usuarioAtualService = usuarioAtualService;
        this.onboardingService = onboardingService;
    }

    @Operation(summary = "Cadastro inicial: perfil, username e peso",
            description = "Enquanto não for feito, quase toda rota responde 409 "
                    + "(urn:corpo-forte:problema:onboarding-pendente). 409 username-indisponivel se o nome "
                    + "for de outra conta; 409 onboarding-ja-concluido se já foi feito.")
    @PostMapping("/api/v1/onboarding")
    @PermitidoSemOnboarding
    public UsuarioAtualResposta concluir(@Valid @RequestBody OnboardingForm form,
                                         @AuthenticationPrincipal Jwt accessToken) {
        return UsuarioAtualResposta.de(
                onboardingService.concluir(usuarioAtualService.obterUsuarioAtual(accessToken), form));
    }
}
