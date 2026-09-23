package com.corpoforte.tracker.auth;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * As duas garantias do refresh token que moram no banco, nao no service:
 * o hash e' unico (V13) e a revogacao e' um UPDATE condicional que so'
 * uma chamada vence. Esta ultima e' o que impede duas requisicoes de
 * refresh com o mesmo token de receberem, as duas, um par novo.
 */
@Transactional
class RefreshTokenRepositoryIT extends IntegrationTestBase {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario usuario;

    @BeforeEach
    void criarUsuario() {
        usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-refresh-repo", "Fulana", "refresh-repo@exemplo.com"));
    }

    @Test
    void bancoRejeitaDoisRefreshTokensComOMesmoHash() {
        LocalDateTime agora = LocalDateTime.now();
        refreshTokenRepository.saveAndFlush(new RefreshToken(usuario.getId(), "hash-repetido", agora, agora.plusDays(30)));

        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(
                new RefreshToken(usuario.getId(), "hash-repetido", agora, agora.plusDays(30))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void revogarSeAtivoSoVenceUmaVez() {
        LocalDateTime agora = LocalDateTime.now();
        RefreshToken token = refreshTokenRepository.save(
                new RefreshToken(usuario.getId(), "hash-unico", agora, agora.plusDays(30)));

        assertThat(refreshTokenRepository.revogarSeAtivo(token.getId(), agora)).isEqualTo(1);
        assertThat(refreshTokenRepository.revogarSeAtivo(token.getId(), agora)).isZero();
        assertThat(refreshTokenRepository.findByTokenHash("hash-unico")).get()
                .extracting(RefreshToken::revogado).isEqualTo(true);
    }

    /** Conta apagada leva as sessoes junto (on delete cascade): nenhum
     * refresh token sobrevive pra renovar acesso a uma conta que nao existe. */
    @Test
    void apagarAContaApagaAsSessoes() {
        LocalDateTime agora = LocalDateTime.now();
        refreshTokenRepository.save(new RefreshToken(usuario.getId(), "hash-da-conta-apagada", agora, agora.plusDays(30)));

        usuarioRepository.delete(usuario);
        entityManager.flush();
        entityManager.clear();

        assertThat(refreshTokenRepository.findByUsuarioId(usuario.getId())).isEmpty();
    }
}
