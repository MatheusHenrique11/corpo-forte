package com.corpoforte.tracker.perfilpublico;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.feed.PostResposta;
import com.corpoforte.tracker.feed.PostService;
import com.corpoforte.tracker.usuario.UsernameService;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Perfil publico: o que a comunidade ve de uma conta. Pacote proprio
 * porque junta conta (usuario) e posts (feed), e nenhum dos dois deve
 * depender do outro por causa disso.
 *
 * Perfil e posts em rotas separadas: a lista de posts pagina, o perfil
 * nao - juntos, cada pagina de posts repetiria o perfil inteiro.
 */
@RestController
@Tag(name = "Perfil público")
public class PerfilPublicoApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final UsernameService usernameService;
    private final PostService postService;

    public PerfilPublicoApiController(UsuarioAtualService usuarioAtualService, UsernameService usernameService,
                                      PostService postService) {
        this.usuarioAtualService = usuarioAtualService;
        this.usernameService = usernameService;
        this.postService = postService;
    }

    @Operation(summary = "Perfil público de uma conta, pelo username (sem diferenciar maiúsculas)")
    @GetMapping("/api/v1/usuarios/{username}")
    public PerfilPublicoResposta perfil(@PathVariable String username) {
        Usuario usuario = porUsername(username);
        return PerfilPublicoResposta.de(usuario, postService.contarDoAutor(usuario.getId()));
    }

    @Operation(summary = "Posts de uma conta, mais recentes primeiro")
    @GetMapping("/api/v1/usuarios/{username}/posts")
    public Pagina<PostResposta> posts(@PathVariable String username, @RequestParam(required = false) String cursor,
                                      @AuthenticationPrincipal Jwt accessToken) {
        Usuario autor = porUsername(username);
        Usuario quemVe = usuarioAtualService.obterUsuarioAtual(accessToken);
        return postService.paginaDoAutor(autor.getId(), quemVe.getId(), Cursor.decodificar(cursor),
                        Pagina.TAMANHO_PADRAO)
                .mapear(PostResposta::de);
    }

    @Operation(summary = "Troca o username e a bio da própria conta",
            description = "409 (urn:corpo-forte:problema:username-indisponivel) se o nome for de outra conta.")
    @PutMapping("/api/v1/perfil/publico")
    public PerfilPublicoResposta atualizar(@Valid @RequestBody PerfilPublicoRequisicao requisicao,
                                           @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        usernameService.exigirLivre(requisicao.username(), usuario.getId());
        usuario.atualizarPerfilPublico(requisicao.username(), requisicao.bio());
        Usuario salvo = usernameService.salvarComUsernameUnico(usuario);
        return PerfilPublicoResposta.de(salvo, postService.contarDoAutor(salvo.getId()));
    }

    private Usuario porUsername(String username) {
        return usernameService.buscarPorUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
