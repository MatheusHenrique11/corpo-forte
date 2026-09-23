package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.avaliacao.AvaliacaoFisica;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaItemResultado;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaService;
import com.corpoforte.tracker.exercicio.Exercicio;
import com.corpoforte.tracker.exercicio.ExercicioFiltroService;
import com.corpoforte.tracker.exercicio.ExercicioService;
import com.corpoforte.tracker.exercicio.MovimentoPadrao;
import com.corpoforte.tracker.usuario.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final AvaliacaoFisicaService avaliacaoFisicaService;
    private final TreinoDoDiaGeradorService treinoDoDiaGeradorService;
    private final PeriodizacaoService periodizacaoService;

    public TreinoDoDiaService(TreinoDoDiaRepository treinoDoDiaRepository, TreinoItemRepository treinoItemRepository,
                               ExercicioService exercicioService, ExercicioFiltroService exercicioFiltroService,
                               AvaliacaoFisicaService avaliacaoFisicaService,
                               TreinoDoDiaGeradorService treinoDoDiaGeradorService,
                               PeriodizacaoService periodizacaoService) {
        this.treinoDoDiaRepository = treinoDoDiaRepository;
        this.treinoItemRepository = treinoItemRepository;
        this.exercicioService = exercicioService;
        this.exercicioFiltroService = exercicioFiltroService;
        this.avaliacaoFisicaService = avaliacaoFisicaService;
        this.treinoDoDiaGeradorService = treinoDoDiaGeradorService;
        this.periodizacaoService = periodizacaoService;
    }

    /**
     * Treino de hoje pra quem ja fez avaliacao fisica; vazio pra quem nao
     * fez (sem repeticoes maximas nao ha volume pra calcular). A regra
     * morava no controller da tela e desceu pra ca pra API seguir a mesma.
     * O ciclo ancora na avaliacao mais recente (Fases 8 e 9).
     */
    public Optional<TreinoDoDiaView> obterOuGerarDoDia(Usuario usuario) {
        return avaliacaoFisicaService.obterMaisRecenteDoUsuario(usuario.getId())
                .map(avaliacao -> obterOuGerarDoDia(usuario, avaliacao));
    }

    public TreinoDoDiaView obterOuGerarDoDia(Usuario usuario, AvaliacaoFisica avaliacaoFisica) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioDoCiclo = avaliacaoFisica.getDataAvaliacao();
        int semanasProgredidas = semanasProgredidasNoCiclo(usuario.getId(), inicioDoCiclo, hoje);

        TreinoDoDia treino = treinoDoDiaRepository.findByUsuarioIdAndData(usuario.getId(), hoje)
                .orElseGet(() -> gerarNovo(usuario, avaliacaoFisica, hoje, semanasProgredidas));

        return paraView(treino, periodizacaoService.status(inicioDoCiclo, hoje, semanasProgredidas));
    }

    /**
     * O ciclo e' ancorado em AvaliacaoFisica.dataAvaliacao (nao numa data
     * separada): refazer a avaliacao atualiza essa data e reinicia o ciclo
     * de graca, sem campo nem logica de reset.
     */
    private int semanasProgredidasNoCiclo(Long usuarioId, LocalDate inicioDoCiclo, LocalDate hoje) {
        List<TreinoDoDia> treinosDoCiclo =
                treinoDoDiaRepository.findByUsuarioIdAndDataGreaterThanEqual(usuarioId, inicioDoCiclo);

        if (treinosDoCiclo.isEmpty()) {
            return 0;
        }

        Map<Long, LocalDate> dataPorTreinoId = treinosDoCiclo.stream()
                .collect(Collectors.toMap(TreinoDoDia::getId, TreinoDoDia::getData));

        Set<LocalDate> semanasComTreinoConcluido =
                treinoItemRepository.findByTreinoDoDiaIdInAndConcluidoTrue(List.copyOf(dataPorTreinoId.keySet()))
                        .stream()
                        .map(item -> periodizacaoService.inicioDaSemana(dataPorTreinoId.get(item.getTreinoDoDiaId())))
                        .collect(Collectors.toSet());

        return periodizacaoService.semanasProgredidas(semanasComTreinoConcluido, hoje);
    }

    /**
     * usuarioId vem do usuario autenticado (Controller), nunca do cliente.
     * Confere que o item pertence a um TreinoDoDia desse usuario antes de
     * alterar - sem essa checagem, dava pra alternar o item de qualquer
     * outro usuario so adivinhando/incrementando o ID (Fase 6).
     * 404 em vez de 403 pra nao confirmar pra quem nao e' dono que o ID
     * existe.
     */
    public void alternarConclusao(Long usuarioId, Long itemId) {
        TreinoItem item = itemDoUsuario(usuarioId, itemId);
        item.alternarConclusao();
        treinoItemRepository.save(item);
    }

    /**
     * Versao idempotente pra API: marcar duas vezes continua marcado. Com o
     * toggle da tela, um cliente que repete a requisicao depois de
     * uma falha de rede desfaria o que o usuario acabou de marcar. Mesma
     * checagem de dono do alternarConclusao.
     */
    public void definirConclusao(Long usuarioId, Long itemId, boolean concluido) {
        TreinoItem item = itemDoUsuario(usuarioId, itemId);
        item.definirConclusao(concluido);
        treinoItemRepository.save(item);
    }

    private TreinoItem itemDoUsuario(Long usuarioId, Long itemId) {
        TreinoItem item = treinoItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        TreinoDoDia treino = treinoDoDiaRepository.findById(item.getTreinoDoDiaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!treino.getUsuarioId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return item;
    }

    private TreinoDoDia gerarNovo(Usuario usuario, AvaliacaoFisica avaliacaoFisica, LocalDate hoje,
                                   int semanasProgredidas) {
        List<AvaliacaoFisicaItemResultado> volumes = avaliacaoFisicaService.resultadoDe(avaliacaoFisica);

        List<Exercicio> catalogo = exercicioService.listarTodos();
        Map<MovimentoPadrao, List<Exercicio>> candidatosPorMovimento = new HashMap<>();
        for (MovimentoPadrao movimento : MovimentoPadrao.values()) {
            List<Exercicio> candidatos = exercicioFiltroService.filtrar(catalogo, usuario.getNivel(), movimento, null)
                    .stream()
                    .filter(exercicio -> exercicioFiltroService.ehCompativel(exercicio, usuario.getEquipamentosDisponiveis()))
                    .toList();
            candidatosPorMovimento.put(movimento, candidatos);
        }

        TreinoGerado gerado = treinoDoDiaGeradorService.gerar(volumes, candidatosPorMovimento, semanasProgredidas);

        TreinoDoDia treinoDoDia = treinoDoDiaRepository.save(new TreinoDoDia(usuario.getId(), hoje));
        for (ItemGerado item : gerado.itens()) {
            treinoItemRepository.save(new TreinoItem(treinoDoDia.getId(), item.exercicio().getId(),
                    item.series(), item.repeticoes()));
        }

        return treinoDoDia;
    }

    private TreinoDoDiaView paraView(TreinoDoDia treino, CicloStatus ciclo) {
        List<TreinoItem> itens = treinoItemRepository.findByTreinoDoDiaId(treino.getId());
        Map<Long, Exercicio> catalogoPorId = exercicioService.listarTodos().stream()
                .collect(Collectors.toMap(Exercicio::getId, Function.identity()));

        List<TreinoItemView> views = itens.stream()
                .map(item -> {
                    Exercicio exercicio = catalogoPorId.get(item.getExercicioId());
                    return new TreinoItemView(item.getId(), exercicio.getMovimento(), exercicio.getId(),
                            exercicio.getNome(), item.getSeries(), item.getRepeticoes(), item.isConcluido());
                })
                .sorted(Comparator.comparingInt(view -> view.movimento().ordinal()))
                .toList();

        Set<MovimentoPadrao> presentes = views.stream().map(TreinoItemView::movimento).collect(Collectors.toSet());
        List<MovimentoPadrao> semOpcao = Arrays.stream(MovimentoPadrao.values())
                .filter(movimento -> !presentes.contains(movimento))
                .toList();

        return new TreinoDoDiaView(treino.getData(), views, semOpcao, ciclo);
    }
}
