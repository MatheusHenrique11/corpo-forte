package com.corpoforte.tracker.feed;

/**
 * Uma foto do post. As URLs sao assinadas e expiram (entre 1 e 2 horas):
 * o cliente usa direto num <img>, sem cabecalho de autenticacao, e pede o
 * post de novo pra ter URLs novas. urlMiniatura e' a versao pequena, pra
 * listagem.
 */
public record FotoResposta(String url, String urlMiniatura, int largura, int altura) {

    static FotoResposta de(FotoView foto) {
        return new FotoResposta(foto.url(), foto.urlMiniatura(), foto.largura(), foto.altura());
    }
}
