package com.corpoforte.tracker.perfilpublico;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.feed.PostResposta;
import com.corpoforte.tracker.feed.PostService;
import com.corpoforte.tracker.social.SeguimentoService;
import com.corpoforte.tracker.social.UsuarioResumoResposta;
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

import java.util.List;

/**
 * Perfil publico: o que a comunidade ve de uma conta. Pacote proprio
 * porque junta conta (usuario), posts (feed) e seguidores (social), e
 * nenhum deles deve depender dos outros por causa disso.
 *
 * Perfil e posts em rotas separadas: a lista de posts pagina, o perfil
 * nao - juntos, cada pagina de posts repetiria o perfil inteiro.
 */
@RestController
@Tag(name = "Perfil público")
public class PerfilPublicoApiController {

    /** Busca e' uma caixa de pesquisa, nao uma listagem: 20 resultados
     * bastam, sem paginar. */
    static final int LIMITE_DA_BUSCA = 20;

    private final UsuarioAtualService usuarioAtualService;
    private final UsernameService usernameService;
    private final PostService postService;
    private final SeguimentoService seguimentoService;

    public PerfilPublicoApiController(UsuarioAtualService usuarioAtualService, UsernameService usernameService,
                                      PostService postService, SeguimentoService seguimentoService) {
        this.usuarioAtualService = usuarioAtualService;
        this.usernameService = usernameService;
        this.postService = postService;
        this.seguimentoService = seguimentoService;
    }

    @Operation(summary = "Busca contas pelo começo do username ou do nome",
            description = "Sem diferenciar maiúsculas, até 20 resultados (proximoCursor sempre nulo).")
    @GetMapping("/api/v1/usuarios")
    public Pagina<UsuarioResumoResposta> buscar(@RequestParam(defaultValue = "") String busca,
                                                @AuthenticationPrincipal Jwt accessToken) {
        Usuario quemVe = usuarioAtualService.obterUsuarioAtual(accessToken);
        List<Usuario> encontrados = usernameService.buscarPorPrefixo(busca, LIMITE_DA_BUSCA);
        return Pagina.completa(UsuarioResumoResposta.de(encontrados, seguimentoService.quaisSegue(
                quemVe.getId(), encontrados.stream().map(Usuario::getId).toList())));
    }

    @Operation(summary = "Perfil público de uma conta, pelo username (sem diferenciar maiúsculas)")
    @GetMapping("/api/v1/usuarios/{username}")
    public PerfilPublicoResposta perfil(@PathVariable String username, @AuthenticationPrincipal Jwt accessToken) {
        Usuario quemVe = usuarioAtualService.obterUsuarioAtual(accessToken);
        return resposta(porUsername(username), quemVe);
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
        return resposta(salvo, salvo);
    }

    private PerfilPublicoResposta resposta(Usuario usuario, Usuario quemVe) {
        return PerfilPublicoResposta.de(usuario, postService.contarDoAutor(usuario.getId()),
                seguimentoService.contar(usuario.getId()),
                seguimentoService.segue(quemVe.getId(), usuario.getId()));
    }

    private Usuario porUsername(String username) {
        return usernameService.buscarPorUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
