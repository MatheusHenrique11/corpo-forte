package com.corpoforte.tracker.onboarding;

import com.corpoforte.tracker.api.PermitidoSemOnboarding;
import com.corpoforte.tracker.usuario.FotoDePerfil;
import com.corpoforte.tracker.usuario.Usuario;
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
    private final FotoDePerfil fotoDePerfil;

    public OnboardingApiController(UsuarioAtualService usuarioAtualService, OnboardingService onboardingService,
                                   FotoDePerfil fotoDePerfil) {
        this.usuarioAtualService = usuarioAtualService;
        this.onboardingService = onboardingService;
        this.fotoDePerfil = fotoDePerfil;
    }

    @Operation(summary = "Cadastro inicial: perfil, username e peso",
            description = "Enquanto não for feito, quase toda rota responde 409 "
                    + "(urn:corpo-forte:problema:onboarding-pendente). 409 username-indisponivel se o nome "
                    + "for de outra conta; 409 onboarding-ja-concluido se já foi feito.")
    @PostMapping("/api/v1/onboarding")
    @PermitidoSemOnboarding
    public UsuarioAtualResposta concluir(@Valid @RequestBody OnboardingForm form,
                                         @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = onboardingService.concluir(usuarioAtualService.obterUsuarioAtual(accessToken), form);
        return UsuarioAtualResposta.de(usuario, fotoDePerfil.url(usuario));
    }
}
