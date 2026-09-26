package com.corpoforte.tracker.arquivos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Arquivos num diretorio local, servidos por /arquivos/** (ArquivoController)
 * com assinatura HMAC-SHA256 de (chave, expiracao).
 *
 * A expiracao e' arredondada pra hora cheia: a URL de uma mesma foto fica
 * igual durante a hora inteira (entre 1h e 2h de validade), e o cache de
 * imagem do cliente, que e' por URL, continua funcionando entre um
 * carregamento do feed e outro. Com expiracao "agora + 1h" exata, cada
 * resposta teria uma URL diferente e nenhuma foto ficaria em cache.
 */
@Component
public class ArmazenamentoEmDisco implements ArmazenamentoArquivos {

    private static final Logger log = LoggerFactory.getLogger(ArmazenamentoEmDisco.class);

    /** So' chave que o proprio servidor gera: nada de "..", barra solta ou
     * nome escolhido por cliente chegando ao sistema de arquivos. */
    static final Pattern CHAVE_VALIDA = Pattern.compile("(posts|perfis)/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(_mini)?\\.jpg");

    private static final long SEGUNDOS_POR_HORA = 3600;

    private final Path diretorio;
    private final String urlBase;
    private final byte[] segredo;
    private final Clock relogio;

    @Autowired
    public ArmazenamentoEmDisco(@Value("${app.arquivos.diretorio}") String diretorio,
                                @Value("${app.arquivos.url-base}") String urlBase,
                                @Value("${app.arquivos.segredo:}") String segredo) {
        this(Path.of(diretorio), urlBase, segredoOuAleatorio(segredo), Clock.systemUTC());
    }

    ArmazenamentoEmDisco(Path diretorio, String urlBase, byte[] segredo, Clock relogio) {
        this.diretorio = diretorio.toAbsolutePath().normalize();
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.segredo = segredo;
        this.relogio = relogio;
    }

    /** Mesma regra do segredo do JWT (Fase 10): nunca um valor padrao escrito
     * no codigo, que o repositorio e' publico. */
    private static byte[] segredoOuAleatorio(String segredo) {
        if (!segredo.isBlank()) {
            return segredo.getBytes(StandardCharsets.UTF_8);
        }
        log.warn("APP_ARQUIVOS_SEGREDO nao configurado: usando segredo aleatorio - "
                + "URLs de imagem deixam de valer quando a aplicacao reiniciar");
        byte[] aleatorio = new byte[32];
        new SecureRandom().nextBytes(aleatorio);
        return aleatorio;
    }

    @Override
    public void gravar(String chave, byte[] conteudo) {
        Path destino = caminho(chave);
        try {
            Files.createDirectories(destino.getParent());
            // escreve ao lado e move: quem le nunca pega um arquivo pela metade
            Path temporario = Files.createTempFile(destino.getParent(), "gravando-", ".tmp");
            Files.write(temporario, conteudo);
            Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar " + chave, e);
        }
    }

    @Override
    public void apagar(String chave) {
        try {
            Files.deleteIfExists(caminho(chave));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao apagar " + chave, e);
        }
    }

    @Override
    public String urlAssinada(String chave) {
        caminho(chave); // valida a chave
        long expira = (relogio.instant().getEpochSecond() / SEGUNDOS_POR_HORA + 2) * SEGUNDOS_POR_HORA;
        return urlBase + "/" + chave + "?expira=" + expira + "&assinatura=" + assinar(chave, expira);
    }

    /**
     * O conteudo, se a assinatura confere e nao expirou. Qualquer problema -
     * chave fora do padrao, assinatura errada, expirada, arquivo apagado - e'
     * o mesmo vazio: quem chama responde 404 sem dizer qual foi.
     */
    Optional<byte[]> lerSeAutorizado(String chave, long expira, String assinatura) {
        if (!CHAVE_VALIDA.matcher(chave).matches() || assinatura == null
                || expira < relogio.instant().getEpochSecond()) {
            return Optional.empty();
        }
        byte[] esperada = assinar(chave, expira).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(esperada, assinatura.getBytes(StandardCharsets.US_ASCII))) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(caminho(chave)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    long segundosAteExpirar(long expira) {
        return Math.max(0, expira - relogio.instant().getEpochSecond());
    }

    private Path caminho(String chave) {
        if (!CHAVE_VALIDA.matcher(chave).matches()) {
            throw new IllegalArgumentException("Chave de arquivo invalida: " + chave);
        }
        return diretorio.resolve(chave);
    }

    private String assinar(String chave, long expira) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            byte[] hmac = mac.doFinal((chave + "\n" + expira).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponivel na JVM", e);
        }
    }
}
