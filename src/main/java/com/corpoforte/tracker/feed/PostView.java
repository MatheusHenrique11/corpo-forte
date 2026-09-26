package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.usuario.Visibilidade;

import java.time.LocalDateTime;
import java.util.List;

/**
 * curtidoPorMim e podeApagar sao as unicas coisas nesta view que dependem
 * de QUEM esta olhando - o resto do feed e' igual pra todo mundo (por isso
 * PostService.listarFeed recebe o usuario atual desde a Fase 7b).
 */
public record PostView(Long id, AutorView autor, String texto, LocalDateTime criadoEm, Visibilidade visibilidade,
                       long curtidas, boolean curtidoPorMim, boolean podeApagar,
                       long totalComentarios, List<ComentarioView> comentarios, List<FotoView> fotos) {

    /** O template da tela le o nome direto do post. */
    public String autorNome() {
        return autor.nome();
    }
}
