package com.corpoforte.tracker.usuario;

/** motivo null quando disponivel. */
public record DisponibilidadeUsernameResposta(String username, boolean disponivel,
                                              MotivoUsernameIndisponivel motivo) {
}
