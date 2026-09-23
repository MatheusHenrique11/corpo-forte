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
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 *                       por ID). Nenhum é um ID de recurso.
 *   @ModelAttribute  - os 3 forms do app (PerfilForm, AvaliacaoFisicaForm,
 *                       RegistroPesoForm): nenhum campo termina em "Id" -
 *                       só valores (nome, altura, reps, data, peso etc.),
 *                       zero hidden input de ID.
 *   @RequestBody     - zero ocorrências no projeto inteiro (é tudo
 *                       Thymeleaf server-side, nenhuma API JSON).
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
 *
 * Conclusao: 5 dos 18 endpoints aceitam um ID de recurso vindo do cliente,
 * confirmado nos quatro vetores (não só @PathVariable). Os outros 13
 * operam exclusivamente sobre "o usuario atual" (Usuario resolvido via
 * UsuarioAtualService.obterUsuarioAtual(principal)) ou sobre enums de
 * filtro sem significado de ID.
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
}
