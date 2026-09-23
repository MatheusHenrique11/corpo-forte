package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * As duas regras de username que moram no banco (V15), provadas direto
 * pelo repository, contornando UsernameService de proposito - mesmo padrao
 * dos outros testes de constraint do projeto.
 */
@Transactional
class UsernameRepositoryIT extends IntegrationTestBase {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /** O indice e' em lower(username): mesmo que um dia o formato aceite
     * maiusculas, "Joao" e "joao" nunca sao duas contas. */
    @Test
    void bancoRejeitaOMesmoUsernameComCaixaDiferente() {
        usuarioRepository.saveAndFlush(contaCom("joao"));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(contaCom("JOAO")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** Conta com onboarding concluido sem username nao teria endereco de
     * perfil publico: o check da V15 recusa. */
    @Test
    void bancoRecusaOnboardingConcluidoSemUsername() {
        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(contaCom(null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Usuario contaCom(String username) {
        Usuario usuario = new Usuario("Conta", 70, 170, 25, ObjetivoTreino.PERDA_GORDURA, NivelTreino.INICIANTE);
        usuario.concluirOnboarding("Conta", username, 170, 25, ObjetivoTreino.PERDA_GORDURA, NivelTreino.INICIANTE);
        return usuario;
    }
}
