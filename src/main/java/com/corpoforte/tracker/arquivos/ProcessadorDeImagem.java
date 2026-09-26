package com.corpoforte.tracker.arquivos;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Toda imagem enviada passa por aqui antes de ser guardada:
 *
 * 1. O tipo e' decidido pelos BYTES do arquivo (assinatura do formato), nao
 *    pela extensao nem pelo Content-Type que o cliente declarou. So' JPEG e
 *    PNG.
 * 2. As dimensoes sao lidas do cabecalho ANTES de decodificar: um arquivo
 *    pequeno pode declarar 30000x30000 e estourar a memoria ao abrir
 *    ("bomba de descompressao").
 * 3. A orientacao do EXIF e' aplicada nos pixels (OrientacaoExif).
 * 4. A imagem e' re-codificada em JPEG a partir dos pixels. Isso descarta
 *    TODO metadado - inclusive a localizacao GPS: foto de treino em casa
 *    nao pode expor onde a pessoa mora. E redimensiona pro lado maximo
 *    pedido, com uma miniatura opcional pra listagem.
 *
 * Sem Spring e sem banco no meio: testavel chamando processar(...) direto.
 */
@Service
public class ProcessadorDeImagem {

    /** Limite por arquivo enviado. O servidor recusa antes (413, limite do
     * multipart), mas a regra mora aqui pra valer pra qualquer porta. */
    public static final int TAMANHO_MAXIMO_BYTES = 5 * 1024 * 1024;

    private static final long MAXIMO_DE_PIXELS = 50_000_000L;
    private static final float QUALIDADE_JPEG = 0.85f;

    public ImagemProcessada processar(byte[] original, int ladoMaximo, int ladoMiniatura) {
        if (original.length > TAMANHO_MAXIMO_BYTES) {
            throw invalida("Cada foto pode ter até 5 MB");
        }
        String formato = formatoPelosBytes(original);
        BufferedImage imagem = decodificar(original, formato);
        if ("jpeg".equals(formato)) {
            imagem = orientar(imagem, OrientacaoExif.ler(original));
        }
        BufferedImage principal = redimensionar(emRgb(imagem), ladoMaximo);
        byte[] miniatura = ladoMiniatura > 0 ? codificarJpeg(redimensionar(principal, ladoMiniatura)) : null;
        return new ImagemProcessada(codificarJpeg(principal), principal.getWidth(), principal.getHeight(), miniatura);
    }

    static String formatoPelosBytes(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        if (b.length >= png.length) {
            boolean ehPng = true;
            for (int i = 0; i < png.length; i++) {
                ehPng &= b[i] == png[i];
            }
            if (ehPng) {
                return "png";
            }
        }
        throw invalida("Envie fotos em JPEG ou PNG");
    }

    private static BufferedImage decodificar(byte[] original, String formato) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            Iterator<ImageReader> leitores = ImageIO.getImageReadersByFormatName(formato);
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                long pixels = (long) leitor.getWidth(0) * leitor.getHeight(0);
                if (pixels > MAXIMO_DE_PIXELS) {
                    throw invalida("Imagem grande demais");
                }
                return leitor.read(0);
            } finally {
                leitor.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ResponseStatusException invalida) {
                throw invalida;
            }
            throw invalida("Não foi possível ler a imagem");
        }
    }

    /**
     * Os 8 valores do EXIF: 2-4 espelham/giram 180, 5-8 giram 90 (e trocam
     * largura com altura). Rotacao de 90 graus e' exata, entao vizinho mais
     * proximo - nenhuma interpolacao borrando pixel.
     */
    static BufferedImage orientar(BufferedImage origem, int orientacao) {
        if (orientacao == OrientacaoExif.NORMAL) {
            return origem;
        }
        int w = origem.getWidth();
        int h = origem.getHeight();
        boolean troca = orientacao >= 5;
        AffineTransform t = switch (orientacao) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
            default -> new AffineTransform();
        };
        BufferedImage destino = new BufferedImage(troca ? h : w, troca ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(origem, t, null);
        } finally {
            g.dispose();
        }
        return destino;
    }

    /** JPEG nao tem transparencia: fundo branco onde o PNG era transparente. */
    private static BufferedImage emRgb(BufferedImage origem) {
        if (origem.getType() == BufferedImage.TYPE_INT_RGB) {
            return origem;
        }
        BufferedImage rgb = new BufferedImage(origem.getWidth(), origem.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(origem, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    /**
     * Reduz ate o maior lado caber em ladoMaximo (nunca aumenta). Em passos
     * de metade, pra reducao grande nao serrilhar como numa passada so'.
     */
    static BufferedImage redimensionar(BufferedImage origem, int ladoMaximo) {
        int maior = Math.max(origem.getWidth(), origem.getHeight());
        if (maior <= ladoMaximo) {
            return origem;
        }
        double escalaFinal = (double) ladoMaximo / maior;
        int larguraFinal = Math.max(1, (int) Math.round(origem.getWidth() * escalaFinal));
        int alturaFinal = Math.max(1, (int) Math.round(origem.getHeight() * escalaFinal));

        BufferedImage atual = origem;
        while (atual.getWidth() / 2 >= larguraFinal && atual.getHeight() / 2 >= alturaFinal) {
            atual = desenhar(atual, atual.getWidth() / 2, atual.getHeight() / 2);
        }
        return desenhar(atual, larguraFinal, alturaFinal);
    }

    private static BufferedImage desenhar(BufferedImage origem, int largura, int altura) {
        BufferedImage destino = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(origem, 0, 0, largura, altura, null);
        } finally {
            g.dispose();
        }
        return destino;
    }

    /** O writer do ImageIO grava so' o cabecalho JFIF: nenhum EXIF sai daqui. */
    private static byte[] codificarJpeg(BufferedImage imagem) {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try (ImageOutputStream destino = ImageIO.createImageOutputStream(saida)) {
            escritor.setOutput(destino);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(QUALIDADE_JPEG);
            escritor.write(null, new IIOImage(imagem, null, null), parametros);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao codificar JPEG", e);
        } finally {
            escritor.dispose();
        }
        return saida.toByteArray();
    }

    private static ResponseStatusException invalida(String detalhe) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detalhe);
    }
}
