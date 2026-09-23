package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
        usuarioAtualService.obterUsuarioAtual(principal);
        model.addAttribute("postForm", new PostForm());
        model.addAttribute("posts", postService.listarFeed());
        return "feed";
    }

    @PostMapping("/feed")
    public String publicarPost(@Valid @ModelAttribute("postForm") PostForm form, BindingResult bindingResult,
                                @AuthenticationPrincipal OidcUser principal, Model model) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);

        if (bindingResult.hasErrors()) {
            model.addAttribute("posts", postService.listarFeed());
            return "feed";
        }

        postService.criar(usuario.getId(), form.getTexto());

        return "redirect:/feed";
    }
}
