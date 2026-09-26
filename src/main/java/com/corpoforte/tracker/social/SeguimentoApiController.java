package com.corpoforte.tracker.social;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.usuario.FotoDePerfil;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Seguir como sub-recurso da conta seguida: PUT segue, DELETE deixa de
 * seguir, os dois idempotentes (mesmo formato da curtida). Seguir a conta
 * de outra pessoa pelo username e' a funcao (EndpointsComIdIT). Conta com
 * bloqueio com quem pede responde 404 em todas as rotas daqui.
 */
@RestController
@Tag(name = "Seguir")
public class SeguimentoApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final BloqueioService bloqueioService;
    private final SeguimentoService seguimentoService;
    private final FotoDePerfil fotoDePerfil;

    public SeguimentoApiController(UsuarioAtualService usuarioAtualService, BloqueioService bloqueioService,
                                   SeguimentoService seguimentoService, FotoDePerfil fotoDePerfil) {
        this.usuarioAtualService = usuarioAtualService;
        this.bloqueioService = bloqueioService;
        this.seguimentoService = seguimentoService;
        this.fotoDePerfil = fotoDePerfil;
    }

    @Operation(summary = "Segue a conta (idempotente)", description = "400 ao tentar seguir a própria conta.")
    @PutMapping("/api/v1/usuarios/{username}/seguimento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void seguir(@PathVariable String username, @AuthenticationPrincipal Jwt accessToken) {
        Usuario eu = usuarioAtualService.obterUsuarioAtual(accessToken);
        seguimentoService.seguir(eu.getId(), bloqueioService.contaVisivel(username, eu.getId()).getId());
    }

    @Operation(summary = "Deixa de seguir a conta (idempotente)")
    @DeleteMapping("/api/v1/usuarios/{username}/seguimento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deixarDeSeguir(@PathVariable String username, @AuthenticationPrincipal Jwt accessToken) {
        Usuario eu = usuarioAtualService.obterUsuarioAtual(accessToken);
        seguimentoService.deixarDeSeguir(eu.getId(), bloqueioService.contaVisivel(username, eu.getId()).getId());
    }

    @Operation(summary = "Quem segue a conta, quem seguiu por último primeiro")
    @GetMapping("/api/v1/usuarios/{username}/seguidores")
    public Pagina<UsuarioResumoResposta> seguidores(@PathVariable String username,
                                                    @RequestParam(required = false) String cursor,
                                                    @AuthenticationPrincipal Jwt accessToken) {
        Usuario eu = usuarioAtualService.obterUsuarioAtual(accessToken);
        return seguimentoService
                .paginaDeSeguidores(bloqueioService.contaVisivel(username, eu.getId()).getId(), eu.getId(),
                        Cursor.decodificar(cursor), Pagina.TAMANHO_PADRAO)
                .mapearTodos(usuarios -> resumos(usuarios, eu));
    }

    @Operation(summary = "Quem a conta segue, quem ela seguiu por último primeiro")
    @GetMapping("/api/v1/usuarios/{username}/seguindo")
    public Pagina<UsuarioResumoResposta> seguindo(@PathVariable String username,
                                                  @RequestParam(required = false) String cursor,
                                                  @AuthenticationPrincipal Jwt accessToken) {
        Usuario eu = usuarioAtualService.obterUsuarioAtual(accessToken);
        return seguimentoService
                .paginaDeSeguindo(bloqueioService.contaVisivel(username, eu.getId()).getId(), eu.getId(),
                        Cursor.decodificar(cursor), Pagina.TAMANHO_PADRAO)
                .mapearTodos(usuarios -> resumos(usuarios, eu));
    }

    private List<UsuarioResumoResposta> resumos(List<Usuario> usuarios, Usuario quemVe) {
        return UsuarioResumoResposta.de(usuarios,
                seguimentoService.quaisSegue(quemVe.getId(), usuarios.stream().map(Usuario::getId).toList()),
                fotoDePerfil::url);
    }
}
