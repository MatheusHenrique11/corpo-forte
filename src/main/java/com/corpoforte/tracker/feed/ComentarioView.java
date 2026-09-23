package com.corpoforte.tracker.feed;

import java.time.LocalDateTime;

public record ComentarioView(Long id, String autorNome, String texto, LocalDateTime criadoEm) {
}
