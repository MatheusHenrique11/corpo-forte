package com.corpoforte.tracker.perfil;

import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mesmo PerfilForm da tela como corpo do PUT: a validacao de altura, idade
 * etc. mora num lugar so' - era exatamente o que a Fase 1 previa quando
 * pos Bean Validation no form e nao no controller.
 */
@RestController
@RequestMapping("/api/v1/perfil")
@Tag(name = "Perfil")
public class PerfilApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final PerfilCalculoService perfilCalculoService;

    public PerfilApiController(UsuarioAtualService usuarioAtualService, PerfilCalculoService perfilCalculoService) {
        this.usuarioAtualService = usuarioAtualService;
        this.perfilCalculoService = perfilCalculoService;
    }

    @Operation(summary = "Perfil e cálculo nutricional (TMB, TDEE, IMC, macros)")
    @GetMapping
    public PerfilResposta obter(@AuthenticationPrincipal Jwt accessToken) {
        return resposta(usuarioAtualService.obterUsuarioAtual(accessToken));
    }

    @Operation(summary = "Atualiza o perfil",
            description = "Peso não entra aqui: ele vem sempre do registro de peso mais recente (POST /api/v1/pesos).")
    @PutMapping
    public PerfilResposta atualizar(@Valid @RequestBody PerfilForm form, @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        usuario.atualizarPerfil(form.getNome(), form.getAlturaCm(), form.getIdade(), form.getObjetivo(),
                form.getNivel());
        return resposta(usuarioAtualService.salvar(usuario));
    }

    private PerfilResposta resposta(Usuario usuario) {
        return PerfilResposta.de(usuario, perfilCalculoService.calcular(
                usuario.getPesoKg(), usuario.getAlturaCm(), usuario.getIdade(), usuario.getObjetivo()));
    }
}
