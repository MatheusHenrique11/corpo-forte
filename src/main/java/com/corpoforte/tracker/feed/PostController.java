package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PostController {

    private final UsuarioAtualService usuarioAtualService;
    private final PostService postService;

    public PostController(UsuarioAtualService usuarioAtualService, PostService postService) {
        this.usuarioAtualService = usuarioAtualService;
        this.postService = postService;
    }

    @GetMapping("/feed")
    public String exibirFeed(@AuthenticationPrincipal OidcUser principal, Model model) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        model.addAttribute("postForm", new PostForm());
        model.addAttribute("posts", postService.listarFeed(usuario.getId()));
        return "feed";
    }

    @PostMapping("/feed")
    public String publicarPost(@Valid @ModelAttribute("postForm") PostForm form, BindingResult bindingResult,
                                @AuthenticationPrincipal OidcUser principal, Model model) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);

        if (bindingResult.hasErrors()) {
            model.addAttribute("posts", postService.listarFeed(usuario.getId()));
            return "feed";
        }

        postService.criar(usuario.getId(), form.getTexto());

        return "redirect:/feed";
    }

    /**
     * O erro de validacao volta como par (postId, mensagem) em vez de
     * th:errors no template: o feed tem uma caixa de comentario por post
     * compartilhando o mesmo ComentarioForm, entao th:object marcaria a
     * mensagem embaixo de TODAS as caixas (e th:field ainda repetiria o
     * texto rejeitado em cada uma). Com o id, o template mostra o erro so'
     * embaixo do post em que o usuario errou.
     */
    @PostMapping("/feed/posts/{postId}/comentarios")
    public String comentar(@PathVariable Long postId,
                            @Valid @ModelAttribute("comentarioForm") ComentarioForm form,
                            BindingResult bindingResult,
                            @AuthenticationPrincipal OidcUser principal, Model model) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);

        if (bindingResult.hasErrors()) {
            FieldError erro = bindingResult.getFieldError("texto");
            model.addAttribute("postForm", new PostForm());
            model.addAttribute("posts", postService.listarFeed(usuario.getId()));
            model.addAttribute("erroComentarioPostId", postId);
            model.addAttribute("erroComentario", erro != null ? erro.getDefaultMessage() : null);
            return "feed";
        }

        postService.comentar(postId, usuario.getId(), form.getTexto());

        return "redirect:/feed";
    }

    @PostMapping("/feed/posts/{postId}/curtir")
    public String alternarCurtida(@PathVariable Long postId, @AuthenticationPrincipal OidcUser principal) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        postService.alternarCurtida(postId, usuario.getId());
        return "redirect:/feed";
    }
}
