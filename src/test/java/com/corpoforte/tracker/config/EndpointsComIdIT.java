package com.corpoforte.tracker.config;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisica;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaService;
import com.corpoforte.tracker.feed.Comentario;
import com.corpoforte.tracker.feed.ComentarioRepository;
import com.corpoforte.tracker.feed.Post;
import com.corpoforte.tracker.feed.PostRepository;
import com.corpoforte.tracker.feed.PostService;
import com.corpoforte.tracker.feed.PostView;
import com.corpoforte.tracker.treino.TreinoDoDiaService;
import com.corpoforte.tracker.treino.TreinoDoDiaView;
import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoria (Fase 6, revisada depois de uma primeira varredura incompleta)
 * de todo endpoint que recebe ID de recurso vindo do cliente - o vetor
 * classico de IDOR (Insecure Direct Object Reference): um usuario
 * autenticado adivinha/incrementa o ID de outro pra ler ou mexer no
 * recurso alheio.
 *
 * A primeira varredura so procurou @PathVariable, que é o jeito mais
 * óbvio de um ID chegar mas não o mais comum num app com formulário
 * server-side - o clássico é um <input type="hidden" name="exercicioId">
 * num @ModelAttribute, ou um @RequestParam solto. Varredura completa,
 * cobrindo os quatro vetores:
 *
 *   @PathVariable    - grep em todos os controllers
 *   @RequestParam    - grep em todos os controllers: os únicos que existem
 *                       são NivelTreino/MovimentoPadrao/Equipamento em
 *                       ExercicioController (filtro sobre catálogo
 *                       compartilhado, não ID de entidade de outro usuário)
 *                       e Set<Equipamento> em EquipamentoController (grava
 *                       no próprio usuário resolvido pelo login, não lê
 *                       por ID). Nenhum é um ID de recurso. Fase 11: o
 *                       "cursor" das listas paginadas da API carrega uma
 *                       POSICAO (data ou instante + id), nao um recurso -
 *                       toda consulta paginada continua filtrada pelo
 *                       usuario do token (peso, avaliacao) ou e' publica
 *                       (feed, comentarios). Cursor forjado com a posicao
 *                       de outra conta nao mostra nada dela
 *                       (IsolamentoEntreUsuariosIT).
 *   @ModelAttribute  - os 3 forms do app (PerfilForm, AvaliacaoFisicaForm,
 *                       RegistroPesoForm): nenhum campo termina em "Id" -
 *                       só valores (nome, altura, reps, data, peso etc.),
 *                       zero hidden input de ID.
 *   @RequestBody     - zero ocorrências ate a Fase 9 (era tudo
 *                       Thymeleaf server-side). Desde a Fase 10, so' os
 *                       corpos de /api/v1/auth: LoginGoogleRequisicao
 *                       (idToken) e RefreshTokenRequisicao (refreshToken).
 *                       Nenhum e' ID de recurso: sao credenciais, e quem
 *                       as tem ja e' o dono (o refresh token e' um segredo
 *                       aleatorio de 256 bits, nao da pra adivinhar nem
 *                       incrementar). Fase 11: a API reusa os 5 forms da
 *                       tela (PerfilForm, AvaliacaoFisicaForm,
 *                       RegistroPesoForm, PostForm, ComentarioForm) e
 *                       EquipamentosRequisicao - nenhum com campo de ID.
 *
 * Levantamento completo, os endpoints do app (grep por
 * @GetMapping/@PostMapping em todos os controllers):
 *
 *   GET  /                                             sem ID
 *   GET  /perfil                                       sem ID (usuario atual, resolvido pelo login)
 *   POST /perfil                                       sem ID (form sem campo Id)
 *   GET  /avaliacao                                    sem ID
 *   POST /avaliacao                                    sem ID (form sem campo Id)
 *   GET  /peso                                         sem ID
 *   POST /peso                                         sem ID (upsert por data, form sem campo Id)
 *   GET  /equipamentos                                 sem ID
 *   POST /equipamentos                                 sem ID (Set<Equipamento>, nao ID de entidade)
 *   GET  /exercicios                                   sem ID (@RequestParam so' de enum de filtro; catalogo compartilhado, nao e' dado por usuario)
 *   GET  /treino-do-dia                                sem ID
 *   POST /treino-do-dia/itens/{itemId}/concluir        RECEBE ID (@PathVariable)
 *   GET  /feed                                         sem ID (Fase 7a)
 *   POST /feed                                         sem ID (PostForm so' tem texto)
 *   POST /feed/posts/{postId}/comentarios              RECEBE ID (@PathVariable, Fase 7b)
 *   POST /feed/posts/{postId}/curtir                   RECEBE ID (@PathVariable, Fase 7b)
 *   POST /feed/posts/{postId}/apagar                   RECEBE ID (@PathVariable, Fase 7c)
 *   POST /feed/comentarios/{comentarioId}/apagar       RECEBE ID (@PathVariable, Fase 7c)
 *   POST /api/v1/auth/google                           sem ID (Fase 10; corpo com credencial, ver @RequestBody)
 *   POST /api/v1/auth/refresh                          sem ID (Fase 10; idem)
 *   POST /api/v1/auth/logout                           sem ID (Fase 10; idem)
 *   GET  /api/v1/me                                    sem ID (Fase 10; conta dona do access token)
 *   GET  /dev/token-api                                sem ID (Fase 10; so' no perfil dev, usuario da sessao)
 *   GET  /api/v1/perfil, PUT /api/v1/perfil            sem ID (Fase 11)
 *   GET  /api/v1/avaliacoes, POST /api/v1/avaliacoes   sem ID (Fase 11; cursor e' posicao)
 *   GET  /api/v1/avaliacoes/comparacao                 sem ID (Fase 11)
 *   GET  /api/v1/pesos, POST /api/v1/pesos             sem ID (Fase 11; cursor e' posicao)
 *   GET  /api/v1/pesos/tendencia                       sem ID (Fase 11)
 *   GET  /api/v1/equipamentos, PUT /api/v1/equipamentos  sem ID (Fase 11)
 *   GET  /api/v1/exercicios                            sem ID (Fase 11; filtros de enum)
 *   GET  /api/v1/treino-do-dia                         sem ID (Fase 11)
 *   PUT  /api/v1/treino-do-dia/itens/{itemId}/conclusao     RECEBE ID (Fase 11) - proibido cruzado
 *   DELETE /api/v1/treino-do-dia/itens/{itemId}/conclusao  RECEBE ID (Fase 11) - proibido cruzado
 *   GET  /api/v1/feed/descobrir                        sem ID (Fase 11; cursor e' posicao)
 *   POST /api/v1/posts                                 sem ID (Fase 11)
 *   DELETE /api/v1/posts/{postId}                      RECEBE ID (Fase 11) - proibido cruzado
 *   GET  /api/v1/posts/{postId}/comentarios            RECEBE ID (Fase 11) - permitido cruzado
 *   POST /api/v1/posts/{postId}/comentarios            RECEBE ID (Fase 11) - permitido cruzado
 *   DELETE /api/v1/comentarios/{comentarioId}          RECEBE ID (Fase 11) - autor ou dono do post
 *   PUT  /api/v1/posts/{postId}/curtida                RECEBE ID (Fase 11) - permitido cruzado
 *   DELETE /api/v1/posts/{postId}/curtida              RECEBE ID (Fase 11) - permitido cruzado
 *   POST /api/v1/onboarding                            sem ID (Fase 12; OnboardingForm sem campo de ID)
 *   PUT  /api/v1/perfil/publico                        sem ID (Fase 12; so' a propria conta)
 *   GET  /api/v1/usernames/{username}/disponivel       RECEBE username (Fase 12) - so' diz se existe, e isso ja e' publico
 *   GET  /api/v1/usuarios/{username}                   RECEBE username (Fase 12) - permitido cruzado, sem dado corporal
 *   GET  /api/v1/usuarios/{username}/posts             RECEBE username (Fase 12) - permitido cruzado
 *   GET  /api/v1/feed/seguindo                         sem ID (Fase 13; cursor e' posicao)
 *   GET  /api/v1/usuarios?busca=                       sem ID (Fase 13; texto de busca, so' contas com perfil publico)
 *   GET  /api/v1/posts/{postId}                        RECEBE ID (Fase 13) - permitido cruzado
 *   PUT  /api/v1/usuarios/{username}/seguimento        RECEBE username (Fase 13) - permitido cruzado (seguir e' a funcao)
 *   DELETE /api/v1/usuarios/{username}/seguimento      RECEBE username (Fase 13) - so' desfaz o seguir do proprio token
 *   GET  /api/v1/usuarios/{username}/seguidores        RECEBE username (Fase 13) - permitido cruzado
 *   GET  /api/v1/usuarios/{username}/seguindo          RECEBE username (Fase 13) - permitido cruzado
 *
 * Conclusao: 21 dos 57 endpoints aceitam um identificador de recurso
 * vindo do cliente (id numerico ou, desde a Fase 12, username),
 * confirmado nos quatro vetores (não só @PathVariable). Os outros
 * 36 operam exclusivamente sobre "o usuario atual" (Usuario resolvido via
 * UsuarioAtualService.obterUsuarioAtual, pela sessao ou pelo access
 * token) ou sobre enums de filtro sem significado de ID. Os da API
 * repetem as regras dos equivalentes da tela e tem teste proprio abaixo,
 * porque sao outra porta de entrada: uma checagem de dono que so' a
 * rota da tela fizesse passaria despercebida.
 *
 * ATENCAO - os 2 endpoints da Fase 7b mudam a NATUREZA desta auditoria.
 * Ate a Fase 6, "receber ID de recurso de outro usuario" era sempre um
 * bug: o teste de /treino-do-dia/itens/{id} exige 404 justamente porque
 * ninguem tem nada a fazer com o treino alheio. Comentar e curtir sao o
 * primeiro caso em que usar o ID de um recurso de OUTRA pessoa e' a
 * funcao, nao a falha - post e' a entidade intencionalmente compartilhada
 * da Fase 7a (ver Post.java). Entao o que precisa ser garantido aqui e'
 * outro:
 *
 *   1. post inexistente -> 404, nao 500 nem stacktrace vazando;
 *   2. autoria vem do login, nunca do cliente - nao existe campo
 *      "usuarioId" em ComentarioForm nem @RequestParam de autor; quem
 *      assina o comentario/curtida e' sempre o Usuario resolvido pelo
 *      principal.
 *
 * Nao existe teste de "B nao pode curtir post de A" porque B PODE - e o
 * teste abaixo trava esse comportamento de proposito, pra ninguem
 * "corrigir" isso depois achando que e' um vazamento de isolamento.
 *
 * Fase 7c: o MESMO post passa a ter as duas naturezas ao mesmo tempo -
 * qualquer um curte/comenta (permitido), so' o autor apaga (proibido pros
 * outros, 404 igual ID inexistente). Comentario tem dois donos legitimos:
 * quem escreveu e o autor do post (modera o proprio espaco); um terceiro
 * recebe 404. Os tres casos estao travados abaixo.
 *
 * Quando uma fase futura adicionar um endpoint novo que receba ID por
 * qualquer um dos quatro vetores acima (ex.: seguir um usuario, editar
 * um registro de peso especifico), atualizar esta lista e
 * acrescentar o teste correspondente: acesso cruzado proibido no padrao de
 * /treino-do-dia/itens/{id}, ou acesso cruzado permitido no padrao do
 * feed, conforme o caso.
 */
@AutoConfigureMockMvc
@Transactional
class EndpointsComIdIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private AvaliacaoFisicaService avaliacaoFisicaService;

    @Autowired
    private TreinoDoDiaService treinoDoDiaService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private ComentarioRepository comentarioRepository;

    private final OidcUser usuarioA = OidcTestUsers.principal("sub-endpoint-id-a", "Usuaria A", "a@exemplo.com");
    private final OidcUser usuarioB = OidcTestUsers.principal("sub-endpoint-id-b", "Usuario B", "b@exemplo.com");

    /**
     * POST /treino-do-dia/itens/{itemId}/concluir - o unico endpoint com ID
     * da lista acima. Usuario B tenta concluir um item que pertence ao
     * treino do usuario A; precisa de 404 (nao 200/302 de sucesso), e o
     * item de A precisa continuar intacto depois da tentativa.
     */
    @Test
    void concluirItemDeTreinoComIdDeOutroUsuarioDevolve404ENaoAlteraOItem() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        a.atualizarEquipamentos(Set.of(Equipamento.values()));
        usuarioAtualService.salvar(a);
        usuarioAtualService.obterUsuarioAtual(usuarioB);

        AvaliacaoFisica avaliacaoDeA = avaliacaoFisicaService.salvar(a.getId(), 10, 15, 40, 20, 30, 50);
        TreinoDoDiaView treinoDeA = treinoDoDiaService.obterOuGerarDoDia(a, avaliacaoDeA);
        Long itemDeA = treinoDeA.itens().get(0).itemId();

        mockMvc.perform(post("/treino-do-dia/itens/" + itemDeA + "/concluir")
                        .with(oidcLogin().oidcUser(usuarioB)).with(csrf()))
                .andExpect(status().isNotFound());

        TreinoDoDiaView treinoDeAAposTentativa = treinoDoDiaService.obterOuGerarDoDia(a, avaliacaoDeA);
        boolean aindaNaoConcluido = treinoDeAAposTentativa.itens().stream()
                .filter(item -> item.itemId().equals(itemDeA))
                .findFirst().orElseThrow().concluido();
        assertThat(aindaNaoConcluido).isFalse();
    }

    /**
     * Mesmo endpoint, ID que nao existe em lugar nenhum (nem de A nem de
     * B) - tambem tem que ser 404, nao 500/exception vazando detalhe
     * interno.
     */
    @Test
    void concluirItemComIdInexistenteDevolve404() throws Exception {
        usuarioAtualService.obterUsuarioAtual(usuarioA);

        mockMvc.perform(post("/treino-do-dia/itens/999999/concluir")
                        .with(oidcLogin().oidcUser(usuarioA)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    /**
     * O oposto do teste de treino acima, e de proposito: usar o ID do post
     * de OUTRO usuario e' exatamente pra isso que esses dois endpoints
     * existem. Se um dia alguem adicionar checagem de dono aqui "por
     * seguranca", este teste quebra e explica que era intencional.
     */
    @Test
    void curtirEComentarPostDeOutroUsuarioEhPermitidoDeProposito() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        Usuario b = usuarioAtualService.obterUsuarioAtual(usuarioB);
        Post postDeA = postRepository.saveAndFlush(
                new Post(a.getId(), "Fechei o ciclo de 8 semanas", LocalDateTime.now()));

        mockMvc.perform(post("/feed/posts/" + postDeA.getId() + "/curtir")
                        .with(oidcLogin().oidcUser(usuarioB)).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/feed/posts/" + postDeA.getId() + "/comentarios")
                        .with(oidcLogin().oidcUser(usuarioB)).with(csrf())
                        .param("texto", "Parabéns!"))
                .andExpect(status().is3xxRedirection());

        PostView post = postService.listarFeed(a.getId()).get(0);
        assertThat(post.curtidas()).isEqualTo(1);
        // autoria vem do login de B, nao de nada que o cliente mandou
        assertThat(post.comentarios()).singleElement()
                .satisfies(comentario -> assertThat(comentario.autorNome()).isEqualTo(b.getNome()));
    }

    @Test
    void curtirEComentarPostComIdInexistenteDevolve404() throws Exception {
        usuarioAtualService.obterUsuarioAtual(usuarioA);

        mockMvc.perform(post("/feed/posts/999999/curtir")
                        .with(oidcLogin().oidcUser(usuarioA)).with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/feed/posts/999999/comentarios")
                        .with(oidcLogin().oidcUser(usuarioA)).with(csrf())
                        .param("texto", "comentário em post fantasma"))
                .andExpect(status().isNotFound());
    }

    @Test
    void usuarioBNaoApagaPostDoUsuarioA() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        usuarioAtualService.obterUsuarioAtual(usuarioB);
        Post postDeA = postRepository.saveAndFlush(new Post(a.getId(), "Post da A", LocalDateTime.now()));

        mockMvc.perform(post("/feed/posts/" + postDeA.getId() + "/apagar")
                        .with(oidcLogin().oidcUser(usuarioB)).with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(postRepository.existsById(postDeA.getId())).isTrue();
    }

    /**
     * Comentario da A no post da A, e o B tenta apagar: B nao e' autor do
     * comentario nem do post. (O caso permitido - autor do post apagando
     * comentario de outra pessoa - vem no teste seguinte.)
     */
    @Test
    void terceiroNaoApagaComentarioQueNaoEhDeleEmPostQueNaoEhDele() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        usuarioAtualService.obterUsuarioAtual(usuarioB);
        Post postDeA = postRepository.saveAndFlush(new Post(a.getId(), "Post da A", LocalDateTime.now()));
        Comentario comentarioDeA = comentarioRepository.saveAndFlush(
                new Comentario(postDeA.getId(), a.getId(), "Comentário da A", LocalDateTime.now()));

        mockMvc.perform(post("/feed/comentarios/" + comentarioDeA.getId() + "/apagar")
                        .with(oidcLogin().oidcUser(usuarioB)).with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(comentarioRepository.existsById(comentarioDeA.getId())).isTrue();
    }

    /** Acesso cruzado PERMITIDO: A apaga o comentario que B deixou no post
     * da A - o dono do post modera a conversa no proprio espaco. */
    @Test
    void autorDoPostApagaComentarioDeOutroUsuarioDeProposito() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        Usuario b = usuarioAtualService.obterUsuarioAtual(usuarioB);
        Post postDeA = postRepository.saveAndFlush(new Post(a.getId(), "Post da A", LocalDateTime.now()));
        Comentario comentarioDeB = comentarioRepository.saveAndFlush(
                new Comentario(postDeA.getId(), b.getId(), "Comentário do B", LocalDateTime.now()));

        mockMvc.perform(post("/feed/comentarios/" + comentarioDeB.getId() + "/apagar")
                        .with(oidcLogin().oidcUser(usuarioA)).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(comentarioRepository.existsById(comentarioDeB.getId())).isFalse();
    }

    @Test
    void apagarPostOuComentarioComIdInexistenteDevolve404() throws Exception {
        usuarioAtualService.obterUsuarioAtual(usuarioA);

        mockMvc.perform(post("/feed/posts/999999/apagar")
                        .with(oidcLogin().oidcUser(usuarioA)).with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/feed/comentarios/999999/apagar")
                        .with(oidcLogin().oidcUser(usuarioA)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ---- API (Fase 11): as mesmas regras, pela outra porta de entrada ----

    @Test
    void apiMarcarOuDesmarcarItemDeTreinoDeOutroUsuarioDevolve404() throws Exception {
        Usuario a = contaComOnboarding(usuarioA);
        Usuario b = contaComOnboarding(usuarioB);
        AvaliacaoFisica avaliacaoDeA = avaliacaoFisicaService.salvar(a.getId(), 10, 15, 40, 20, 30, 50);
        Long itemDeA = treinoDoDiaService.obterOuGerarDoDia(a, avaliacaoDeA).itens().get(0).itemId();
        String rota = "/api/v1/treino-do-dia/itens/" + itemDeA + "/conclusao";

        mockMvc.perform(put(rota).header(HttpHeaders.AUTHORIZATION, bearer(b))).andExpect(status().isNotFound());
        mockMvc.perform(delete(rota).header(HttpHeaders.AUTHORIZATION, bearer(b))).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/treino-do-dia/itens/999999/conclusao").header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNotFound());

        assertThat(treinoDoDiaService.obterOuGerarDoDia(a, avaliacaoDeA).itens().get(0).concluido()).isFalse();
    }

    @Test
    void apiCurtirComentarELerComentariosDePostDeOutroUsuarioEhPermitidoDeProposito() throws Exception {
        Usuario a = contaComOnboarding(usuarioA);
        Usuario b = contaComOnboarding(usuarioB);
        Post postDeA = postRepository.saveAndFlush(new Post(a.getId(), "Post da A", LocalDateTime.now()));
        String rota = "/api/v1/posts/" + postDeA.getId();

        mockMvc.perform(put(rota + "/curtida").header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post(rota + "/comentarios").header(HttpHeaders.AUTHORIZATION, bearer(b))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\": \"Parabéns!\"}"))
                .andExpect(status().isCreated())
                // autoria vem do token de B, nao de nada que o cliente mandou
                .andExpect(jsonPath("$.autor.id").value(b.getId()));
        mockMvc.perform(get(rota + "/comentarios").header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].texto").value("Parabéns!"));
        mockMvc.perform(delete(rota + "/curtida").header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isNoContent());
    }

    @Test
    void apiPostInexistenteDevolve404EmTodaRotaComPostId() throws Exception {
        Usuario a = contaComOnboarding(usuarioA);
        String rota = "/api/v1/posts/999999";

        mockMvc.perform(put(rota + "/curtida").header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(rota + "/curtida").header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(rota + "/comentarios").header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(rota + "/comentarios").header(HttpHeaders.AUTHORIZATION, bearer(a))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\": \"fantasma\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(rota).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/comentarios/999999").header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNotFound());
    }

    @Test
    void apiUsuarioBNaoApagaPostNemComentarioDaA() throws Exception {
        Usuario a = contaComOnboarding(usuarioA);
        Usuario b = contaComOnboarding(usuarioB);
        Post postDeA = postRepository.saveAndFlush(new Post(a.getId(), "Post da A", LocalDateTime.now()));
        Comentario comentarioDeA = comentarioRepository.saveAndFlush(
                new Comentario(postDeA.getId(), a.getId(), "Comentário da A", LocalDateTime.now()));

        mockMvc.perform(delete("/api/v1/posts/" + postDeA.getId()).header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/comentarios/" + comentarioDeA.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isNotFound());

        assertThat(postRepository.existsById(postDeA.getId())).isTrue();
        assertThat(comentarioRepository.existsById(comentarioDeA.getId())).isTrue();
    }

    /** Acesso cruzado PERMITIDO pela API tambem: dono do post modera. */
    @Test
    void apiAutorDoPostApagaComentarioDeOutroUsuarioDeProposito() throws Exception {
        Usuario a = contaComOnboarding(usuarioA);
        Usuario b = contaComOnboarding(usuarioB);
        Post postDeA = postRepository.saveAndFlush(new Post(a.getId(), "Post da A", LocalDateTime.now()));
        Comentario comentarioDeB = comentarioRepository.saveAndFlush(
                new Comentario(postDeA.getId(), b.getId(), "Comentário do B", LocalDateTime.now()));

        mockMvc.perform(delete("/api/v1/comentarios/" + comentarioDeB.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNoContent());

        assertThat(comentarioRepository.existsById(comentarioDeB.getId())).isFalse();
    }

    // ---- Perfil publico (Fase 12): ler outra conta pelo username e' a funcao ----

    /**
     * Acesso cruzado PERMITIDO: B le o perfil publico e os posts de A. O que
     * precisa ser garantido e' outra coisa: nada de dado corporal de A
     * passa por ai (o conteudo completo e' conferido em PerfilPublicoApiIT).
     */
    @Test
    void apiPerfilPublicoDeOutroUsuarioEhPermitidoDeProposito() throws Exception {
        Usuario a = contaComOnboarding(usuarioA);
        Usuario b = contaComOnboarding(usuarioB);
        postRepository.saveAndFlush(new Post(a.getId(), "Post da A", LocalDateTime.now()));

        mockMvc.perform(get("/api/v1/usuarios/" + a.getUsername()).header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId()))
                .andExpect(jsonPath("$.pesoKg").doesNotExist());
        mockMvc.perform(get("/api/v1/usuarios/" + a.getUsername() + "/posts").header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].texto").value("Post da A"));
    }

    // ---- Seguir e pagina do post (Fase 13) ----

    /**
     * Acesso cruzado PERMITIDO: seguir a conta de outra pessoa e' a funcao.
     * O que precisa ser garantido e' que o seguidor e' sempre quem esta no
     * token - B segue A, e A continua sem seguir ninguem.
     */
    @Test
    void apiSeguirOutraContaEhPermitidoDeProposito() throws Exception {
        Usuario a = contaComOnboarding(usuarioA);
        Usuario b = contaComOnboarding(usuarioB);

        mockMvc.perform(put("/api/v1/usuarios/" + a.getUsername() + "/seguimento")
                        .header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/usuarios/" + a.getUsername() + "/seguidores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(jsonPath("$.itens[0].id").value(b.getId()));
        mockMvc.perform(get("/api/v1/usuarios/" + a.getUsername() + "/seguindo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(jsonPath("$.itens").isEmpty());
    }

    @Test
    void apiPaginaDoPostDeOutroUsuarioEhPermitidaEInexistenteDa404() throws Exception {
        Usuario a = contaComOnboarding(usuarioA);
        Usuario b = contaComOnboarding(usuarioB);
        Post postDeA = postRepository.saveAndFlush(new Post(a.getId(), "Post da A", LocalDateTime.now()));

        mockMvc.perform(get("/api/v1/posts/" + postDeA.getId()).header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.podeApagar").value(false));
        mockMvc.perform(get("/api/v1/posts/999999").header(HttpHeaders.AUTHORIZATION, bearer(b)))
                .andExpect(status().isNotFound());
    }
}
