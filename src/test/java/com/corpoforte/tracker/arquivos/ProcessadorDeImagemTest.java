package com.corpoforte.tracker.arquivos;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessadorDeImagemTest {

    private final ProcessadorDeImagem processador = new ProcessadorDeImagem();

    @Test
    void aceitaJpegEPngERecodificaEmJpeg() {
        ImagemProcessada deJpeg = processador.processar(ImagensDeTeste.jpeg(200, 100), 1600, 400);
        ImagemProcessada dePng = processador.processar(ImagensDeTeste.png(200, 100), 1600, 400);

        assertThat(ProcessadorDeImagem.formatoPelosBytes(deJpeg.principal())).isEqualTo("jpeg");
        assertThat(ProcessadorDeImagem.formatoPelosBytes(dePng.principal())).isEqualTo("jpeg");
    }

    /** O tipo vem dos bytes: um texto, um GIF ou um SVG sao recusados, seja
     * qual for o nome ou o Content-Type que o cliente mandou (o processador
     * nem recebe essas informacoes). */
    @Test
    void recusaQualquerCoisaQueNaoSejaJpegOuPngPeloConteudo() {
        assertInvalida("isto nao e uma foto".getBytes(StandardCharsets.UTF_8), "Envie fotos em JPEG ou PNG");
        assertInvalida("GIF89a....".getBytes(StandardCharsets.US_ASCII), "Envie fotos em JPEG ou PNG");
        assertInvalida("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8),
                "Envie fotos em JPEG ou PNG");
        // cabecalho de JPEG com o resto quebrado
        assertInvalida(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 1, 2, 3}, "Não foi possível ler a imagem");
    }

    /** O motivo da fase: foto de treino em casa nao pode levar a localizacao
     * junto. Nada do EXIF sobrevive a re-codificacao. */
    @Test
    void removeOExifInteiroInclusiveOGps() {
        byte[] comGps = ImagensDeTeste.jpegComExif(200, 100, 1);
        assertThat(ImagensDeTeste.contem(comGps, ImagensDeTeste.MARCA_DO_GPS)).isTrue();

        ImagemProcessada processada = processador.processar(comGps, 1600, 400);

        assertThat(ImagensDeTeste.contem(processada.principal(), ImagensDeTeste.MARCA_DO_GPS)).isFalse();
        assertThat(ImagensDeTeste.contem(processada.principal(), "Exif")).isFalse();
        assertThat(ImagensDeTeste.contem(processada.miniatura(), "Exif")).isFalse();
    }

    /** Orientation 6 = girar 90 graus no sentido horario: 200x100 vira
     * 100x200, e o canto vermelho (superior esquerdo) vai pro superior
     * direito. Sem isso a foto do celular apareceria deitada. */
    @Test
    void aplicaAOrientacaoDoExifNosPixelsAntesDeDescartarOExif() {
        ImagemProcessada processada = processador.processar(ImagensDeTeste.jpegComExif(200, 100, 6), 1600, 0);

        BufferedImage resultado = ImagensDeTeste.ler(processada.principal());
        assertThat(resultado.getWidth()).isEqualTo(100);
        assertThat(resultado.getHeight()).isEqualTo(200);
        assertThat(vermelho(resultado, 95, 5)).as("canto superior direito").isTrue();
        assertThat(vermelho(resultado, 5, 5)).as("canto superior esquerdo").isFalse();
    }

    @Test
    void reduzAteOLadoMaximoEGeraAMiniatura() {
        ImagemProcessada processada = processador.processar(ImagensDeTeste.jpeg(3000, 1000), 1600, 400);

        assertThat(processada.largura()).isEqualTo(1600);
        assertThat(processada.altura()).isEqualTo(533);
        BufferedImage miniatura = ImagensDeTeste.ler(processada.miniatura());
        assertThat(miniatura.getWidth()).isEqualTo(400);
        assertThat(miniatura.getHeight()).isEqualTo(133);
    }

    @Test
    void naoAumentaImagemPequenaENaoGeraMiniaturaSemPedir() {
        ImagemProcessada processada = processador.processar(ImagensDeTeste.jpeg(300, 200), 1600, 0);

        assertThat(processada.largura()).isEqualTo(300);
        assertThat(processada.altura()).isEqualTo(200);
        assertThat(processada.miniatura()).isNull();
    }

    /** PNG de poucos bytes que declara 30000x30000: recusado pelo cabecalho,
     * antes de tentar alocar a imagem na memoria. */
    @Test
    void recusaImagemGiganteSemDecodificar() {
        assertInvalida(pngQueDeclara(30000, 30000), "Imagem grande demais");
    }

    @Test
    void recusaArquivoAcimaDe5Mb() {
        byte[] grande = new byte[ProcessadorDeImagem.TAMANHO_MAXIMO_BYTES + 1];
        grande[0] = (byte) 0xFF;
        grande[1] = (byte) 0xD8;
        grande[2] = (byte) 0xFF;

        assertInvalida(grande, "Cada foto pode ter até 5 MB");
    }

    private void assertInvalida(byte[] conteudo, String detalhe) {
        assertThatThrownBy(() -> processador.processar(conteudo, 1600, 400))
                .isInstanceOfSatisfying(ResponseStatusException.class, erro -> {
                    assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(erro.getReason()).isEqualTo(detalhe);
                });
    }

    private static boolean vermelho(BufferedImage imagem, int x, int y) {
        int rgb = imagem.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        return r > 180 && g < 90;
    }

    private static byte[] pngQueDeclara(int largura, int altura) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        saida.writeBytes(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        ByteBuffer ihdr = ByteBuffer.allocate(4 + 13);
        ihdr.put("IHDR".getBytes(StandardCharsets.US_ASCII)).putInt(largura).putInt(altura)
                .put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
        CRC32 crc = new CRC32();
        crc.update(ihdr.array());
        saida.writeBytes(ByteBuffer.allocate(4).putInt(13).array());
        saida.writeBytes(ihdr.array());
        saida.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
        return saida.toByteArray();
    }
}
