package com.corpoforte.tracker.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Sessao da API do lado do servidor. Guarda so' o hash do valor (ver
 * V13__create_refresh_token.sql). Revogar e' feito por UPDATE no
 * repository (RefreshTokenRepository.revogarSeAtivo), nao por setter
 * aqui: a revogacao precisa ser atomica no banco pra duas requisicoes nao
 * consumirem o mesmo token.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "revogado_em")
    private LocalDateTime revogadoEm;

    protected RefreshToken() {
    }

    public RefreshToken(Long usuarioId, String tokenHash, LocalDateTime criadoEm, LocalDateTime expiraEm) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean revogado() {
        return revogadoEm != null;
    }

    public boolean expirado(LocalDateTime agora) {
        return !agora.isBefore(expiraEm);
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

    public LocalDateTime getRevogadoEm() {
        return revogadoEm;
    }
}
