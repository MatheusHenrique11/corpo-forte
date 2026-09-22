package com.corpoforte.tracker.perfil;

import com.corpoforte.tracker.usuario.NivelTreino;
import com.corpoforte.tracker.usuario.ObjetivoTreino;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PerfilController {

    private final UsuarioAtualService usuarioAtualService;
    private final PerfilCalculoService perfilCalculoService;

    public PerfilController(UsuarioAtualService usuarioAtualService, PerfilCalculoService perfilCalculoService) {
        this.usuarioAtualService = usuarioAtualService;
        this.perfilCalculoService = perfilCalculoService;
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/perfil";
    }

    @GetMapping("/perfil")
    public String exibirPerfil(Model model) {
        Usuario usuario = usuarioAtualService.obterOuCriarPadrao();
        model.addAttribute("perfilForm", paraFormulario(usuario));
        model.addAttribute("resultado", calcularResultado(usuario));
        adicionarOpcoesDeEnum(model);
        return "perfil";
    }

    @PostMapping("/perfil")
    public String salvarPerfil(@Valid @ModelAttribute("perfilForm") PerfilForm form, BindingResult bindingResult, Model model) {
        adicionarOpcoesDeEnum(model);

        if (bindingResult.hasErrors()) {
            return "perfil";
        }

        Usuario usuario = usuarioAtualService.obterOuCriarPadrao();
        usuario.atualizarPerfil(form.getNome(), form.getPesoKg(), form.getAlturaCm(),
                form.getIdade(), form.getObjetivo(), form.getNivel());
        usuarioAtualService.salvar(usuario);

        return "redirect:/perfil";
    }

    private PerfilForm paraFormulario(Usuario usuario) {
        PerfilForm form = new PerfilForm();
        form.setNome(usuario.getNome());
        form.setPesoKg(usuario.getPesoKg());
        form.setAlturaCm(usuario.getAlturaCm());
        form.setIdade(usuario.getIdade());
        form.setObjetivo(usuario.getObjetivo());
        form.setNivel(usuario.getNivel());
        return form;
    }

    private PerfilResultado calcularResultado(Usuario usuario) {
        return perfilCalculoService.calcular(usuario.getPesoKg(), usuario.getAlturaCm(),
                usuario.getIdade(), usuario.getObjetivo());
    }

    private void adicionarOpcoesDeEnum(Model model) {
        model.addAttribute("objetivos", ObjetivoTreino.values());
        model.addAttribute("niveis", NivelTreino.values());
    }
}
