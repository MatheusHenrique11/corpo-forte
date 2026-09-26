package com.corpoforte.tracker.arquivos;

/**
 * Le so' a tag Orientation (0x0112) do EXIF de um JPEG. Foto de celular
 * costuma vir gravada "deitada", com o EXIF dizendo como girar. Como a
 * re-codificacao joga fora todo o EXIF (que e' o objetivo: tirar o GPS), a
 * rotacao tem que ser aplicada nos pixels antes - senao a foto aparece de
 * lado.
 *
 * Leitura defensiva: qualquer estrutura inesperada devolve 1 (normal), em
 * vez de recusar a foto por causa de um metadado.
 */
final class OrientacaoExif {

    static final int NORMAL = 1;

    private OrientacaoExif() {
    }

    static int ler(byte[] jpeg) {
        try {
            return lerSemProteger(jpeg);
        } catch (RuntimeException e) {
            return NORMAL;
        }
    }

    private static int lerSemProteger(byte[] b) {
        int i = 2; // depois do SOI (FF D8)
        while (i + 4 <= b.length) {
            if ((b[i] & 0xFF) != 0xFF) {
                return NORMAL;
            }
            int marcador = b[i + 1] & 0xFF;
            if (marcador == 0xD9 || marcador == 0xDA) {
                return NORMAL; // fim da imagem / inicio dos dados: acabaram os cabecalhos
            }
            int tamanho = u16(b, i + 2, false);
            if (marcador == 0xE1 && ehExif(b, i + 4)) {
                return orientacaoNoTiff(b, i + 10);
            }
            i += 2 + tamanho;
        }
        return NORMAL;
    }

    private static boolean ehExif(byte[] b, int i) {
        return b[i] == 'E' && b[i + 1] == 'x' && b[i + 2] == 'i' && b[i + 3] == 'f' && b[i + 4] == 0 && b[i + 5] == 0;
    }

    private static int orientacaoNoTiff(byte[] b, int tiff) {
        boolean littleEndian = b[tiff] == 'I' && b[tiff + 1] == 'I';
        int ifd0 = tiff + (int) u32(b, tiff + 4, littleEndian);
        int entradas = u16(b, ifd0, littleEndian);
        for (int k = 0; k < entradas; k++) {
            int entrada = ifd0 + 2 + k * 12;
            if (u16(b, entrada, littleEndian) == 0x0112) {
                int valor = u16(b, entrada + 8, littleEndian);
                return valor >= 1 && valor <= 8 ? valor : NORMAL;
            }
        }
        return NORMAL;
    }

    private static int u16(byte[] b, int i, boolean littleEndian) {
        int alto = b[littleEndian ? i + 1 : i] & 0xFF;
        int baixo = b[littleEndian ? i : i + 1] & 0xFF;
        return (alto << 8) | baixo;
    }

    private static long u32(byte[] b, int i, boolean littleEndian) {
        long alto = u16(b, littleEndian ? i + 2 : i, littleEndian);
        long baixo = u16(b, littleEndian ? i : i + 2, littleEndian);
        return (alto << 16) | baixo;
    }
}
