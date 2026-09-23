package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persistencia do feed: mesmo papel que os outros XService tem pras suas
 * entidades. O Controller nunca chama os repositories direto.
 */
@Service
public class PostService {

    /** Quantos comentarios cada post traz no feed paginado; o resto vem de
     * paginaDeComentarios. */
    static final int COMENTARIOS_RECENTES_POR_POST = 3;

    private final PostRepository postRepository;
    private final ComentarioRepository comentarioRepository;
    private final CurtidaRepository curtidaRepository;
    private final UsuarioRepository usuarioRepository;

    public PostService(PostRepository postRepository, ComentarioRepository comentarioRepository,
                        CurtidaRepository curtidaRepository, UsuarioRepository usuarioRepository) {
        this.postRepository = postRepository;
        this.comentarioRepository = comentarioRepository;
        this.curtidaRepository = curtidaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Post criar(Long usuarioId, String texto) {
        return postRepository.save(new Post(usuarioId, texto, agora()));
    }

    public Comentario comentar(Long postId, Long usuarioId, String texto) {
        exigirPostExistente(postId);
        return comentarioRepository.save(new Comentario(postId, usuarioId, texto, agora()));
    }

    /**
     * O Postgres guarda timestamp com microssegundos, e o relogio do Java
     * tem nanossegundos. Sem truncar, a resposta de quem acabou de postar
     * (valor em memoria) mostraria um criadoEm diferente do mesmo post lido
     * do feed depois (valor do banco) - e o horario e' tambem a posicao do
     * cursor.
     */
    private static LocalDateTime agora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * So' o autor apaga o proprio post. Comentarios e curtidas saem junto
     * pelo "on delete cascade" do banco (V11), nao por delete em laco aqui.
     *
     * Post alheio e post inexistente dao o MESMO 404 (nao 403 pro alheio):
     * o mesmo post que qualquer um pode curtir/comentar (Fase 7b) nao pode
     * ser apagado por outra pessoa, e a resposta nao deve confirmar nada
     * alem disso - mesmo padrao de TreinoDoDiaService.alternarConclusao.
     */
    public void apagarPost(Long postId, Long usuarioId) {
        Post post = postRepository.findById(postId)
                .filter(encontrado -> encontrado.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        postRepository.delete(post);
    }

    /**
     * Apaga quem escreveu o comentario OU quem e' dono do post - o autor do
     * post modera a conversa no proprio espaco (Facebook/Instagram fazem o
     * mesmo). Qualquer outra pessoa recebe 404, igual comentario
     * inexistente.
     */
    public void apagarComentario(Long comentarioId, Long usuarioId) {
        Comentario comentario = comentarioRepository.findById(comentarioId)
                .filter(encontrado -> postRepository.findById(encontrado.getPostId())
                        .map(post -> podeApagarComentario(encontrado, post.getUsuarioId(), usuarioId))
                        .orElse(false))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        comentarioRepository.delete(comentario);
    }

    /**
     * Regra unica de "quem pode apagar este comentario", usada tanto pra
     * autorizar (apagarComentario) quanto pra decidir se o botao aparece
     * (listarFeed) - duas copias dessa regra divergiriam na primeira
     * mudanca (ex.: um novo papel que tambem possa moderar comentarios).
     */
    private static boolean podeApagarComentario(Comentario comentario, Long autorDoPostId, Long usuarioId) {
        return comentario.getUsuarioId().equals(usuarioId) || autorDoPostId.equals(usuarioId);
    }

    /**
     * Curtir de novo descurte: o toggle nao tem estado proprio, e' so' a
     * linha existir ou nao. E' o botao da tela; a API usa curtir/descurtir.
     */
    public void alternarCurtida(Long postId, Long usuarioId) {
        exigirPostExistente(postId);

        Optional<Curtida> existente = curtidaRepository.findByPostIdAndUsuarioId(postId, usuarioId);
        if (existente.isPresent()) {
            curtidaRepository.delete(existente.get());
            return;
        }

        gravarCurtida(postId, usuarioId);
    }

    /**
     * Versao idempotente pra API (PUT): curtir o que ja esta curtido nao
     * muda nada. Com o toggle, um cliente que repete a requisicao depois de
     * uma falha de rede desfaria a curtida.
     */
    public void curtir(Long postId, Long usuarioId) {
        exigirPostExistente(postId);

        if (curtidaRepository.findByPostIdAndUsuarioId(postId, usuarioId).isEmpty()) {
            gravarCurtida(postId, usuarioId);
        }
    }

    /** Idempotente (DELETE): descurtir o que nao esta curtido nao e' erro. */
    public void descurtir(Long postId, Long usuarioId) {
        exigirPostExistente(postId);

        curtidaRepository.findByPostIdAndUsuarioId(postId, usuarioId).ifPresent(curtidaRepository::delete);
    }

    /**
     * O catch e' o caso do duplo clique (ou duas abas): as duas
     * requisicoes passam pelo findByPostIdAndUsuarioId sem achar nada e as
     * duas tentam inserir; a segunda esbarra no unique(post_id, usuario_id)
     * e viraria 500 sem tratamento. Aqui nao ha o que fazer alem de
     * ignorar - o estado final desejado ("este usuario curtiu este post")
     * ja e' exatamente o que a outra requisicao acabou de gravar. Mesmo
     * tratamento que UsuarioAtualService da pra corrida no primeiro login
     * (Fase 6).
     */
    private void gravarCurtida(Long postId, Long usuarioId) {
        try {
            curtidaRepository.save(new Curtida(postId, usuarioId, LocalDateTime.now()));
        } catch (DataIntegrityViolationException e) {
            // ja curtido por uma requisicao concorrente - nada a fazer
        }
    }

    /**
     * Feed da tela: todos os posts (de todo mundo, de proposito - ver
     * Post.java), cada um com todos os comentarios.
     */
    public List<PostView> listarFeed(Long usuarioIdAtual) {
        List<Post> posts = postRepository.findAllByOrderByCriadoEmDesc();
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Comentario> comentarios = comentarioRepository.findByPostIdInOrderByCriadoEmAsc(idsDe(posts));
        Map<Long, Long> totalDeComentarios = comentarios.stream()
                .collect(Collectors.groupingBy(Comentario::getPostId, Collectors.counting()));

        return montar(posts, comentarios, totalDeComentarios, usuarioIdAtual);
    }

    /**
     * Feed da API: paginado por cursor (keyset), e cada post traz so' os
     * ultimos comentarios mais o total. Um post com centenas de comentarios
     * nao pode inflar cada pagina do feed; o resto vem de
     * paginaDeComentarios.
     */
    public Pagina<PostView> paginaDoFeed(Long usuarioIdAtual, Cursor cursor, int tamanho) {
        Limit limite = Limit.of(tamanho + 1);
        List<Post> buscados = cursor == null
                ? postRepository.buscarMaisRecentes(limite)
                : postRepository.buscarAnterioresA(cursor.comoInstante(), cursor.id(), limite);

        return Pagina.deBuscaComUmAMais(buscados, tamanho, post -> Cursor.apos(post.getCriadoEm(), post.getId()))
                .mapearTodos(posts -> montarComComentariosRecentes(posts, usuarioIdAtual));
    }

    /** Um post no mesmo formato do feed paginado (resposta de quem acabou
     * de criar). */
    public PostView visaoDoPost(Post post, Long usuarioIdAtual) {
        return montarComComentariosRecentes(List.of(post), usuarioIdAtual).get(0);
    }

    /** Comentarios de um post, do mais antigo pro mais novo, paginados. 404
     * pra post inexistente, igual comentar e curtir. */
    public Pagina<ComentarioView> paginaDeComentarios(Long postId, Long usuarioIdAtual, Cursor cursor, int tamanho) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Limit limite = Limit.of(tamanho + 1);
        List<Comentario> buscados = cursor == null
                ? comentarioRepository.buscarDoPost(postId, limite)
                : comentarioRepository.buscarDoPostApos(postId, cursor.comoInstante(), cursor.id(), limite);

        return Pagina.deBuscaComUmAMais(buscados, tamanho,
                        comentario -> Cursor.apos(comentario.getCriadoEm(), comentario.getId()))
                .mapearTodos(comentarios -> {
                    Map<Long, String> nomes = nomesDosAutores(List.of(), comentarios);
                    return comentarios.stream()
                            .map(comentario -> paraView(comentario, nomes, post.getUsuarioId(), usuarioIdAtual))
                            .toList();
                });
    }

    /** Um comentario no formato da listagem (resposta de quem acabou de
     * comentar), com podeApagar pela mesma regra de sempre. */
    public ComentarioView visaoDoComentario(Comentario comentario, Long usuarioIdAtual) {
        Long autorDoPostId = postRepository.findById(comentario.getPostId())
                .map(Post::getUsuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return paraView(comentario, nomesDosAutores(List.of(), List.of(comentario)), autorDoPostId, usuarioIdAtual);
    }

    private List<PostView> montarComComentariosRecentes(List<Post> posts, Long usuarioIdAtual) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = idsDe(posts);
        List<Comentario> recentes = comentarioRepository.buscarRecentesPorPost(postIds, COMENTARIOS_RECENTES_POR_POST);
        Map<Long, Long> totalDeComentarios = comentarioRepository.contarPorPost(postIds).stream()
                .collect(Collectors.toMap(ContagemPorPost::getPostId, ContagemPorPost::getTotal));

        return montar(posts, recentes, totalDeComentarios, usuarioIdAtual);
    }

    /**
     * Junta os posts com o nome de cada autor, os comentarios recebidos e a
     * contagem de curtidas - usado pelo feed da tela e pelo da API, que so'
     * diferem em quais posts e quais comentarios entram.
     *
     * Numero fixo de consultas, independente de quantos posts ou
     * comentarios existam - nenhuma dentro de laco (mesmo cuidado com N+1
     * que TreinoDoDiaService tem ao juntar TreinoItem com o catalogo):
     * autores distintos (de post E de comentario, numa busca so'),
     * contagem de curtidas agrupada por post e as curtidas do proprio
     * usuario entre os posts exibidos.
     */
    private List<PostView> montar(List<Post> posts, List<Comentario> comentarios,
                                  Map<Long, Long> totalDeComentarios, Long usuarioIdAtual) {
        List<Long> postIds = idsDe(posts);
        Map<Long, String> nomePorUsuarioId = nomesDosAutores(posts, comentarios);
        Map<Long, Long> autorPorPostId = posts.stream()
                .collect(Collectors.toMap(Post::getId, Post::getUsuarioId));

        Map<Long, List<ComentarioView>> comentariosPorPost = comentarios.stream()
                .collect(Collectors.groupingBy(Comentario::getPostId, LinkedHashMap::new,
                        Collectors.mapping(comentario -> paraView(comentario, nomePorUsuarioId,
                                        autorPorPostId.get(comentario.getPostId()), usuarioIdAtual),
                                Collectors.toList())));

        Map<Long, Long> curtidasPorPost = curtidaRepository.contarPorPost(postIds).stream()
                .collect(Collectors.toMap(ContagemPorPost::getPostId, ContagemPorPost::getTotal));

        Set<Long> curtidosPorMim = curtidaRepository.findByUsuarioIdAndPostIdIn(usuarioIdAtual, postIds).stream()
                .map(Curtida::getPostId)
                .collect(Collectors.toSet());

        return posts.stream()
                .map(post -> new PostView(post.getId(), post.getUsuarioId(), nomePorUsuarioId.get(post.getUsuarioId()),
                        post.getTexto(), post.getCriadoEm(),
                        curtidasPorPost.getOrDefault(post.getId(), 0L),
                        curtidosPorMim.contains(post.getId()),
                        post.getUsuarioId().equals(usuarioIdAtual),
                        totalDeComentarios.getOrDefault(post.getId(), 0L),
                        comentariosPorPost.getOrDefault(post.getId(), List.of())))
                .toList();
    }

    private static ComentarioView paraView(Comentario comentario, Map<Long, String> nomePorUsuarioId,
                                           Long autorDoPostId, Long usuarioIdAtual) {
        return new ComentarioView(comentario.getId(), comentario.getUsuarioId(),
                nomePorUsuarioId.get(comentario.getUsuarioId()), comentario.getTexto(), comentario.getCriadoEm(),
                podeApagarComentario(comentario, autorDoPostId, usuarioIdAtual));
    }

    private static List<Long> idsDe(List<Post> posts) {
        return posts.stream().map(Post::getId).toList();
    }

    /** Autor de post e autor de comentario saem da mesma busca: quem
     * comentou no feed quase sempre tambem aparece como autor de algum
     * post, e duas buscas separadas trariam as mesmas linhas duas vezes. */
    private Map<Long, String> nomesDosAutores(List<Post> posts, List<Comentario> comentarios) {
        Set<Long> autorIds = new HashSet<>();
        posts.forEach(post -> autorIds.add(post.getUsuarioId()));
        comentarios.forEach(comentario -> autorIds.add(comentario.getUsuarioId()));

        return usuarioRepository.findAllById(autorIds).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNome));
    }

    /**
     * 404 (nao 400/500) pra post inexistente, igual
     * TreinoDoDiaService.alternarConclusao: postId vem do cliente, e a
     * resposta pra um ID que nao existe nao deve vazar detalhe interno.
     * Aqui NAO se checa dono do post - comentar e curtir post dos outros e'
     * a funcao, ver EndpointsComIdIT.
     */
    private void exigirPostExistente(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
