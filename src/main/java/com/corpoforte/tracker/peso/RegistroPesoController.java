package com.corpoforte.tracker.peso;

import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
public class RegistroPesoController {

    private final UsuarioAtualService usuarioAtualService;
    private final RegistroPesoService registroPesoService;

    public RegistroPesoController(UsuarioAtualService usuarioAtualService,
                                   RegistroPesoService registroPesoService) {
        this.usuarioAtualService = usuarioAtualService;
        this.registroPesoService = registroPesoService;
    }

    @GetMapping("/peso")
    public String exibirRegistroPeso(@AuthenticationPrincipal OidcUser principal, Model model) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        model.addAttribute("registroPesoForm", new RegistroPesoForm());
        adicionarHistoricoETendencia(model, usuario.getId());
        return "peso";
    }

    @PostMapping("/peso")
    public String salvarRegistroPeso(@Valid @ModelAttribute("registroPesoForm") RegistroPesoForm form,
                                      BindingResult bindingResult, @AuthenticationPrincipal OidcUser principal,
                                      Model model) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);

        if (bindingResult.hasErrors()) {
            adicionarHistoricoETendencia(model, usuario.getId());
            return "peso";
        }

        registroPesoService.registrar(usuario, form.getData(), form.getPesoKg());

        return "redirect:/peso";
    }

    private void adicionarHistoricoETendencia(Model model, Long usuarioId) {
        model.addAttribute("registros", registroPesoService.listarDoUsuario(usuarioId));
        registroPesoService.tendenciaDoUsuario(usuarioId)
                .ifPresent(tendencia -> model.addAttribute("tendencia", tendencia));
    }
}
