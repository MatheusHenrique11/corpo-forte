package com.corpoforte.tracker.arquivos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArmazenamentoEmDiscoTest {

    private static final String CHAVE = "posts/0f8fad5b-d9cb-469f-a165-70867728950e.jpg";
    private static final byte[] SEGREDO = "segredo-de-teste".getBytes(StandardCharsets.UTF_8);
    private static final Instant AGORA = Instant.parse("2026-09-23T15:10:00Z");

    @TempDir
    Path diretorio;

    @Test
    void urlAssinadaDaAcessoAoArquivoEApagarTiraOAcesso() {
        ArmazenamentoEmDisco armazenamento = em(AGORA);
        armazenamento.gravar(CHAVE, new byte[]{1, 2, 3});

        UriComponents url = url(armazenamento.urlAssinada(CHAVE));
        assertThat(ler(armazenamento, url)).containsExactly(1, 2, 3);

        armazenamento.apagar(CHAVE);
        armazenamento.apagar(CHAVE); // idempotente
        assertThat(armazenamento.lerSeAutorizado(CHAVE, expira(url), assinatura(url))).isEmpty();
    }

    @Test
    void assinaturaAdulteradaOuDeOutroArquivoNaoDaAcesso() {
        ArmazenamentoEmDisco armazenamento = em(AGORA);
        String outra = "posts/7c9e6679-7425-40de-944b-e07fc1f90ae7.jpg";
        armazenamento.gravar(CHAVE, new byte[]{1});
        armazenamento.gravar(outra, new byte[]{2});
        UriComponents url = url(armazenamento.urlAssinada(CHAVE));

        assertThat(armazenamento.lerSeAutorizado(CHAVE, expira(url), assinatura(url) + "x")).isEmpty();
        assertThat(armazenamento.lerSeAutorizado(outra, expira(url), assinatura(url))).isEmpty();
        assertThat(armazenamento.lerSeAutorizado(CHAVE, expira(url) + 3600, assinatura(url))).isEmpty();
        assertThat(armazenamento.lerSeAutorizado(CHAVE, expira(url), null)).isEmpty();
    }

    /** Vale entre 1 e 2 horas e depois expira - mesmo segredo, relogio
     * adiantado. */
    @Test
    void urlExpira() {
        em(AGORA).gravar(CHAVE, new byte[]{1});
        UriComponents url = url(em(AGORA).urlAssinada(CHAVE));

        assertThat(ler(em(AGORA.plusSeconds(3600)), url)).isNotNull();
        assertThat(em(AGORA.plusSeconds(3 * 3600)).lerSeAutorizado(CHAVE, expira(url), assinatura(url))).isEmpty();
    }

    /** Dentro da mesma hora a URL e' a mesma: o cache de imagem do cliente,
     * que e' por URL, funciona entre um carregamento e outro. */
    @Test
    void urlFicaIgualDentroDaMesmaHora() {
        assertThat(em(AGORA).urlAssinada(CHAVE)).isEqualTo(em(AGORA.plusSeconds(40 * 60)).urlAssinada(CHAVE));
    }

    /** So' chave gerada pelo servidor chega ao disco: nada de ".." nem nome
     * escolhido por cliente. */
    @Test
    void chaveForaDoPadraoNaoChegaAoDisco() {
        ArmazenamentoEmDisco armazenamento = em(AGORA);

        assertThatThrownBy(() -> armazenamento.gravar("posts/../../etc/passwd", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> armazenamento.gravar("posts/foto-da-praia.jpg", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(armazenamento.lerSeAutorizado("../application.yml", Long.MAX_VALUE, "x")).isEmpty();
    }

    private ArmazenamentoEmDisco em(Instant instante) {
        return new ArmazenamentoEmDisco(diretorio, "http://localhost/arquivos/", SEGREDO,
                Clock.fixed(instante, ZoneOffset.UTC));
    }

    private static UriComponents url(String url) {
        assertThat(url).startsWith("http://localhost/arquivos/" + CHAVE + "?");
        return UriComponentsBuilder.fromUriString(url).build();
    }

    private static long expira(UriComponents url) {
        return Long.parseLong(url.getQueryParams().getFirst("expira"));
    }

    private static String assinatura(UriComponents url) {
        return url.getQueryParams().getFirst("assinatura");
    }

    private static byte[] ler(ArmazenamentoEmDisco armazenamento, UriComponents url) {
        return armazenamento.lerSeAutorizado(CHAVE, expira(url), assinatura(url)).orElseThrow();
    }
}
