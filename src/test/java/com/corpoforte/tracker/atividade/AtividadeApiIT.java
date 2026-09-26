package com.corpoforte.tracker.atividade;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisica;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaRepository;
import com.corpoforte.tracker.exercicio.Exercicio;
import com.corpoforte.tracker.exercicio.ExercicioRepository;
import com.corpoforte.tracker.treino.TreinoDoDia;
import com.corpoforte.tracker.treino.TreinoDoDiaRepository;
import com.corpoforte.tracker.treino.TreinoItem;
import com.corpoforte.tracker.treino.TreinoItemRepository;
import com.corpoforte.tracker.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Diario de treino pela API: as duas formas de uma atividade nascer
 * (finalizar o treino do dia, registrar treino livre), a leitura e o
 * apagar.
 *
 * O treino do dia e' montado direto pelo repository, com exercicios
 * escolhidos pelo nome, em vez de gerado: o gerador sorteia (Fase 5), e
 * estes testes precisam saber qual exercicio e qual medida caiu em cada
 * item.
 */
@AutoConfigureMockMvc
@Transactional
class AtividadeApiIT extends IntegrationTestBase {

    private static final String FINALIZAR = "/api/v1/treino-do-dia/finalizar";
    private static final String ATIVIDADES = "/api/v1/atividades";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExercicioRepository exercicioRepository;

    @Autowired
    private TreinoDoDiaRepository treinoDoDiaRepository;

    @Autowired
    private TreinoItemRepository treinoItemRepository;

    @Autowired
    private AvaliacaoFisicaRepository avaliacaoFisicaRepository;

    @Autowired
    private AtividadeService atividadeService;

    @Autowired
    private AtividadeSerieRepository atividadeSerieRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario usuario;

    @BeforeEach
    void criarUsuario() {
        usuario = contaComOnboarding(OidcTestUsers.principal("sub-atividade-api", "Fulana", "atividade-api@exemplo.com"));
    }

    // ---- Finalizar o treino do dia ----

