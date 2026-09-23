package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mesmo padrao do RegistroPesoRepositoryIT (Fase 3): confirma que "um
 * treino por dia" tambem e' uma constraint de verdade no banco, nao so uma
 * regra checada no service antes de inserir.
 */
@Transactional
class TreinoDoDiaRepositoryIT extends IntegrationTestBase {

    @Autowired
    private TreinoDoDiaRepository treinoDoDiaRepository;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Test
    void bancoRejeitaDoisTreinosNoMesmoDiaParaOMesmoUsuario() {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-treino-constraint", "Usuaria Teste", "teste@exemplo.com"));
        LocalDate data = LocalDate.of(2026, 4, 1);

        treinoDoDiaRepository.saveAndFlush(new TreinoDoDia(usuario.getId(), data));

        assertThatThrownBy(() ->
                treinoDoDiaRepository.saveAndFlush(new TreinoDoDia(usuario.getId(), data))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
