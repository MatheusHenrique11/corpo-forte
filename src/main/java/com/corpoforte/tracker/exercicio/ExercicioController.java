package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.NivelTreino;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ExercicioController {

    private final UsuarioAtualService usuarioAtualService;
    private final ExercicioService exercicioService;
    private final ExercicioFiltroService exercicioFiltroService;

    public ExercicioController(UsuarioAtualService usuarioAtualService, ExercicioService exercicioService,
                                ExercicioFiltroService exercicioFiltroService) {
        this.usuarioAtualService = usuarioAtualService;
        this.exercicioService = exercicioService;
        this.exercicioFiltroService = exercicioFiltroService;
    }

    @GetMapping("/exercicios")
    public String listarExercicios(
            @RequestParam(required = false) NivelTreino nivel,
            @RequestParam(required = false) MovimentoPadrao movimento,
            @RequestParam(required = false) Equipamento equipamento,
            @RequestParam(defaultValue = "false") boolean apenasCompativel,
            Model model) {
        Usuario usuario = usuarioAtualService.obterOuCriarPadrao();

        List<Exercicio> exercicios = exercicioFiltroService.filtrar(
                exercicioService.listarTodos(), nivel, movimento, equipamento);

        if (apenasCompativel) {
            exercicios = exercicios.stream()
                    .filter(exercicio -> exercicioFiltroService.ehCompativel(exercicio, usuario.getEquipamentosDisponiveis()))
                    .toList();
        }

        model.addAttribute("exercicios", exercicios);
        model.addAttribute("niveis", NivelTreino.values());
        model.addAttribute("movimentos", MovimentoPadrao.values());
        model.addAttribute("equipamentos", Equipamento.values());
        model.addAttribute("nivelSelecionado", nivel);
        model.addAttribute("movimentoSelecionado", movimento);
        model.addAttribute("equipamentoSelecionado", equipamento);
        model.addAttribute("apenasCompativel", apenasCompativel);

        return "exercicios";
    }
}
