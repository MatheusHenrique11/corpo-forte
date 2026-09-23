package com.corpoforte.tracker.social;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
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
 * As regras do seguir que moram no banco (V16), direto pelo repository,
 * contornando o SeguimentoService de proposito.
 */
@Transactional
class SeguimentoRepositoryIT extends IntegrationTestBase {

    @Autowired
    private SeguimentoRepository seguimentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario a;
    private Usuario b;

    @BeforeEach
    void criarContas() {
        a = contaComOnboarding(OidcTestUsers.principal("sub-seg-repo-a", "A", "seg-repo-a@exemplo.com"));
        b = contaComOnboarding(OidcTestUsers.principal("sub-seg-repo-b", "B", "seg-repo-b@exemplo.com"));
    }

    @Test
    void bancoRejeitaSeguirDuasVezes() {
        seguimentoRepository.saveAndFlush(new Seguimento(a.getId(), b.getId(), LocalDateTime.now()));

        assertThatThrownBy(() -> seguimentoRepository.saveAndFlush(
                new Seguimento(a.getId(), b.getId(), LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRejeitaSeguirASiMesmo() {
        assertThatThrownBy(() -> seguimentoRepository.saveAndFlush(
                new Seguimento(a.getId(), a.getId(), LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** Conta apagada sai das listas de todo mundo (on delete cascade nas
     * duas pontas). */
    @Test
    void apagarContaApagaOsSeguimentosNasDuasPontas() {
        Usuario c = contaComOnboarding(OidcTestUsers.principal("sub-seg-repo-c", "C", "seg-repo-c@exemplo.com"));
        seguimentoRepository.save(new Seguimento(a.getId(), b.getId(), LocalDateTime.now()));
        seguimentoRepository.save(new Seguimento(b.getId(), c.getId(), LocalDateTime.now()));

        usuarioRepository.delete(b);
        entityManager.flush();
        entityManager.clear();

        assertThat(seguimentoRepository.countBySeguidorId(a.getId())).isZero();
        assertThat(seguimentoRepository.countBySeguidoId(c.getId())).isZero();
    }
}
