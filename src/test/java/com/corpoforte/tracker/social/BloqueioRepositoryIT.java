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

/** As regras do bloqueio que moram no banco (V17), direto pelo repository. */
@Transactional
class BloqueioRepositoryIT extends IntegrationTestBase {

    @Autowired
    private BloqueioRepository bloqueioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario a;
    private Usuario b;

    @BeforeEach
    void criarContas() {
        a = contaComOnboarding(OidcTestUsers.principal("sub-bloq-repo-a", "A", "bloq-repo-a@exemplo.com"));
        b = contaComOnboarding(OidcTestUsers.principal("sub-bloq-repo-b", "B", "bloq-repo-b@exemplo.com"));
    }

    @Test
    void bancoRejeitaBloquearDuasVezes() {
        bloqueioRepository.saveAndFlush(new Bloqueio(a.getId(), b.getId(), LocalDateTime.now()));

        assertThatThrownBy(() -> bloqueioRepository.saveAndFlush(new Bloqueio(a.getId(), b.getId(), LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRejeitaBloquearASiMesmo() {
        assertThatThrownBy(() -> bloqueioRepository.saveAndFlush(new Bloqueio(a.getId(), a.getId(), LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** existeEntre olha os dois sentidos: e' a mesma pergunta da regra. */
    @Test
    void existeEntreValeNosDoisSentidosECascadeApagaComAConta() {
        bloqueioRepository.save(new Bloqueio(a.getId(), b.getId(), LocalDateTime.now()));

        assertThat(bloqueioRepository.existeEntre(a.getId(), b.getId())).isTrue();
        assertThat(bloqueioRepository.existeEntre(b.getId(), a.getId())).isTrue();

        usuarioRepository.delete(a);
        entityManager.flush();
        entityManager.clear();
        assertThat(bloqueioRepository.existeEntre(a.getId(), b.getId())).isFalse();
    }
}
