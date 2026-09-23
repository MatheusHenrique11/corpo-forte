package com.corpoforte.tracker.feed;

import java.time.LocalDateTime;
import java.util.List;

/**
 * curtidoPorMim e podeApagar sao as unicas coisas nesta view que dependem
 * de QUEM esta olhando - o resto do feed e' igual pra todo mundo (por isso
 * PostService.listarFeed recebe o usuario atual desde a Fase 7b).
 */
public record PostView(Long id, Long autorId, String autorNome, String texto, LocalDateTime criadoEm,
                       long curtidas, boolean curtidoPorMim, boolean podeApagar,
                       long totalComentarios, List<ComentarioView> comentarios) {
}
