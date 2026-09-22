package com.corpoforte.tracker.peso;

import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class RegistroPesoController {

    private final UsuarioAtualService usuarioAtualService;
    private final RegistroPesoService registroPesoService;
    private final RegistroPesoCalculoService registroPesoCalculoService;

    public RegistroPesoController(UsuarioAtualService usuarioAtualService,
                                   RegistroPesoService registroPesoService,
                                   RegistroPesoCalculoService registroPesoCalculoService) {
        this.usuarioAtualService = usuarioAtualService;
        this.registroPesoService = registroPesoService;
        this.registroPesoCalculoService = registroPesoCalculoService;
    }

    @GetMapping("/peso")
    public String exibirRegistroPeso(Model model) {
        Usuario usuario = usuarioAtualService.obterOuCriarPadrao();
        model.addAttribute("registroPesoForm", new RegistroPesoForm());
        adicionarHistoricoETendencia(model, usuario.getId());
        return "peso";
    }

    @PostMapping("/peso")
    public String salvarRegistroPeso(@Valid @ModelAttribute("registroPesoForm") RegistroPesoForm form,
                                      BindingResult bindingResult, Model model) {
        Usuario usuario = usuarioAtualService.obterOuCriarPadrao();

        if (bindingResult.hasErrors()) {
            adicionarHistoricoETendencia(model, usuario.getId());
            return "peso";
        }

        registroPesoService.salvar(usuario.getId(), form.getData(), form.getPesoKg());

        registroPesoService.obterMaisRecenteDoUsuario(usuario.getId())
                .ifPresent(maisRecente -> {
                    usuario.atualizarPeso(maisRecente.getPesoKg());
                    usuarioAtualService.salvar(usuario);
                });

        return "redirect:/peso";
    }

    private void adicionarHistoricoETendencia(Model model, Long usuarioId) {
        List<RegistroPeso> registros = registroPesoService.listarDoUsuario(usuarioId);
        model.addAttribute("registros", registros);

        if (!registros.isEmpty()) {
            List<RegistroPesoPonto> pontos = registros.stream()
                    .map(r -> new RegistroPesoPonto(r.getData(), r.getPesoKg()))
                    .toList();
            model.addAttribute("tendencia", registroPesoCalculoService.calcularTendencia(pontos));
        }
    }
}
