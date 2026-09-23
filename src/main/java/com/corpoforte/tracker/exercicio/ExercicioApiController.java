package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.NivelTreino;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@Tag(name = "Exercícios")
public class ExercicioApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final ExercicioService exercicioService;
    private final ExercicioFiltroService exercicioFiltroService;

    public ExercicioApiController(UsuarioAtualService usuarioAtualService, ExercicioService exercicioService,
                                  ExercicioFiltroService exercicioFiltroService) {
        this.usuarioAtualService = usuarioAtualService;
        this.exercicioService = exercicioService;
        this.exercicioFiltroService = exercicioFiltroService;
    }

    /**
     * Catalogo inteiro numa pagina so' (e' fixo e pequeno), no mesmo
     * envelope das listas paginadas. Ordem por id, pra resposta nao
     * depender da ordem em que o banco devolve as linhas.
     */
    @Operation(summary = "Catálogo de exercícios, com filtros opcionais",
            description = "apenasCompativel=true deixa só o que dá pra fazer com os equipamentos do usuário.")
    @GetMapping("/api/v1/exercicios")
    public Pagina<ExercicioResposta> listar(@RequestParam(required = false) NivelTreino nivel,
                                            @RequestParam(required = false) MovimentoPadrao movimento,
                                            @RequestParam(required = false) Equipamento equipamento,
                                            @RequestParam(defaultValue = "false") boolean apenasCompativel,
                                            @AuthenticationPrincipal Jwt accessToken) {
        List<Exercicio> exercicios = exercicioFiltroService.filtrar(
                exercicioService.listarTodos(), nivel, movimento, equipamento);

        if (apenasCompativel) {
            Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
            exercicios = exercicios.stream()
                    .filter(exercicio -> exercicioFiltroService.ehCompativel(
                            exercicio, usuario.getEquipamentosDisponiveis()))
                    .toList();
        }

        return Pagina.completa(exercicios.stream()
                .sorted(Comparator.comparing(Exercicio::getId))
                .map(ExercicioResposta::de)
                .toList());
    }
}
