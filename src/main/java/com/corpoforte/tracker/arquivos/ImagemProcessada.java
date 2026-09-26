package com.corpoforte.tracker.arquivos;

/** JPEG re-codificado (sem metadado nenhum) e, se pedida, a miniatura. */
public record ImagemProcessada(byte[] principal, int largura, int altura, byte[] miniatura) {
}
