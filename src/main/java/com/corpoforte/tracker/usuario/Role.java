package com.corpoforte.tracker.usuario;

/**
 * Papel do usuario no sistema. Ainda nao usado para autorizacao (isso entra
 * na fase de login com Google), mas ja existe na entidade para nao precisar
 * migrar o schema depois.
 */
public enum Role {
    USER,
    ADMIN
}
