package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.api.ProblemaApi;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/avaliacoes")
@Tag(name = "Avaliação física")
public class AvaliacaoFisicaApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final AvaliacaoFisicaService avaliacaoFisicaService;

    public AvaliacaoFisicaApiController(UsuarioAtualService usuarioAtualService,
                                        AvaliacaoFisicaService avaliacaoFisicaService) {
        this.usuarioAtualService = usuarioAtualService;
        this.avaliacaoFisicaService = avaliacaoFisicaService;
    }

    @Operation(summary = "Histórico de avaliações, mais recente primeiro")
    @GetMapping
    public Pagina<AvaliacaoResposta> historico(@RequestParam(required = false) String cursor,
                                               @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return avaliacaoFisicaService
                .paginaDoHistorico(usuario.getId(), Cursor.decodificar(cursor), Pagina.TAMANHO_PADRAO)
                .mapear(this::resposta);
    }

    @Operation(summary = "Registra a avaliação de hoje",
            description = "Uma avaliação por dia: enviar de novo no mesmo dia corrige a de hoje. "
                    + "A mais recente reinicia o ciclo de periodização do treino.")
    @PostMapping
    public AvaliacaoResposta registrar(@Valid @RequestBody AvaliacaoFisicaForm form,
                                       @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        AvaliacaoFisica salva = avaliacaoFisicaService.salvar(usuario.getId(),
                form.getRepsPuxarVertical(), form.getRepsEmpurrarVertical(), form.getRepsPernasBilateral(),
                form.getRepsPuxarHorizontal(), form.getRepsEmpurrarHorizontal(), form.getRepsPernasUnilateral());
        return resposta(salva);
    }

    @Operation(summary = "Compara a avaliação mais recente com a anterior",
            description = "409 (urn:corpo-forte:problema:avaliacoes-insuficientes) com menos de duas avaliações.")
    @GetMapping("/comparacao")
    public ComparacaoResposta comparacao(@AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return avaliacaoFisicaService.compararUltimas(usuario.getId())
                .map(ComparacaoResposta::de)
                .orElseThrow(ProblemaApi::avaliacoesInsuficientes);
    }

    private AvaliacaoResposta resposta(AvaliacaoFisica avaliacao) {
        return AvaliacaoResposta.de(avaliacao, avaliacaoFisicaService.resultadoDe(avaliacao));
    }
}
