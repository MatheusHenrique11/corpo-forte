package com.corpoforte.tracker.feed;

import java.time.LocalDateTime;
import java.util.List;

/**
 * curtidoPorMim e' a unica coisa nesta view que depende de QUEM esta
 * olhando - o resto do feed e' igual pra todo mundo (por isso
 * PostService.listarFeed passou a receber o usuario atual na Fase 7b).
 */
public record PostView(Long id, String autorNome, String texto, LocalDateTime criadoEm,
                       long curtidas, boolean curtidoPorMim, List<ComentarioView> comentarios) {
}
