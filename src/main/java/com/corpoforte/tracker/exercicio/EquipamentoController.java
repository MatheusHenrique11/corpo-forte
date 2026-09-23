package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.Set;

/**
 * Sem Form/Bean Validation aqui de proposito: nao existe estado invalido
 * pra selecao de equipamento (qualquer subconjunto, inclusive vazio, e'
 * valido), entao um DTO so pra isso seria abstracao sem uso.
 */
@Controller
public class EquipamentoController {

    private final UsuarioAtualService usuarioAtualService;

    public EquipamentoController(UsuarioAtualService usuarioAtualService) {
        this.usuarioAtualService = usuarioAtualService;
    }

    @GetMapping("/equipamentos")
    public String exibirEquipamentos(@AuthenticationPrincipal OidcUser principal, Model model) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        model.addAttribute("opcoes", opcoesSelecionaveis());
        model.addAttribute("selecionados", usuario.getEquipamentosDisponiveis());
        return "equipamentos";
    }

    @PostMapping("/equipamentos")
    public String salvarEquipamentos(@RequestParam(required = false) Set<Equipamento> equipamentos,
                                      @AuthenticationPrincipal OidcUser principal) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        usuario.atualizarEquipamentos(equipamentos == null ? Set.of() : equipamentos);
        usuarioAtualService.salvar(usuario);
        return "redirect:/equipamentos";
    }

    private Equipamento[] opcoesSelecionaveis() {
        return Arrays.stream(Equipamento.values())
                .filter(equipamento -> equipamento != Equipamento.NENHUM)
                .toArray(Equipamento[]::new);
    }
}
