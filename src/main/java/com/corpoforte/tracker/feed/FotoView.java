package com.corpoforte.tracker.feed;

/** Foto de um post com as URLs ja assinadas pra quem esta vendo. */
public record FotoView(String url, String urlMiniatura, int largura, int altura) {
}
