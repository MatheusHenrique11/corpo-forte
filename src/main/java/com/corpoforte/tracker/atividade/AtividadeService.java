package com.corpoforte.tracker.atividade;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Instantes;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.api.ProblemaApi;
import com.corpoforte.tracker.exercicio.Exercicio;
import com.corpoforte.tracker.exercicio.ExercicioService;
import com.corpoforte.tracker.exercicio.Medida;
import com.corpoforte.tracker.treino.TreinoDoDia;
import com.corpoforte.tracker.treino.TreinoDoDiaRepository;
import com.corpoforte.tracker.treino.TreinoItem;
import com.corpoforte.tracker.treino.TreinoItemRepository;
import com.corpoforte.tracker.usuario.Usuario;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Diario de treino (Fase 16). Duas formas de uma atividade nascer:
 * finalizando o treino do dia (a partir dos itens marcados) ou montando
 * um treino livre com exercicios do catalogo.
 *
 * Nada aqui toca a periodizacao: o ciclo da Fase 8 continua contando so'
 * os itens do treino do dia marcados como concluidos, e finalizar nao
 * muda essas marcacoes.
 */
@Service
public class AtividadeService {

    /** Teto por serie: 3600 segundos ja vem da validacao do corpo; em
     * repeticoes, 1000 numa serie so' e' erro de digitacao. */
    static final int MAXIMO_DE_REPETICOES = 1000;

    private final AtividadeRepository atividadeRepository;
    private final AtividadeSerieRepository atividadeSerieRepository;
    private final TreinoDoDiaRepository treinoDoDiaRepository;
    private final TreinoItemRepository treinoItemRepository;
    private final ExercicioService exercicioService;

    public AtividadeService(AtividadeRepository atividadeRepository,
                            AtividadeSerieRepository atividadeSerieRepository,
                            TreinoDoDiaRepository treinoDoDiaRepository, TreinoItemRepository treinoItemRepository,
                            ExercicioService exercicioService) {
        this.atividadeRepository = atividadeRepository;
        this.atividadeSerieRepository = atividadeSerieRepository;
        this.treinoDoDiaRepository = treinoDoDiaRepository;
        this.treinoItemRepository = treinoItemRepository;
        this.exercicioService = exercicioService;
    }

    /**
     * Os itens marcados do treino de hoje viram as series da atividade, na
     * ordem do treino (padrao de movimento). Cada item entra com a
     * prescricao (series x repeticoes) a menos que venha um ajuste pra ele.
     * Ajuste de item que nao esta entre os marcados de hoje - inclusive de
     * outra conta - e' recusado sem dizer se o item existe.
     */
    @Transactional
    public Atividade finalizarTreinoDoDia(Usuario usuario, FinalizacaoRequisicao requisicao) {
        FinalizacaoRequisicao pedido = requisicao != null ? requisicao : new FinalizacaoRequisicao(null, null, null, null);
        TreinoDoDia treino = treinoDoDiaRepository.findByUsuarioIdAndData(usuario.getId(), LocalDate.now())
                .orElseThrow(ProblemaApi::treinoSemItensConcluidos);
        if (atividadeRepository.existsByTreinoDoDiaId(treino.getId())) {
            throw ProblemaApi.treinoJaFinalizado();
        }

        Map<Long, Exercicio> catalogo = catalogo();
        List<TreinoItem> marcados = treinoItemRepository.findByTreinoDoDiaId(treino.getId()).stream()
                .filter(TreinoItem::isConcluido)
                .sorted(Comparator.comparingInt((TreinoItem item) -> catalogo.get(item.getExercicioId()).getMovimento().ordinal())
                        .thenComparing(TreinoItem::getId))
                .toList();
        if (marcados.isEmpty()) {
            throw ProblemaApi.treinoSemItensConcluidos();
        }

        Map<Long, List<Integer>> ajustes = ajustesPorItem(pedido.ajustes(), marcados);
        List<SeriesDeExercicio> feito = marcados.stream()
                .map(item -> new SeriesDeExercicio(item.getExercicioId(), ajustes.getOrDefault(item.getId(),
                        Collections.nCopies(item.getSeries(), item.getRepeticoes()))))
                .toList();

        return gravar(new Atividade(usuario.getId(), treino.getData(), OrigemAtividade.TREINO_DO_DIA, treino.getId(),
                pedido.duracaoMinutos(), pedido.esforcoPercebido(), pedido.notas(), agora()), feito, catalogo);
    }

