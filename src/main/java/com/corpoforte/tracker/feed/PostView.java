package com.corpoforte.tracker.feed;

import java.time.LocalDateTime;

public record PostView(Long id, String autorNome, String texto, LocalDateTime criadoEm) {
}
