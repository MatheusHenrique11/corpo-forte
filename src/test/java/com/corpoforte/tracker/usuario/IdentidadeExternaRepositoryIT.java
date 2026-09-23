package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * "Um login pertence a uma conta so'" tem que ser constraint de banco
 * (unique(provedor, sub) na V12), nao so' o buscarPorIdentidade do
 * UsuarioAtualService - e' o que segura dois primeiros logins simultaneos.
 * Herdou a garantia que usuario.google_sub unique dava desde a Fase 6.
 * Insere direto pelo repository, contornando o service de proposito, igual
 * aos testes de constraint de registro_peso, treino_do_dia e curtida.
 */
@Transactional
class IdentidadeExternaRepositoryIT extends IntegrationTestBase {

    @Autowired
    private IdentidadeExternaRepository identidadeExternaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void bancoRejeitaOMesmoLoginVinculadoADuasContas() {
        Usuario primeira = usuarioRepository.save(new Usuario("Primeira Conta", 70, 170, 25,
                ObjetivoTreino.PERDA_GORDURA, NivelTreino.INICIANTE));
        Usuario segunda = usuarioRepository.save(new Usuario("Segunda Conta", 80, 180, 30,
                ObjetivoTreino.GANHO_MASSA, NivelTreino.AVANCADO));

        identidadeExternaRepository.saveAndFlush(
                new IdentidadeExterna(primeira.getId(), Provedor.GOOGLE, "sub-duplicado", LocalDateTime.now()));

        assertThatThrownBy(() -> identidadeExternaRepository.saveAndFlush(
                new IdentidadeExterna(segunda.getId(), Provedor.GOOGLE, "sub-duplicado", LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** Apagar e' apagar: o login sai junto com a conta, pelo banco
     * (on delete cascade), sem delete em laco no service. */
    @Test
    void apagarAContaApagaOLoginVinculado() {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-conta-apagada", "Fulana", "apagada@exemplo.com"));

        usuarioRepository.delete(usuario);
        entityManager.flush();
        entityManager.clear();

        assertThat(identidadeExternaRepository.findByProvedorAndSub(Provedor.GOOGLE, "sub-conta-apagada")).isEmpty();
    }
}
