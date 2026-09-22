package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.avaliacao.AvaliacaoFisica;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaCalculoService;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaItemResultado;
import com.corpoforte.tracker.exercicio.Exercicio;
import com.corpoforte.tracker.exercicio.ExercicioFiltroService;
import com.corpoforte.tracker.exercicio.ExercicioService;
import com.corpoforte.tracker.exercicio.MovimentoPadrao;
import com.corpoforte.tracker.usuario.Usuario;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Persistencia + montagem da view do treino do dia: mesmo papel que
 * AvaliacaoFisicaService/RegistroPesoService tem pras suas entidades. O
 * Controller nunca chama os repositories direto.
 */
@Service
public class TreinoDoDiaService {

    private final TreinoDoDiaRepository treinoDoDiaRepository;
    private final TreinoItemRepository treinoItemRepository;
    private final ExercicioService exercicioService;
    private final ExercicioFiltroService exercicioFiltroService;
    private final AvaliacaoFisicaCalculoService avaliacaoFisicaCalculoService;
    private final TreinoDoDiaGeradorService treinoDoDiaGeradorService;

    public TreinoDoDiaService(TreinoDoDiaRepository treinoDoDiaRepository, TreinoItemRepository treinoItemRepository,
                               ExercicioService exercicioService, ExercicioFiltroService exercicioFiltroService,
                               AvaliacaoFisicaCalculoService avaliacaoFisicaCalculoService,
                               TreinoDoDiaGeradorService treinoDoDiaGeradorService) {
        this.treinoDoDiaRepository = treinoDoDiaRepository;
        this.treinoItemRepository = treinoItemRepository;
        this.exercicioService = exercicioService;
        this.exercicioFiltroService = exercicioFiltroService;
        this.avaliacaoFisicaCalculoService = avaliacaoFisicaCalculoService;
        this.treinoDoDiaGeradorService = treinoDoDiaGeradorService;
    }

    public TreinoDoDiaView obterOuGerarDoDia(Usuario usuario, AvaliacaoFisica avaliacaoFisica) {
        LocalDate hoje = LocalDate.now();

        TreinoDoDia treino = treinoDoDiaRepository.findByUsuarioIdAndData(usuario.getId(), hoje)
                .orElseGet(() -> gerarNovo(usuario, avaliacaoFisica, hoje));

        return paraView(treino);
    }

    public void alternarConclusao(Long itemId) {
        TreinoItem item = treinoItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item de treino nao encontrado: " + itemId));
        item.alternarConclusao();
        treinoItemRepository.save(item);
    }

    private TreinoDoDia gerarNovo(Usuario usuario, AvaliacaoFisica avaliacaoFisica, LocalDate hoje) {
        List<AvaliacaoFisicaItemResultado> volumes = avaliacaoFisicaCalculoService.calcular(
                avaliacaoFisica.getRepsPuxarVertical(), avaliacaoFisica.getRepsEmpurrarVertical(),
                avaliacaoFisica.getRepsPernasBilateral(), avaliacaoFisica.getRepsPuxarHorizontal(),
                avaliacaoFisica.getRepsEmpurrarHorizontal(), avaliacaoFisica.getRepsPernasUnilateral());

        List<Exercicio> catalogo = exercicioService.listarTodos();
        Map<MovimentoPadrao, List<Exercicio>> candidatosPorMovimento = new HashMap<>();
        for (MovimentoPadrao movimento : MovimentoPadrao.values()) {
            List<Exercicio> candidatos = exercicioFiltroService.filtrar(catalogo, usuario.getNivel(), movimento, null)
                    .stream()
                    .filter(exercicio -> exercicioFiltroService.ehCompativel(exercicio, usuario.getEquipamentosDisponiveis()))
                    .toList();
            candidatosPorMovimento.put(movimento, candidatos);
        }

        TreinoGerado gerado = treinoDoDiaGeradorService.gerar(volumes, candidatosPorMovimento);

        TreinoDoDia treinoDoDia = treinoDoDiaRepository.save(new TreinoDoDia(usuario.getId(), hoje));
        for (ItemGerado item : gerado.itens()) {
            treinoItemRepository.save(new TreinoItem(treinoDoDia.getId(), item.exercicio().getId(),
                    item.series(), item.repeticoes()));
        }

        return treinoDoDia;
    }

    private TreinoDoDiaView paraView(TreinoDoDia treino) {
        List<TreinoItem> itens = treinoItemRepository.findByTreinoDoDiaId(treino.getId());
        Map<Long, Exercicio> catalogoPorId = exercicioService.listarTodos().stream()
                .collect(Collectors.toMap(Exercicio::getId, Function.identity()));

        List<TreinoItemView> views = itens.stream()
                .map(item -> {
                    Exercicio exercicio = catalogoPorId.get(item.getExercicioId());
                    return new TreinoItemView(item.getId(), exercicio.getMovimento(), exercicio.getNome(),
                            item.getSeries(), item.getRepeticoes(), item.isConcluido());
                })
                .sorted(java.util.Comparator.comparingInt(view -> view.movimento().ordinal()))
                .toList();

        Set<MovimentoPadrao> presentes = views.stream().map(TreinoItemView::movimento).collect(Collectors.toSet());
        List<MovimentoPadrao> semOpcao = Arrays.stream(MovimentoPadrao.values())
                .filter(movimento -> !presentes.contains(movimento))
                .toList();

        return new TreinoDoDiaView(views, semOpcao);
    }
}