    /**
     * Sem corpo nenhum: cada item marcado entra com a prescricao, na ordem
     * do treino (padrao de movimento), e o que nao foi marcado fica de
     * fora. O isometrico mantem o valor prescrito, lido em segundos.
     */
    @Test
    void finalizarSemCorpoRegistraAPrescricaoDosItensMarcados() throws Exception {
        TreinoDoDia treino = treinoDeHoje();
        TreinoItem flexao = item(treino, "Flexao completa", 3, 10, true);
        item(treino, "Agachamento na parede (wall sit)", 3, 8, true);
        item(treino, "Barra fixa completa", 3, 6, true);
        TreinoItem naoFeito = item(treino, "Afundo com passada", 3, 12, false);

        mockMvc.perform(post(FINALIZAR).header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origem").value("TREINO_DO_DIA"))
                .andExpect(jsonPath("$.data").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.exercicios", hasSize(3)))
                .andExpect(jsonPath("$.exercicios[0].exercicio.nome").value("Barra fixa completa"))
                .andExpect(jsonPath("$.exercicios[0].series", contains(6, 6, 6)))
                .andExpect(jsonPath("$.exercicios[0].total").value(18))
                .andExpect(jsonPath("$.exercicios[0].exercicio.medida").value("REPETICOES"))
                .andExpect(jsonPath("$.exercicios[1].exercicio.nome").value("Agachamento na parede (wall sit)"))
                .andExpect(jsonPath("$.exercicios[1].exercicio.medida").value("SEGUNDOS"))
                .andExpect(jsonPath("$.exercicios[1].exercicio.movimento").value("PERNAS_BILATERAL"))
                .andExpect(jsonPath("$.exercicios[2].exercicio.nome").value("Flexao completa"))
                .andExpect(jsonPath("$.duracaoMinutos").doesNotExist());

        // finalizar registra, nao mexe no checklist: e' ele que a
        // periodizacao le
        assertThat(treinoItemRepository.findById(flexao.getId()).orElseThrow().isConcluido()).isTrue();
        assertThat(treinoItemRepository.findById(naoFeito.getId()).orElseThrow().isConcluido()).isFalse();
    }

    /** Quem fez 8 em vez de 10 registra 8. */
    @Test
    void ajusteTrocaAPrescricaoDoQueFoiFeitoDiferente() throws Exception {
        TreinoDoDia treino = treinoDeHoje();
        TreinoItem flexao = item(treino, "Flexao completa", 3, 10, true);
        item(treino, "Barra fixa completa", 3, 6, true);

        mockMvc.perform(post(FINALIZAR).header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": 45, "esforcoPercebido": 8, "notas": "Cansada hoje",
                                 "ajustes": [{"itemId": %d, "series": [10, 10, 8, 5]}]}
                                """.formatted(flexao.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracaoMinutos").value(45))
                .andExpect(jsonPath("$.esforcoPercebido").value(8))
                .andExpect(jsonPath("$.notas").value("Cansada hoje"))
                .andExpect(jsonPath("$.exercicios[0].series", contains(6, 6, 6)))
                .andExpect(jsonPath("$.exercicios[1].series", contains(10, 10, 8, 5)))
                .andExpect(jsonPath("$.exercicios[1].total").value(33));
    }

    @Test
    void semTreinoHojeOuSemItemMarcadoRespondeProblemaTipado() throws Exception {
        String tipo = "urn:corpo-forte:problema:treino-sem-itens-concluidos";
        finalizar().andExpect(status().isConflict()).andExpect(jsonPath("$.type").value(tipo));

        item(treinoDeHoje(), "Flexao completa", 3, 10, false);
        finalizar().andExpect(status().isConflict()).andExpect(jsonPath("$.type").value(tipo));

        assertThat(atividadesDaConta()).isEmpty();
    }

    /** O treino do dia vira no maximo uma atividade. Apagar a atividade
     * libera finalizar de novo (ex.: pra corrigir o que foi registrado). */
    @Test
    void finalizarDuasVezesResponde409EApagarLiberaDeNovo() throws Exception {
        item(treinoDeHoje(), "Flexao completa", 3, 10, true);
        Integer id = JsonPath.read(finalizar().andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        finalizar().andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:corpo-forte:problema:treino-ja-finalizado"));

        mockMvc.perform(delete(ATIVIDADES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isNoContent());
        finalizar().andExpect(status().isCreated());
    }

    /** Ajuste so' vale pra item marcado hoje. O mesmo 400 pra item nao
     * marcado, inexistente ou repetido - nada diz se o id existe. */
    @Test
    void ajusteDeItemQueNaoEstaMarcadoHojeEhRecusado() throws Exception {
        TreinoDoDia treino = treinoDeHoje();
        TreinoItem marcado = item(treino, "Flexao completa", 3, 10, true);
        TreinoItem naoMarcado = item(treino, "Barra fixa completa", 3, 6, false);

        for (String ajustes : List.of(
                "[{\"itemId\": %d, \"series\": [5]}]".formatted(naoMarcado.getId()),
                "[{\"itemId\": 999999, \"series\": [5]}]",
                "[{\"itemId\": %1$d, \"series\": [5]}, {\"itemId\": %1$d, \"series\": [6]}]".formatted(marcado.getId()))) {
            mockMvc.perform(post(FINALIZAR).header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"ajustes\": " + ajustes + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail", containsString("não corresponde a um item marcado hoje")));
        }
        assertThat(atividadesDaConta()).isEmpty();
    }

    @Test
    void finalizarValidaOCorpo() throws Exception {
        TreinoItem flexao = item(treinoDeHoje(), "Flexao completa", 3, 10, true);

        mockMvc.perform(post(FINALIZAR).header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"esforcoPercebido": 11, "duracaoMinutos": 0,
                                 "ajustes": [{"itemId": %d, "series": []}]}
                                """.formatted(flexao.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo",
                        containsInAnyOrder("esforcoPercebido", "duracaoMinutos", "ajustes[0].series")));
        // repeticao acima do teto numa serie so' e' erro de digitacao
        mockMvc.perform(post(FINALIZAR).header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ajustes\": [{\"itemId\": %d, \"series\": [1001]}]}".formatted(flexao.getId())))
                .andExpect(status().isBadRequest());
        assertThat(atividadesDaConta()).isEmpty();
    }

    // ---- Treino livre ----

