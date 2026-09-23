package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.api.ProblemaApi;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conclusao de item como sub-recurso: PUT marca, DELETE desmarca. As duas
 * sao idempotentes, ao contrario do toggle da tela (ver
 * TreinoDoDiaService.definirConclusao).
 */
@RestController
@RequestMapping("/api/v1/treino-do-dia")
@Tag(name = "Treino do dia")
public class TreinoDoDiaApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final TreinoDoDiaService treinoDoDiaService;

    public TreinoDoDiaApiController(UsuarioAtualService usuarioAtualService, TreinoDoDiaService treinoDoDiaService) {
        this.usuarioAtualService = usuarioAtualService;
        this.treinoDoDiaService = treinoDoDiaService;
    }

    @Operation(summary = "Treino de hoje (gerado na primeira consulta do dia e fixo até amanhã)",
            description = "409 (urn:corpo-forte:problema:avaliacao-pendente) sem avaliação física.")
    @GetMapping
    public TreinoDoDiaResposta obter(@AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return treinoDoDiaService.obterOuGerarDoDia(usuario)
                .map(TreinoDoDiaResposta::de)
                .orElseThrow(ProblemaApi::avaliacaoPendente);
    }

    @Operation(summary = "Marca o item como concluído")
    @PutMapping("/itens/{itemId}/conclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void concluir(@PathVariable Long itemId, @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        treinoDoDiaService.definirConclusao(usuario.getId(), itemId, true);
    }

    @Operation(summary = "Desmarca o item")
    @DeleteMapping("/itens/{itemId}/conclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desmarcar(@PathVariable Long itemId, @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        treinoDoDiaService.definirConclusao(usuario.getId(), itemId, false);
    }
}
