package com.corpoforte.tracker.atividade;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Diario de treino (Fase 16): privado. Toda rota opera so' sobre as
 * atividades da conta do token - atividade de outra conta da o mesmo 404
 * de inexistente.
 */
@RestController
@Tag(name = "Atividades")
public class AtividadeApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final AtividadeService atividadeService;

    public AtividadeApiController(UsuarioAtualService usuarioAtualService, AtividadeService atividadeService) {
        this.usuarioAtualService = usuarioAtualService;
        this.atividadeService = atividadeService;
    }

    @Operation(summary = "Finaliza o treino de hoje, criando a atividade a partir dos itens marcados",
            description = "Sem ajustes, cada item marcado entra com a prescrição. 409 "
                    + "(urn:corpo-forte:problema:treino-sem-itens-concluidos) sem item marcado; 409 "
                    + "(...:treino-ja-finalizado) se já foi finalizado.")
    @PostMapping("/api/v1/treino-do-dia/finalizar")
    @ResponseStatus(HttpStatus.CREATED)
    public AtividadeResposta finalizar(@Valid @RequestBody(required = false) FinalizacaoRequisicao requisicao,
                                       @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return resposta(atividadeService.finalizarTreinoDoDia(usuario, requisicao));
    }

    @Operation(summary = "Registra um treino livre com exercícios do catálogo",
            description = "Cada série vale repetições ou segundos, conforme a medida do exercício.")
    @PostMapping("/api/v1/atividades")
    @ResponseStatus(HttpStatus.CREATED)
    public AtividadeResposta registrarLivre(@Valid @RequestBody AtividadeLivreRequisicao requisicao,
                                            @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return resposta(atividadeService.registrarLivre(usuario, requisicao));
    }

    @Operation(summary = "Diário de treino, dia mais recente primeiro")
    @GetMapping("/api/v1/atividades")
    public Pagina<AtividadeResposta> diario(@RequestParam(required = false) String cursor,
                                            @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return atividadeService.paginaDoUsuario(usuario.getId(), Cursor.decodificar(cursor), Pagina.TAMANHO_PADRAO)
                .mapearTodos(atividadeService::respostas);
    }

    @Operation(summary = "Uma atividade do diário")
    @GetMapping("/api/v1/atividades/{atividadeId}")
    public AtividadeResposta obter(@PathVariable Long atividadeId, @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return resposta(atividadeService.obter(usuario.getId(), atividadeId));
    }

    @Operation(summary = "Apaga uma atividade do diário")
    @DeleteMapping("/api/v1/atividades/{atividadeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long atividadeId, @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        atividadeService.apagar(usuario.getId(), atividadeId);
    }

    private AtividadeResposta resposta(Atividade atividade) {
        return atividadeService.respostas(List.of(atividade)).get(0);
    }
}
