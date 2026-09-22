package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.avaliacao.AvaliacaoFisica;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaService;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class TreinoDoDiaController {

    private final UsuarioAtualService usuarioAtualService;
    private final AvaliacaoFisicaService avaliacaoFisicaService;
    private final TreinoDoDiaService treinoDoDiaService;

    public TreinoDoDiaController(UsuarioAtualService usuarioAtualService, AvaliacaoFisicaService avaliacaoFisicaService,
                                  TreinoDoDiaService treinoDoDiaService) {
        this.usuarioAtualService = usuarioAtualService;
        this.avaliacaoFisicaService = avaliacaoFisicaService;
        this.treinoDoDiaService = treinoDoDiaService;
    }

    @GetMapping("/treino-do-dia")
    public String exibirTreinoDoDia(Model model) {
        Usuario usuario = usuarioAtualService.obterOuCriarPadrao();
        Optional<AvaliacaoFisica> avaliacao = avaliacaoFisicaService.obterDoUsuario(usuario.getId());

        if (avaliacao.isEmpty()) {
            model.addAttribute("semAvaliacao", true);
            return "treino-do-dia";
        }

        TreinoDoDiaView treino = treinoDoDiaService.obterOuGerarDoDia(usuario, avaliacao.get());
        model.addAttribute("treino", treino);

        return "treino-do-dia";
    }

    @PostMapping("/treino-do-dia/itens/{itemId}/concluir")
    public String alternarConclusao(@PathVariable Long itemId) {
        treinoDoDiaService.alternarConclusao(itemId);
        return "redirect:/treino-do-dia";
    }
}
