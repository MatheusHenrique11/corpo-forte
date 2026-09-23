package com.corpoforte.tracker.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh token opaco (32 bytes aleatorios), guardado so' como hash e
 * ROTATIVO: cada uso revoga o token usado e o cliente recebe um novo.
 *
 * Sem @Transactional aqui de proposito: cada chamada ao repository e' a
 * propria transacao. Numa transacao unica, o 401 lancado depois de
 * revogarTodosDoUsuario desfaria justamente a revogacao que protege a conta.
 */
@Service
public class RefreshTokenService {

    static final Duration VALIDADE = Duration.ofDays(30);

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** Devolve o valor em claro - a unica vez que ele existe no servidor. */
    public String emitir(Long usuarioId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        LocalDateTime agora = LocalDateTime.now();
        refreshTokenRepository.save(new RefreshToken(usuarioId, hash(valor), agora, agora.plus(VALIDADE)));
        return valor;
    }

    /**
     * Consome o token (revoga) e devolve o dono, pra quem chama emitir um
     * par novo.
     *
     * Token ja revogado aparecendo de novo e' tratado como ROUBO, nao como
     * erro comum: rotacao significa que o dono legitimo ja trocou esse
     * token por outro, entao quem o apresenta agora e' uma copia. A resposta
     * e' revogar todas as sessoes do usuario - o dono legitimo loga de novo,
     * e quem roubou perde o acesso junto. O mesmo vale pra quem perde a
     * corrida do UPDATE atomico (duas requisicoes com o mesmo token).
     */
    public Long consumir(String valor) {
        LocalDateTime agora = LocalDateTime.now();
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(valor))
                .orElseThrow(RefreshTokenService::invalido);

        if (token.revogado()) {
            refreshTokenRepository.revogarTodosDoUsuario(token.getUsuarioId(), agora);
            throw invalido();
        }
        if (token.expirado(agora)) {
            throw invalido();
        }
        if (refreshTokenRepository.revogarSeAtivo(token.getId(), agora) == 0) {
            refreshTokenRepository.revogarTodosDoUsuario(token.getUsuarioId(), agora);
            throw invalido();
        }
        return token.getUsuarioId();
    }

    /**
     * Logout. APAGA a linha em vez de marcar revogado_em: so' token
     * ROTACIONADO que reaparece indica roubo. Um token de logout
     * reapresentado (cliente com bug repetindo o refresh depois de sair)
     * cai em "token desconhecido" - 401 simples - em vez de derrubar as
     * sessoes da conta em outros aparelhos.
     *
     * Token desconhecido nao e' erro: o resultado que o cliente quer ("esta
     * sessao nao vale mais") ja e' verdade.
     */
    public void revogar(String valor) {
        refreshTokenRepository.apagarPorHash(hash(valor));
    }

    static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }

    private static ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido ou expirado");
    }
}
