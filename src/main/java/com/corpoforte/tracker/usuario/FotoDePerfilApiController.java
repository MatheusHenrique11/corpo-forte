package com.corpoforte.tracker.usuario;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@Tag(name = "Perfil público")
public class FotoDePerfilApiController {

    private final UsuarioAtualService usuarioAtualService;
    private final FotoDePerfil fotoDePerfil;

    public FotoDePerfilApiController(UsuarioAtualService usuarioAtualService, FotoDePerfil fotoDePerfil) {
        this.usuarioAtualService = usuarioAtualService;
        this.fotoDePerfil = fotoDePerfil;
    }

    @Operation(summary = "Troca a foto de perfil (multipart, campo 'foto')",
            description = "JPEG ou PNG, até 5 MB. Vale no lugar da foto do Google até ser removida.")
    @PutMapping(path = "/api/v1/perfil/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UsuarioAtualResposta trocar(@RequestParam("foto") MultipartFile foto,
                                       @AuthenticationPrincipal Jwt accessToken) throws IOException {
        Usuario usuario = fotoDePerfil.trocar(usuarioAtualService.obterUsuarioAtual(accessToken), foto.getBytes());
        return UsuarioAtualResposta.de(usuario, fotoDePerfil.url(usuario));
    }

    @Operation(summary = "Remove a foto de perfil enviada (volta a do Google)")
    @DeleteMapping("/api/v1/perfil/foto")
    public UsuarioAtualResposta remover(@AuthenticationPrincipal Jwt accessToken) {
        Usuario usuario = fotoDePerfil.remover(usuarioAtualService.obterUsuarioAtual(accessToken));
        return UsuarioAtualResposta.de(usuario, fotoDePerfil.url(usuario));
    }
}
