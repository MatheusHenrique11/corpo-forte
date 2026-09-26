package com.corpoforte.tracker.social;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.usuario.FotoDePerfil;
import com.corpoforte.tracker.usuario.UsernameService;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * Bloquear e desbloquear resolvem o username SEM a regra de bloqueio: quem
 * foi bloqueado por alguem ainda pode bloquear de volta (e continuar
 * bloqueando se o outro desbloquear), e quem bloqueou precisa achar a conta
 * pra desbloquear - ela nao aparece mais em perfil nem busca, so' em
 * GET /bloqueios.
 */
@RestController
@Tag(name = "Bloqueio")
public class BloqueioApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final UsernameService usernameService;
    private final BloqueioService bloqueioService;
    private final FotoDePerfil fotoDePerfil;

    public BloqueioApiController(UsuarioAtualService usuarioAtualService, UsernameService usernameService,
                                 BloqueioService bloqueioService, FotoDePerfil fotoDePerfil) {
        this.usuarioAtualService = usuarioAtualService;
        this.usernameService = usernameService;
        this.bloqueioService = bloqueioService;
        this.fotoDePerfil = fotoDePerfil;
    }

    @Operation(summary = "Bloqueia a conta (idempotente)",
            description = "Desfaz o seguir nos dois sentidos. Nenhuma das duas contas vê mais o conteúdo da outra.")
    @PutMapping("/api/v1/usuarios/{username}/bloqueio")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bloquear(@PathVariable String username, @AuthenticationPrincipal Jwt accessToken) {
        Usuario eu = usuarioAtualService.obterUsuarioAtual(accessToken);
        bloqueioService.bloquear(eu.getId(), porUsername(username).getId());
    }

    @Operation(summary = "Desfaz o bloqueio feito por esta conta (idempotente)")
    @DeleteMapping("/api/v1/usuarios/{username}/bloqueio")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desbloquear(@PathVariable String username, @AuthenticationPrincipal Jwt accessToken) {
        Usuario eu = usuarioAtualService.obterUsuarioAtual(accessToken);
        bloqueioService.desbloquear(eu.getId(), porUsername(username).getId());
    }

    @Operation(summary = "Contas que esta conta bloqueou, mais recente primeiro")
    @GetMapping("/api/v1/bloqueios")
    public Pagina<UsuarioResumoResposta> bloqueados(@RequestParam(required = false) String cursor,
                                                    @AuthenticationPrincipal Jwt accessToken) {
        Usuario eu = usuarioAtualService.obterUsuarioAtual(accessToken);
        // bloquear desfaz o seguir: ninguem desta lista e' seguido
        return bloqueioService.paginaDeBloqueados(eu.getId(), Cursor.decodificar(cursor), Pagina.TAMANHO_PADRAO)
                .mapearTodos(usuarios -> UsuarioResumoResposta.de(usuarios, Set.of(), fotoDePerfil::url));
    }

    private Usuario porUsername(String username) {
        return usernameService.buscarPorUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