    @Transactional
    public Atividade registrarLivre(Usuario usuario, AtividadeLivreRequisicao requisicao) {
        Map<Long, Exercicio> catalogo = catalogo();
        requisicao.exercicios().forEach(bloco -> {
            if (!catalogo.containsKey(bloco.exercicioId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Exercício " + bloco.exercicioId() + " não existe no catálogo");
            }
        });
        LocalDate data = requisicao.data() != null ? requisicao.data() : LocalDate.now();
        return gravar(new Atividade(usuario.getId(), data, OrigemAtividade.LIVRE, null, requisicao.duracaoMinutos(),
                requisicao.esforcoPercebido(), requisicao.notas(), agora()), requisicao.exercicios(), catalogo);
    }

    public Atividade obter(Long usuarioId, Long atividadeId) {
        return atividadeRepository.findByIdAndUsuarioId(atividadeId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Apagar e' apagar: as series saem pelo cascade do banco. */
    public void apagar(Long usuarioId, Long atividadeId) {
        atividadeRepository.delete(obter(usuarioId, atividadeId));
    }

    /** Diario da propria conta, dia mais recente primeiro. */
    public Pagina<Atividade> paginaDoUsuario(Long usuarioId, Cursor cursor, int tamanho) {
        Limit limite = Limit.of(tamanho + 1);
        List<Atividade> buscadas = cursor == null
                ? atividadeRepository.buscarDoUsuario(usuarioId, limite)
                : atividadeRepository.buscarDoUsuarioApos(usuarioId, cursor.comoData(), cursor.id(), limite);
        return Pagina.deBuscaComUmAMais(buscadas, tamanho, atividade -> Cursor.apos(atividade.getData(), atividade.getId()));
    }

    /** Series de todas as atividades numa consulta so', agrupadas em blocos
     * de series seguidas do mesmo exercicio. */
    public List<AtividadeResposta> respostas(List<Atividade> atividades) {
        if (atividades.isEmpty()) {
            return List.of();
        }
        Map<Long, Exercicio> catalogo = catalogo();
        Map<Long, List<AtividadeSerie>> seriesPorAtividade = atividadeSerieRepository
                .findByAtividadeIdInOrderByAtividadeIdAscOrdemAsc(atividades.stream().map(Atividade::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(AtividadeSerie::getAtividadeId));

        return atividades.stream()
                .map(atividade -> new AtividadeResposta(atividade.getId(), atividade.getData(), atividade.getOrigem(),
                        atividade.getDuracaoMinutos(), atividade.getEsforcoPercebido(), atividade.getNotas(),
                        Instantes.emUtc(atividade.getCriadoEm()),
                        blocos(seriesPorAtividade.getOrDefault(atividade.getId(), List.of()), catalogo)))
                .toList();
    }

    private Atividade gravar(Atividade atividade, List<SeriesDeExercicio> feito, Map<Long, Exercicio> catalogo) {
        feito.forEach(bloco -> exigirDentroDaMedida(bloco, catalogo.get(bloco.exercicioId())));

        Atividade salva;
        try {
            // flush aqui: duas finalizacoes simultaneas do mesmo treino
            // esbarram no unique(treino_do_dia_id) dentro deste metodo, onde
            // da pra responder o mesmo 409 da checagem de cima
            salva = atividadeRepository.saveAndFlush(atividade);
        } catch (DataIntegrityViolationException e) {
            if (atividade.getTreinoDoDiaId() == null) {
                throw e;
            }
            throw ProblemaApi.treinoJaFinalizado();
        }

        List<AtividadeSerie> series = new ArrayList<>();
        int ordem = 0;
        for (SeriesDeExercicio bloco : feito) {
            for (Integer valor : bloco.series()) {
                series.add(new AtividadeSerie(salva.getId(), bloco.exercicioId(), ordem++, valor));
            }
        }
        atividadeSerieRepository.saveAll(series);
        return salva;
    }

    private static void exigirDentroDaMedida(SeriesDeExercicio bloco, Exercicio exercicio) {
        if (exercicio.getMedida() == Medida.REPETICOES
                && bloco.series().stream().anyMatch(valor -> valor > MAXIMO_DE_REPETICOES)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No máximo " + MAXIMO_DE_REPETICOES + " repetições por série em " + exercicio.getNome());
        }
    }

    private static Map<Long, List<Integer>> ajustesPorItem(List<FinalizacaoRequisicao.Ajuste> ajustes,
                                                           List<TreinoItem> marcados) {
        if (ajustes == null) {
            return Map.of();
        }
        List<Long> idsMarcados = marcados.stream().map(TreinoItem::getId).toList();
        Map<Long, List<Integer>> porItem = new HashMap<>();
        for (FinalizacaoRequisicao.Ajuste ajuste : ajustes) {
            if (!idsMarcados.contains(ajuste.itemId()) || porItem.put(ajuste.itemId(), ajuste.series()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ajuste do item " + ajuste.itemId() + " não corresponde a um item marcado hoje");
            }
        }
        return porItem;
    }

    /** Series seguidas do mesmo exercicio formam um bloco; voltar a um
     * exercicio depois de outro abre um bloco novo. */
    private static List<AtividadeResposta.Bloco> blocos(List<AtividadeSerie> series, Map<Long, Exercicio> catalogo) {
        List<AtividadeResposta.Bloco> blocos = new ArrayList<>();
        Long exercicioAtual = null;
        List<Integer> valores = new ArrayList<>();
        for (AtividadeSerie serie : series) {
            if (!Objects.equals(serie.getExercicioId(), exercicioAtual) && !valores.isEmpty()) {
                blocos.add(bloco(catalogo.get(exercicioAtual), valores));
                valores = new ArrayList<>();
            }
            exercicioAtual = serie.getExercicioId();
            valores.add(serie.getValor());
        }
        if (!valores.isEmpty()) {
            blocos.add(bloco(catalogo.get(exercicioAtual), valores));
        }
        return blocos;
    }

    private static AtividadeResposta.Bloco bloco(Exercicio exercicio, List<Integer> valores) {
        return new AtividadeResposta.Bloco(
                new AtividadeResposta.ExercicioDoBloco(exercicio.getId(), exercicio.getNome(), exercicio.getMovimento(),
                        exercicio.getMedida()),
                List.copyOf(valores), valores.stream().mapToInt(Integer::intValue).sum());
    }

    private Map<Long, Exercicio> catalogo() {
        return exercicioService.listarTodos().stream().collect(Collectors.toMap(Exercicio::getId, Function.identity()));
    }

    private static LocalDateTime agora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
