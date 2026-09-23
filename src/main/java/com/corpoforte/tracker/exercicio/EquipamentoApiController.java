package com.corpoforte.tracker.exercicio;

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

@RestController
@RequestMapping("/api/v1/equipamentos")
@Tag(name = "Equipamentos")
public class EquipamentoApiController {

    private final UsuarioAtualService usuarioAtualService;

    public EquipamentoApiController(UsuarioAtualService usuarioAtualService) {
        this.usuarioAtualService = usuarioAtualService;
    }

    @Operation(summary = "Equipamentos que o usuário tem")
    @GetMapping
    public EquipamentosResposta obter(@AuthenticationPrincipal Jwt accessToken) {
        return EquipamentosResposta.de(usuarioAtualService.obterUsuarioAtual(accessToken).getEquipamentosDisponiveis());
    }

    @Operation(summary = "Substitui os equipamentos do usuário",
            description = "NENHUM é ignorado: exercício de peso corporal já é sempre compatível.")
    @PutMapping
    public EquipamentosResposta atualizar(@Valid @RequestBody EquipamentosRequisicao requisicao,
                                          @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        usuario.atualizarEquipamentos(requisicao.equipamentos());
        return EquipamentosResposta.de(usuarioAtualService.salvar(usuario).getEquipamentosDisponiveis());
    }
}
