package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feed, posts, comentarios e curtidas. Autoria vem sempre do access
 * token: nenhum corpo aceita id de usuario.
 *
 * Acesso a recurso de outra pessoa por id, nos dois sentidos (auditado em
 * EndpointsComIdIT): comentar e curtir post alheio e' a funcao; apagar post
 * alheio da 404; comentario alheio so' o autor do post apaga.
 *
 * Curtida e' sub-recurso (PUT curte, DELETE descurte), idempotente, em vez
 * do toggle da tela - ver PostService.curtir.
 */
@RestController
@Tag(name = "Feed")
public class FeedApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final PostService postService;

    public FeedApiController(UsuarioAtualService usuarioAtualService, PostService postService) {
        this.usuarioAtualService = usuarioAtualService;
        this.postService = postService;
    }

    @Operation(summary = "Feed global (Descobrir), posts mais recentes primeiro")
    @GetMapping("/api/v1/feed/descobrir")
    public Pagina<PostResposta> descobrir(@RequestParam(required = false) String cursor,
                                          @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return postService.paginaDoFeed(usuario.getId(), Cursor.decodificar(cursor), Pagina.TAMANHO_PADRAO)
                .mapear(PostResposta::de);
    }

    @Operation(summary = "Feed de quem o usuário segue, com os próprios posts, mais recentes primeiro")
    @GetMapping("/api/v1/feed/seguindo")
    public Pagina<PostResposta> seguindo(@RequestParam(required = false) String cursor,
                                         @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return postService.paginaSeguindo(usuario.getId(), Cursor.decodificar(cursor), Pagina.TAMANHO_PADRAO)
                .mapear(PostResposta::de);
    }

    @Operation(summary = "Um post, no formato do feed",
            description = "Traz os comentários mais recentes e o total; os demais vêm de "
                    + "GET /api/v1/posts/{postId}/comentarios.")
    @GetMapping("/api/v1/posts/{postId}")
    public PostResposta post(@PathVariable Long postId, @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return PostResposta.de(postService.visaoDoPost(postId, usuario.getId()));
    }

    @Operation(summary = "Publica um post de texto")
    @PostMapping("/api/v1/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResposta publicar(@Valid @RequestBody PostForm form, @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        Post post = postService.criar(usuario.getId(), form.getTexto());
        return PostResposta.de(postService.visaoDoPost(post, usuario.getId()));
    }

    @Operation(summary = "Apaga o próprio post (comentários e curtidas saem junto)",
            description = "Post de outra pessoa responde 404, igual a post inexistente.")
    @DeleteMapping("/api/v1/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarPost(@PathVariable Long postId, @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        postService.apagarPost(postId, usuario.getId());
    }

    @Operation(summary = "Comentários de um post, do mais antigo para o mais novo")
    @GetMapping("/api/v1/posts/{postId}/comentarios")
    public Pagina<ComentarioResposta> comentarios(@PathVariable Long postId,
                                                  @RequestParam(required = false) String cursor,
                                                  @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return postService.paginaDeComentarios(postId, usuario.getId(), Cursor.decodificar(cursor),
                        Pagina.TAMANHO_PADRAO)
                .mapear(ComentarioResposta::de);
    }

    @Operation(summary = "Comenta em um post (de qualquer pessoa)")
    @PostMapping("/api/v1/posts/{postId}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioResposta comentar(@PathVariable Long postId, @Valid @RequestBody ComentarioForm form,
                                       @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        Comentario comentario = postService.comentar(postId, usuario.getId(), form.getTexto());
        return ComentarioResposta.de(postService.visaoDoComentario(comentario, usuario.getId()));
    }

    @Operation(summary = "Apaga um comentário",
            description = "Quem escreveu ou o autor do post. Qualquer outra pessoa recebe 404.")
    @DeleteMapping("/api/v1/comentarios/{comentarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarComentario(@PathVariable Long comentarioId, @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        postService.apagarComentario(comentarioId, usuario.getId());
    }

    @Operation(summary = "Curte o post (idempotente)")
    @PutMapping("/api/v1/posts/{postId}/curtida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void curtir(@PathVariable Long postId, @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        postService.curtir(postId, usuario.getId());
    }

    @Operation(summary = "Desfaz a curtida (idempotente)")
    @DeleteMapping("/api/v1/posts/{postId}/curtida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void descurtir(@PathVariable Long postId, @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        postService.descurtir(postId, usuario.getId());
    }
}
