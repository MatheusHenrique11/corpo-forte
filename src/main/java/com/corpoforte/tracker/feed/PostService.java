package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
        return postRepository.save(new Post(usuarioId, texto, LocalDateTime.now()));
    }

    public Comentario comentar(Long postId, Long usuarioId, String texto) {
        exigirPostExistente(postId);
        return comentarioRepository.save(new Comentario(postId, usuarioId, texto, LocalDateTime.now()));
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
     * linha existir ou nao.
     *
     * O catch e' o caso do duplo clique (ou duas abas): as duas
     * requisicoes passam pelo findByPostIdAndUsuarioId sem achar nada e as
     * duas tentam inserir; a segunda esbarra no unique(post_id, usuario_id)
     * e viraria 500 sem tratamento. Aqui nao ha o que fazer alem de
     * ignorar - o estado final desejado ("este usuario curtiu este post")
     * ja e' exatamente o que a outra requisicao acabou de gravar. Mesmo
     * tratamento que UsuarioAtualService da pra corrida no primeiro login
     * (Fase 6).
     */
    public void alternarCurtida(Long postId, Long usuarioId) {
        exigirPostExistente(postId);

        Optional<Curtida> existente = curtidaRepository.findByPostIdAndUsuarioId(postId, usuarioId);
        if (existente.isPresent()) {
            curtidaRepository.delete(existente.get());
            return;
        }

        try {
            curtidaRepository.save(new Curtida(postId, usuarioId, LocalDateTime.now()));
        } catch (DataIntegrityViolationException e) {
            // ja curtido por uma requisicao concorrente - nada a fazer
        }
    }

    /**
     * Junta os posts (de todo mundo, de proposito - ver Post.java) com o
     * nome de cada autor, os comentarios e a contagem de curtidas.
     *
     * Sao 5 consultas de numero fixo, independente de quantos posts ou
     * comentarios existam - nenhuma dentro de laco (mesmo cuidado com N+1
     * que TreinoDoDiaService tem ao juntar TreinoItem com o catalogo):
     * posts, comentarios de todos eles, autores distintos (de post E de
     * comentario, numa busca so'), contagem de curtidas agrupada por post
     * e as curtidas do proprio usuario entre os posts exibidos.
     */
    public List<PostView> listarFeed(Long usuarioIdAtual) {
        List<Post> posts = postRepository.findAllByOrderByCriadoEmDesc();
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        List<Comentario> comentarios = comentarioRepository.findByPostIdInOrderByCriadoEmAsc(postIds);

        Map<Long, String> nomePorUsuarioId = nomesDosAutores(posts, comentarios);
        Map<Long, Long> autorPorPostId = posts.stream()
                .collect(Collectors.toMap(Post::getId, Post::getUsuarioId));

        Map<Long, List<ComentarioView>> comentariosPorPost = comentarios.stream()
                .collect(Collectors.groupingBy(Comentario::getPostId, LinkedHashMap::new,
                        Collectors.mapping(comentario -> new ComentarioView(comentario.getId(),
                                nomePorUsuarioId.get(comentario.getUsuarioId()),
                                comentario.getTexto(), comentario.getCriadoEm(),
                                podeApagarComentario(comentario, autorPorPostId.get(comentario.getPostId()),
                                        usuarioIdAtual)),
                                Collectors.toList())));

        Map<Long, Long> curtidasPorPost = curtidaRepository.contarPorPost(postIds).stream()
                .collect(Collectors.toMap(CurtidaRepository.ContagemPorPost::getPostId,
                        CurtidaRepository.ContagemPorPost::getTotal));

        Set<Long> curtidosPorMim = curtidaRepository.findByUsuarioIdAndPostIdIn(usuarioIdAtual, postIds).stream()
                .map(Curtida::getPostId)
                .collect(Collectors.toSet());

        return posts.stream()
                .map(post -> new PostView(post.getId(), nomePorUsuarioId.get(post.getUsuarioId()),
                        post.getTexto(), post.getCriadoEm(),
                        curtidasPorPost.getOrDefault(post.getId(), 0L),
                        curtidosPorMim.contains(post.getId()),
                        post.getUsuarioId().equals(usuarioIdAtual),
                        comentariosPorPost.getOrDefault(post.getId(), List.of())))
                .toList();
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
