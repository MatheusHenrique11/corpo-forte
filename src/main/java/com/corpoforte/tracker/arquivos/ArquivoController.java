package com.corpoforte.tracker.arquivos;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Entrega os arquivos do ArmazenamentoEmDisco pela URL assinada. Fora de
 * /api de proposito: nao faz parte do contrato da API (o cliente so'
 * recebe a URL pronta e a usa num <img>), e com outra implementacao de
 * ArmazenamentoArquivos a URL poderia apontar pra fora desta aplicacao.
 * Rota publica (SecurityConfig): quem autoriza e'
 * a assinatura, nao uma sessao.
 *
 * Resposta 404 via ResponseEntity, nao via excecao: excecao viraria
 * sendError -> /error, que mora na chain web e exige login - um <img> com
 * URL vencida receberia um redirecionamento pro Google em vez de 404.
 */
@RestController
public class ArquivoController {

    private final ArmazenamentoEmDisco armazenamento;

    public ArquivoController(ArmazenamentoEmDisco armazenamento) {
        this.armazenamento = armazenamento;
    }

    @GetMapping("/arquivos/{pasta}/{nome}")
    public ResponseEntity<byte[]> entregar(@PathVariable String pasta, @PathVariable String nome,
                                           @RequestParam(defaultValue = "0") long expira,
                                           @RequestParam(required = false) String assinatura) {
        return armazenamento.lerSeAutorizado(pasta + "/" + nome, expira, assinatura)
                .map(conteudo -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.maxAge(armazenamento.segundosAteExpirar(expira), TimeUnit.SECONDS)
                                .cachePrivate())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(conteudo))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
