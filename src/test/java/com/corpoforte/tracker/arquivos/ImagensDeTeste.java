package com.corpoforte.tracker.arquivos;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Imagens pros testes, montadas na hora (nada de arquivo binario no
 * repositorio). A imagem e' branca com um quadrado vermelho no canto
 * superior esquerdo: da pra conferir pra onde ele foi depois de aplicar a
 * orientacao.
 */
public final class ImagensDeTeste {

    /** Texto que vai dentro do bloco GPS do EXIF montado aqui - se aparecer
     * na saida, o GPS vazou. */
    public static final String MARCA_DO_GPS = "LOCALIZACAO-DA-CASA";

    private ImagensDeTeste() {
    }

    public static byte[] jpeg(int largura, int altura) {
        return codificar(imagem(largura, altura), "jpeg");
    }

    public static byte[] png(int largura, int altura) {
        return codificar(imagem(largura, altura), "png");
    }

    /**
     * JPEG com um bloco EXIF (APP1) logo depois do SOI, como sai de camera
     * de celular: tag Orientation e um bloco GPS com MARCA_DO_GPS.
     */
    public static byte[] jpegComExif(int largura, int altura, int orientacao) {
        byte[] original = jpeg(largura, altura);
        byte[] tiff = tiffComOrientacaoEGps(orientacao);
        byte[] exif = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);
        int tamanhoDoSegmento = 2 + exif.length + tiff.length;

        ByteBuffer saida = ByteBuffer.allocate(original.length + 2 + tamanhoDoSegmento);
        saida.put(original, 0, 2); // SOI
        saida.put((byte) 0xFF).put((byte) 0xE1).putShort((short) tamanhoDoSegmento).put(exif).put(tiff);
        saida.put(original, 2, original.length - 2);
        return saida.array();
    }

    /** TIFF big-endian: IFD0 com Orientation e o ponteiro pro IFD do GPS;
     * IFD do GPS com uma tag ASCII carregando MARCA_DO_GPS. */
    private static byte[] tiffComOrientacaoEGps(int orientacao) {
        byte[] marca = (MARCA_DO_GPS + "\0").getBytes(StandardCharsets.US_ASCII);
        int ifd0 = 8;
        int ifdGps = ifd0 + 2 + 2 * 12 + 4;
        int textoGps = ifdGps + 2 + 12 + 4;
        ByteBuffer b = ByteBuffer.allocate(textoGps + marca.length);
        b.put((byte) 'M').put((byte) 'M').putShort((short) 42).putInt(ifd0);
        b.putShort((short) 2);
        b.putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientacao).putShort((short) 0);
        b.putShort((short) 0x8825).putShort((short) 4).putInt(1).putInt(ifdGps);
        b.putInt(0);
        b.putShort((short) 1);
        b.putShort((short) 0x001B).putShort((short) 7).putInt(marca.length).putInt(textoGps);
        b.putInt(0);
        b.put(marca);
        return b.array();
    }

    private static BufferedImage imagem(int largura, int altura) {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, largura, altura);
        g.setColor(Color.RED);
        g.fillRect(0, 0, Math.max(1, largura / 4), Math.max(1, altura / 4));
        g.dispose();
        return imagem;
    }

    private static byte[] codificar(BufferedImage imagem, String formato) {
        try {
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            ImageIO.write(imagem, formato, saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static BufferedImage ler(byte[] imagem) {
        try {
            return ImageIO.read(new java.io.ByteArrayInputStream(imagem));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static boolean contem(byte[] conteudo, String texto) {
        return new String(conteudo, StandardCharsets.ISO_8859_1).contains(texto);
    }
}