    /** Series seguidas do mesmo exercicio formam um bloco; voltar a um
     * exercicio depois de outro abre um bloco novo, na ordem feita. */
    @Test
    void treinoLivreGuardaAOrdemFeitaEmBlocos() throws Exception {
        Long flexao = exercicio("Flexao completa").getId();
        Long barra = exercicio("Barra fixa completa").getId();
        Long frontLever = exercicio("Front lever tuck (isometrico)").getId();
        LocalDate ontem = LocalDate.now().minusDays(1);

        registrarLivre("""
                {"data": "%s", "duracaoMinutos": 30,
                 "exercicios": [{"exercicioId": %d, "series": [12, 10]},
                                {"exercicioId": %d, "series": [5]},
                                {"exercicioId": %d, "series": [15, 12]},
                                {"exercicioId": %d, "series": [8]}]}
                """.formatted(ontem, flexao, barra, frontLever, flexao))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origem").value("LIVRE"))
                .andExpect(jsonPath("$.data").value(ontem.toString()))
                .andExpect(jsonPath("$.exercicios", hasSize(4)))
                .andExpect(jsonPath("$.exercicios[*].exercicio.id", contains(
                        flexao.intValue(), barra.intValue(), frontLever.intValue(), flexao.intValue())))
                .andExpect(jsonPath("$.exercicios[0].series", contains(12, 10)))
                .andExpect(jsonPath("$.exercicios[0].total").value(22))
                .andExpect(jsonPath("$.exercicios[2].exercicio.medida").value("SEGUNDOS"))
                .andExpect(jsonPath("$.exercicios[2].total").value(27))
                .andExpect(jsonPath("$.exercicios[3].series", contains(8)));
    }

