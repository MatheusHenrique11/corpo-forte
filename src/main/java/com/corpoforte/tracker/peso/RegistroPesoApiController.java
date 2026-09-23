package com.corpoforte.tracker.peso;

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
@RequestMapping("/api/v1/pesos")
@Tag(name = "Registro de peso")
public class RegistroPesoApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final RegistroPesoService registroPesoService;

    public RegistroPesoApiController(UsuarioAtualService usuarioAtualService,
                                     RegistroPesoService registroPesoService) {
        this.usuarioAtualService = usuarioAtualService;
        this.registroPesoService = registroPesoService;
    }

    @Operation(summary = "Histórico de pesagens, mais recente primeiro")
    @GetMapping
    public Pagina<RegistroPesoResposta> historico(@RequestParam(required = false) String cursor,
                                                  @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return registroPesoService
                .paginaDoUsuario(usuario.getId(), Cursor.decodificar(cursor), Pagina.TAMANHO_PADRAO)
                .mapear(RegistroPesoResposta::de);
    }

    @Operation(summary = "Registra o peso de um dia",
            description = "Um registro por dia: mandar de novo a mesma data corrige aquele dia. Sem data, vale hoje. "
                    + "O peso do perfil passa a ser o do registro mais recente por data.")
    @PostMapping
    public RegistroPesoResposta registrar(@Valid @RequestBody RegistroPesoForm form,
                                          @AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return RegistroPesoResposta.de(registroPesoService.registrar(usuario, form.getData(), form.getPesoKg()));
    }

    @Operation(summary = "Tendência semanal (média da semana contra a anterior)",
            description = "409 (urn:corpo-forte:problema:registro-de-peso-pendente) sem nenhum registro.")
    @GetMapping("/tendencia")
    public TendenciaResposta tendencia(@AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(accessToken);
        return registroPesoService.tendenciaDoUsuario(usuario.getId())
                .map(TendenciaResposta::de)
                .orElseThrow(ProblemaApi::registroDePesoPendente);
    }
}
