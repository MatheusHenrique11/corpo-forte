package com.corpoforte.tracker.avaliacao;

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

import java.util.List;
import java.util.Optional;

@Controller
public class AvaliacaoFisicaController {

    private final UsuarioAtualService usuarioAtualService;
    private final AvaliacaoFisicaService avaliacaoFisicaService;

    public AvaliacaoFisicaController(UsuarioAtualService usuarioAtualService,
                                      AvaliacaoFisicaService avaliacaoFisicaService) {
        this.usuarioAtualService = usuarioAtualService;
        this.avaliacaoFisicaService = avaliacaoFisicaService;
    }

    @GetMapping("/avaliacao")
    public String exibirAvaliacao(@AuthenticationPrincipal OidcUser principal, Model model) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        List<AvaliacaoFisica> historico = avaliacaoFisicaService.listarHistorico(usuario.getId());
        Optional<AvaliacaoFisica> avaliacao = historico.stream().findFirst();

        model.addAttribute("avaliacaoForm", avaliacao.map(this::paraFormulario).orElseGet(AvaliacaoFisicaForm::new));
        avaliacao.ifPresent(a -> model.addAttribute("resultado", avaliacaoFisicaService.resultadoDe(a)));
        model.addAttribute("historico", historico);

        avaliacaoFisicaService.compararUltimas(usuario.getId()).ifPresent(comparacao -> {
            model.addAttribute("comparacao", comparacao.itens());
            model.addAttribute("dataAnterior", comparacao.dataAnterior());
        });

        return "avaliacao";
    }

    @PostMapping("/avaliacao")
    public String salvarAvaliacao(@Valid @ModelAttribute("avaliacaoForm") AvaliacaoFisicaForm form,
                                   BindingResult bindingResult, @AuthenticationPrincipal OidcUser principal,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            return "avaliacao";
        }

        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        avaliacaoFisicaService.salvar(usuario.getId(),
                form.getRepsPuxarVertical(), form.getRepsEmpurrarVertical(), form.getRepsPernasBilateral(),
                form.getRepsPuxarHorizontal(), form.getRepsEmpurrarHorizontal(), form.getRepsPernasUnilateral());

        return "redirect:/avaliacao";
    }

    private AvaliacaoFisicaForm paraFormulario(AvaliacaoFisica avaliacao) {
        AvaliacaoFisicaForm form = new AvaliacaoFisicaForm();
        form.setRepsPuxarVertical(avaliacao.getRepsPuxarVertical());
        form.setRepsEmpurrarVertical(avaliacao.getRepsEmpurrarVertical());
        form.setRepsPernasBilateral(avaliacao.getRepsPernasBilateral());
        form.setRepsPuxarHorizontal(avaliacao.getRepsPuxarHorizontal());
        form.setRepsEmpurrarHorizontal(avaliacao.getRepsEmpurrarHorizontal());
        form.setRepsPernasUnilateral(avaliacao.getRepsPernasUnilateral());
        return form;
    }
}