    @Test
    void treinoLivreSemDataValeHoje() throws Exception {
        registrarLivre("{\"exercicios\": [{\"exercicioId\": %d, \"series\": [10]}]}"
                .formatted(exercicio("Flexao completa").getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").value(LocalDate.now().toString()));
    }

    @Test
    void treinoLivreValidaOCorpo() throws Exception {
        Long flexao = exercicio("Flexao completa").getId();
        Long wallSit = exercicio("Agachamento na parede (wall sit)").getId();

        registrarLivre("{\"exercicios\": []}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("exercicios"));
        registrarLivre("{\"data\": \"%s\", \"exercicios\": [{\"exercicioId\": %d, \"series\": [10]}]}"
                .formatted(LocalDate.now().plusDays(1), flexao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("data"));
        registrarLivre("{\"exercicios\": [{\"exercicioId\": %d, \"series\": [0]}]}".formatted(flexao))
                .andExpect(status().isBadRequest());
        registrarLivre("{\"exercicios\": [{\"exercicioId\": 999999, \"series\": [10]}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("não existe no catálogo")));
        // o teto de repeticoes nao vale pra segundos: 20 minutos de wall sit
        // e' raro, nao erro de digitacao
        registrarLivre("{\"exercicios\": [{\"exercicioId\": %d, \"series\": [1200]}]}".formatted(flexao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("repetições por série")));
        assertThat(atividadesDaConta()).isEmpty();

        registrarLivre("{\"exercicios\": [{\"exercicioId\": %d, \"series\": [1200]}]}".formatted(wallSit))
                .andExpect(status().isCreated());
    }

    // ---- Diario ----

    /** Keyset (data, id): 21 atividades no mesmo dia dao exatamente 20 + 1,
     * sem repetir nem pular - o id desempata. */
    @Test
    void diarioPaginaDoDiaMaisRecenteSemRepetirNemPular() throws Exception {
        Long flexao = exercicio("Flexao completa").getId();
        LocalDate dia = LocalDate.of(2026, 5, 1);
        for (int i = 0; i < Pagina.TAMANHO_PADRAO + 1; i++) {
            atividadeService.registrarLivre(usuario, new AtividadeLivreRequisicao(
                    dia, null, null, "treino " + i, List.of(new SeriesDeExercicio(flexao, List.of(10)))));
        }
        atividadeService.registrarLivre(usuario, new AtividadeLivreRequisicao(
                dia.plusDays(1), null, null, "mais recente", List.of(new SeriesDeExercicio(flexao, List.of(10)))));

        String primeira = diario(null)
                .andExpect(jsonPath("$.itens", hasSize(Pagina.TAMANHO_PADRAO)))
                .andExpect(jsonPath("$.itens[0].notas").value("mais recente"))
                .andExpect(jsonPath("$.itens[0].exercicios[0].series", contains(10)))
                .andReturn().getResponse().getContentAsString();
        String cursor = JsonPath.read(primeira, "$.proximoCursor");
        String segunda = diario(cursor)
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.proximoCursor").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        List<Integer> ids = new ArrayList<>(JsonPath.<List<Integer>>read(primeira, "$.itens[*].id"));
        ids.addAll(JsonPath.read(segunda, "$.itens[*].id"));
        assertThat(ids).hasSize(Pagina.TAMANHO_PADRAO + 2);
        assertThat(new HashSet<>(ids)).hasSize(Pagina.TAMANHO_PADRAO + 2);
    }

    @Test
    void obterEApagarUmaAtividade() throws Exception {
        Integer id = JsonPath.read(registrarLivre("{\"exercicios\": [{\"exercicioId\": %d, \"series\": [10, 8]}]}"
                        .formatted(exercicio("Flexao completa").getId()))
                .andReturn().getResponse().getContentAsString(), "$.id");
        String rota = ATIVIDADES + "/" + id;

        mockMvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.exercicios[0].total").value(18));

        mockMvc.perform(delete(rota).header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(rota).header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isNotFound());
        assertThat(atividadeSerieRepository.findAll())
                .noneMatch(serie -> serie.getAtividadeId().equals(id.longValue()));
    }

    /**
     * A periodizacao (Fase 8) e' sobre o programa prescrito: treino livre
     * na semana passada nao avanca o ciclo. So' o checklist do treino do
     * dia conta (PeriodizacaoIT, sem alteracao nesta fase).
     */
    @Test
    void treinoLivreNaoAvancaOCiclo() throws Exception {
        avaliacaoFisicaRepository.save(new AvaliacaoFisica(
                usuario.getId(), 10, 10, 10, 10, 10, 10, LocalDate.now().minusWeeks(3)));
        atividadeService.registrarLivre(usuario, new AtividadeLivreRequisicao(
                LocalDate.now().minusWeeks(1), null, null, null,
                List.of(new SeriesDeExercicio(exercicio("Flexao completa").getId(), List.of(10, 10, 10)))));

        mockMvc.perform(get("/api/v1/treino-do-dia").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ciclo.semanaAtual").value(1));
    }

    /** Diario de treino e' dado da conta, como o peso: nao passa pelo
     * perfil publico nem pelo feed. */
    @Test
    void atividadeNaoApareceNoPerfilPublicoNemNoFeed() throws Exception {
        registrarLivre("{\"notas\": \"segredo do diario\", \"exercicios\": [{\"exercicioId\": %d, \"series\": [10]}]}"
                .formatted(exercicio("Flexao completa").getId()))
                .andExpect(status().isCreated());

        for (String rota : List.of("/api/v1/usuarios/" + usuario.getUsername(),
                "/api/v1/usuarios/" + usuario.getUsername() + "/posts", "/api/v1/feed/descobrir")) {
            mockMvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("segredo do diario"))));
        }
    }

    private ResultActions finalizar() throws Exception {
        return mockMvc.perform(post(FINALIZAR).header(HttpHeaders.AUTHORIZATION, bearer(usuario)));
    }

    private ResultActions registrarLivre(String corpo) throws Exception {
        return mockMvc.perform(post(ATIVIDADES).header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions diario(String cursor) throws Exception {
        var requisicao = get(ATIVIDADES).header(HttpHeaders.AUTHORIZATION, bearer(usuario));
        if (cursor != null) {
            requisicao = requisicao.param("cursor", cursor);
        }
        return mockMvc.perform(requisicao).andExpect(status().isOk());
    }

    private TreinoDoDia treinoDeHoje() {
        return treinoDoDiaRepository.save(new TreinoDoDia(usuario.getId(), LocalDate.now()));
    }

    private TreinoItem item(TreinoDoDia treino, String nomeDoExercicio, int series, int repeticoes, boolean concluido) {
        TreinoItem item = new TreinoItem(treino.getId(), exercicio(nomeDoExercicio).getId(), series, repeticoes);
        item.definirConclusao(concluido);
        return treinoItemRepository.save(item);
    }

    private Exercicio exercicio(String nome) {
        return exercicioRepository.findAll().stream()
                .filter(exercicio -> exercicio.getNome().equals(nome))
                .findFirst().orElseThrow();
    }

    private List<Atividade> atividadesDaConta() {
        return atividadeService.paginaDoUsuario(usuario.getId(), null, 100).itens();
    }
}
