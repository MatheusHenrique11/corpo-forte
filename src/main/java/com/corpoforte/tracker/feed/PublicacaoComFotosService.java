package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.arquivos.ImagemProcessada;
import com.corpoforte.tracker.arquivos.ProcessadorDeImagem;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.Visibilidade;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Regras do post com fotos (Fase 15), separadas do PostService pra o
 * processamento das imagens (pesado, de CPU) acontecer ANTES e FORA da
 * transacao: tudo e' validado e re-codificado primeiro, e so' entao
 * PostService.criarComFotos abre a transacao pra gravar. Uma foto invalida
 * recusa o post inteiro sem nada ter sido gravado.
 */
@Service
public class PublicacaoComFotosService {

    static final int MAXIMO_DE_FOTOS = 4;
    static final int LADO_MAXIMO = 1600;
    static final int LADO_MINIATURA = 400;

    private final ProcessadorDeImagem processadorDeImagem;
    private final PostService postService;

    public PublicacaoComFotosService(ProcessadorDeImagem processadorDeImagem, PostService postService) {
        this.processadorDeImagem = processadorDeImagem;
        this.postService = postService;
    }

    /** Post de foto pode vir sem legenda; o que nao pode e' vir sem nada. */
    public Post publicar(Usuario autor, String texto, Visibilidade visibilidade, List<byte[]> originais) {
        String legenda = texto == null ? "" : texto.strip();
        if (originais.size() > MAXIMO_DE_FOTOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No máximo 4 fotos por post");
        }
        if (legenda.isEmpty() && originais.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva algo ou envie ao menos uma foto");
        }

        List<ImagemProcessada> fotos = originais.stream()
                .map(original -> processadorDeImagem.processar(original, LADO_MAXIMO, LADO_MINIATURA))
                .toList();
        return postService.criarComFotos(autor, legenda, visibilidade, fotos);
    }
}
