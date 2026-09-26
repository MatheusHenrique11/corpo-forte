package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.arquivos.ArmazenamentoArquivos;
import com.corpoforte.tracker.arquivos.ImagemProcessada;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.usuario.FotoDePerfil;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import com.corpoforte.tracker.usuario.Visibilidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final ComentarioRepository comentarioRepository;
    private final CurtidaRepository curtidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final FotoPostRepository fotoPostRepository;
    private final ArmazenamentoArquivos armazenamento;
    private final FotoDePerfil fotoDePerfil;

    public PostService(PostRepository postRepository, ComentarioRepository comentarioRepository,
                        CurtidaRepository curtidaRepository, UsuarioRepository usuarioRepository,
                        FotoPostRepository fotoPostRepository, ArmazenamentoArquivos armazenamento,
                        FotoDePerfil fotoDePerfil) {
        this.postRepository = postRepository;
        this.comentarioRepository = comentarioRepository;
        this.curtidaRepository = curtidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.fotoPostRepository = fotoPostRepository;
        this.armazenamento = armazenamento;
        this.fotoDePerfil = fotoDePerfil;
    }

    /** Sem visibilidade escolhida, vale o padrao da conta (Fase 14). */
    public Post criar(Usuario autor, String texto, Visibilidade escolhida) {
        Visibilidade visibilidade = escolhida != null ? escolhida : autor.getVisibilidadePadrao();
        return postRepository.save(new Post(autor.getId(), texto, agora(), visibilidade));
    }

    /**
     * Post com fotos ja processadas (ProcessadorDeImagem: validadas,
     * re-codificadas, sem EXIF). Nome de arquivo e' UUID aleatorio, nunca
     * algo vindo do cliente. Se qualquer gravacao falhar, os arquivos ja
     * gravados sao apagados e a transacao desfaz post e linhas de foto: nao
     * sobra post pela metade nem arquivo sem dono.
     */
    @Transactional
    public Post criarComFotos(Usuario autor, String texto, Visibilidade escolhida, List<ImagemProcessada> fotos) {
        Post post = criar(autor, texto, escolhida);
        List<String> gravadas = new ArrayList<>();
        try {
            for (int posicao = 0; posicao < fotos.size(); posicao++) {
                ImagemProcessada foto = fotos.get(posicao);
                String base = "posts/" + UUID.randomUUID();
                String chave = base + ".jpg";
                String chaveMiniatura = base + "_mini.jpg";
                armazenamento.gravar(chave, foto.principal());
                gravadas.add(chave);
                armazenamento.gravar(chaveMiniatura, foto.miniatura());
                gravadas.add(chaveMiniatura);
                fotoPostRepository.save(new FotoPost(post.getId(), posicao, chave, chaveMiniatura,
                        foto.largura(), foto.altura(), agora()));
            }
        } catch (RuntimeException e) {
            gravadas.forEach(this::apagarArquivoSemFalhar);
            throw e;
        }
        return post;
    }

    /** So' em post que quem comenta consegue ver - post invisivel da o
     * mesmo 404 de inexistente (inclui bloqueio com o autor). */
    public Comentario comentar(Long postId, Long usuarioId, String texto) {
        exigirPostVisivel(postId, usuarioId);
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

        // as linhas de foto saem pelo cascade; os arquivos, daqui - depois do
        // post ja apagado, pra uma falha no banco nao levar as fotos junto
        List<FotoPost> fotos = fotoPostRepository.findByPostId(post.getId());
        postRepository.delete(post);
        fotos.forEach(foto -> {
            apagarArquivoSemFalhar(foto.getChave());
            apagarArquivoSemFalhar(foto.getChaveMiniatura());
        });
    }

    /** O post ja foi apagado: um arquivo que falhe em sair vira sobra no
     * disco (logada), nao um erro pra quem pediu pra apagar. */
    private void apagarArquivoSemFalhar(String chave) {
        try {
            armazenamento.apagar(chave);
        } catch (RuntimeException e) {
            log.warn("Nao foi possivel apagar o arquivo {}", chave, e);
        }
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
        exigirPostVisivel(postId, usuarioId);

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
        exigirPostVisivel(postId, usuarioId);

        if (curtidaRepository.findByPostIdAndUsuarioId(postId, usuarioId).isEmpty()) {
            gravarCurtida(postId, usuarioId);
        }
    }

    /** Idempotente (DELETE): descurtir o que nao esta curtido nao e' erro. */
    public void descurtir(Long postId, Long usuarioId) {
        exigirPostVisivel(postId, usuarioId);

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
     * Feed da tela: todos os posts que quem ve pode ver (de todo mundo, de
     * proposito - ver Post.java - menos o que a visibilidade e o bloqueio
     * escondem), cada um com todos os comentarios. A tela e' outra porta de
     * entrada pro mesmo dado, entao usa a mesma regra da API.
     */
    public List<PostView> listarFeed(Long usuarioIdAtual) {
        List<Post> posts = postRepository.buscarTodosVisiveis(usuarioIdAtual);
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Comentario> comentarios = comentarioRepository.buscarDosPosts(idsDe(posts), usuarioIdAtual);
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
                ? postRepository.buscarMaisRecentes(usuarioIdAtual, limite)
                : postRepository.buscarAnterioresA(usuarioIdAtual, cursor.comoInstante(), cursor.id(), limite);

        return Pagina.deBuscaComUmAMais(buscados, tamanho, post -> Cursor.apos(post.getCriadoEm(), post.getId()))
                .mapearTodos(posts -> montarComComentariosRecentes(posts, usuarioIdAtual));
    }

    /** Feed "Seguindo": os posts do proprio usuario e de quem ele segue. */
    public Pagina<PostView> paginaSeguindo(Long usuarioIdAtual, Cursor cursor, int tamanho) {
        Limit limite = Limit.of(tamanho + 1);
        List<Post> buscados = cursor == null
                ? postRepository.buscarSeguindo(usuarioIdAtual, limite)
                : postRepository.buscarSeguindoAnterioresA(usuarioIdAtual, cursor.comoInstante(), cursor.id(), limite);

        return Pagina.deBuscaComUmAMais(buscados, tamanho, post -> Cursor.apos(post.getCriadoEm(), post.getId()))
                .mapearTodos(posts -> montarComComentariosRecentes(posts, usuarioIdAtual));
    }

    /** Pagina de um post (pra onde um link ou aviso aponta): o mesmo formato
     * do feed; o resto dos comentarios vem de paginaDeComentarios. */
    public PostView visaoDoPost(Long postId, Long usuarioIdAtual) {
        return visaoDoPost(postVisivel(postId, usuarioIdAtual), usuarioIdAtual);
    }

    /** Posts de um autor, no mesmo formato e com o mesmo cursor do feed
     * (perfil publico). */
    public Pagina<PostView> paginaDoAutor(Long autorId, Long usuarioIdAtual, Cursor cursor, int tamanho) {
        Limit limite = Limit.of(tamanho + 1);
        List<Post> buscados = cursor == null
                ? postRepository.buscarMaisRecentesDoAutor(autorId, usuarioIdAtual, limite)
                : postRepository.buscarDoAutorAnterioresA(autorId, usuarioIdAtual, cursor.comoInstante(), cursor.id(),
                        limite);

        return Pagina.deBuscaComUmAMais(buscados, tamanho, post -> Cursor.apos(post.getCriadoEm(), post.getId()))
                .mapearTodos(posts -> montarComComentariosRecentes(posts, usuarioIdAtual));
    }

    /** Quantos posts do autor quem ve consegue ver. */
    public long contarVisiveisDoAutor(Long autorId, Long usuarioIdAtual) {
        return postRepository.contarVisiveisDoAutor(autorId, usuarioIdAtual);
    }

    /** Um post no mesmo formato do feed paginado (resposta de quem acabou
     * de criar). */
    public PostView visaoDoPost(Post post, Long usuarioIdAtual) {
        return montarComComentariosRecentes(List.of(post), usuarioIdAtual).get(0);
    }

    /** Comentarios de um post, do mais antigo pro mais novo, paginados. 404
     * pra post inexistente, igual comentar e curtir. */
    public Pagina<ComentarioView> paginaDeComentarios(Long postId, Long usuarioIdAtual, Cursor cursor, int tamanho) {
        Post post = postVisivel(postId, usuarioIdAtual);

        Limit limite = Limit.of(tamanho + 1);
        List<Comentario> buscados = cursor == null
                ? comentarioRepository.buscarDoPost(postId, usuarioIdAtual, limite)
                : comentarioRepository.buscarDoPostApos(postId, usuarioIdAtual, cursor.comoInstante(), cursor.id(),
                        limite);

        return Pagina.deBuscaComUmAMais(buscados, tamanho,
                        comentario -> Cursor.apos(comentario.getCriadoEm(), comentario.getId()))
                .mapearTodos(comentarios -> {
                    Map<Long, AutorView> autores = autoresDe(List.of(), comentarios);
                    return comentarios.stream()
                            .map(comentario -> paraView(comentario, autores, post.getUsuarioId(), usuarioIdAtual))
                            .toList();
                });
    }

    /** Um comentario no formato da listagem (resposta de quem acabou de
     * comentar), com podeApagar pela mesma regra de sempre. */
    public ComentarioView visaoDoComentario(Comentario comentario, Long usuarioIdAtual) {
        Long autorDoPostId = postRepository.findById(comentario.getPostId())
                .map(Post::getUsuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return paraView(comentario, autoresDe(List.of(), List.of(comentario)), autorDoPostId, usuarioIdAtual);
    }

    private List<PostView> montarComComentariosRecentes(List<Post> posts, Long usuarioIdAtual) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = idsDe(posts);
        List<Comentario> recentes = comentarioRepository.buscarRecentesPorPost(postIds, COMENTARIOS_RECENTES_POR_POST,
                usuarioIdAtual);
        Map<Long, Long> totalDeComentarios = comentarioRepository.contarPorPost(postIds, usuarioIdAtual).stream()
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
        Map<Long, AutorView> autores = autoresDe(posts, comentarios);
        Map<Long, Long> autorPorPostId = posts.stream()
                .collect(Collectors.toMap(Post::getId, Post::getUsuarioId));

        Map<Long, List<ComentarioView>> comentariosPorPost = comentarios.stream()
                .collect(Collectors.groupingBy(Comentario::getPostId, LinkedHashMap::new,
                        Collectors.mapping(comentario -> paraView(comentario, autores,
                                        autorPorPostId.get(comentario.getPostId()), usuarioIdAtual),
                                Collectors.toList())));

        Map<Long, Long> curtidasPorPost = curtidaRepository.contarPorPost(postIds).stream()
                .collect(Collectors.toMap(ContagemPorPost::getPostId, ContagemPorPost::getTotal));

        Set<Long> curtidosPorMim = curtidaRepository.findByUsuarioIdAndPostIdIn(usuarioIdAtual, postIds).stream()
                .map(Curtida::getPostId)
                .collect(Collectors.toSet());

        // URLs assinadas aqui: so' chegam a este metodo posts que ja passaram
        // pela RegraDeVisibilidade pra quem esta vendo
        Map<Long, List<FotoView>> fotosPorPost = fotoPostRepository.findByPostIdInOrderByPostIdAscPosicaoAsc(postIds)
                .stream()
                .collect(Collectors.groupingBy(FotoPost::getPostId, LinkedHashMap::new,
                        Collectors.mapping(foto -> new FotoView(armazenamento.urlAssinada(foto.getChave()),
                                        armazenamento.urlAssinada(foto.getChaveMiniatura()),
                                        foto.getLargura(), foto.getAltura()),
                                Collectors.toList())));

        return posts.stream()
                .map(post -> new PostView(post.getId(), autores.get(post.getUsuarioId()),
                        post.getTexto(), post.getCriadoEm(), post.getVisibilidade(),
                        curtidasPorPost.getOrDefault(post.getId(), 0L),
                        curtidosPorMim.contains(post.getId()),
                        post.getUsuarioId().equals(usuarioIdAtual),
                        totalDeComentarios.getOrDefault(post.getId(), 0L),
                        comentariosPorPost.getOrDefault(post.getId(), List.of()),
                        fotosPorPost.getOrDefault(post.getId(), List.of())))
                .toList();
    }

    private static ComentarioView paraView(Comentario comentario, Map<Long, AutorView> autores,
                                           Long autorDoPostId, Long usuarioIdAtual) {
        return new ComentarioView(comentario.getId(), autores.get(comentario.getUsuarioId()),
                comentario.getTexto(), comentario.getCriadoEm(),
                podeApagarComentario(comentario, autorDoPostId, usuarioIdAtual));
    }

    private static List<Long> idsDe(List<Post> posts) {
        return posts.stream().map(Post::getId).toList();
    }

    /** Autor de post e autor de comentario saem da mesma busca: quem
     * comentou no feed quase sempre tambem aparece como autor de algum
     * post, e duas buscas separadas trariam as mesmas linhas duas vezes. */
    private Map<Long, AutorView> autoresDe(List<Post> posts, List<Comentario> comentarios) {
        Set<Long> autorIds = new HashSet<>();
        posts.forEach(post -> autorIds.add(post.getUsuarioId()));
        comentarios.forEach(comentario -> autorIds.add(comentario.getUsuarioId()));

        return usuarioRepository.findAllById(autorIds).stream()
                .collect(Collectors.toMap(Usuario::getId, usuario -> AutorView.de(usuario, fotoDePerfil.url(usuario))));
    }

    /**
     * 404 (nao 400/500) pra post inexistente OU que quem pede nao pode ver
     * (RegraDeVisibilidade, pela mesma consulta das listagens): a resposta
     * nao confirma que o post existe. Aqui NAO se checa dono do post -
     * comentar e curtir post dos outros e' a funcao, ver EndpointsComIdIT.
     */
    private Post postVisivel(Long postId, Long usuarioId) {
        return postRepository.buscarVisivel(postId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void exigirPostVisivel(Long postId, Long usuarioId) {
        postVisivel(postId, usuarioId);
    }
}
