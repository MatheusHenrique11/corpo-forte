package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.OidcTestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unitario (repository mockado, sem Spring/banco) porque simular a corrida
 * de verdade via Testcontainers exigiria duas threads reais disputando a
 * mesma conexao - determinismo baixo pro que da pra provar so com mocks.
 */
class UsuarioAtualServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final UsuarioAtualService service = new UsuarioAtualService(usuarioRepository, "");

    @Test
    void loginConcorrenteQueColideNaConstraintBuscaDeNovoEmVezDeQuebrarCom500() {
        OidcUser principal = OidcTestUsers.principal("sub-corrida", "Fulana", "fulana@exemplo.com");
        Usuario usuarioCriadoPelaOutraRequisicao = new Usuario(
                "Fulana", 100, 178, 27, ObjetivoTreino.PERDA_GORDURA, NivelTreino.INICIANTE, LocalDate.now());

        // 1a chamada: ninguem commitou ainda -> nao acha. 2a chamada (apos
        // capturar a excecao): a outra requisicao ja commitou -> acha.
        when(usuarioRepository.findByGoogleSub("sub-corrida"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(usuarioCriadoPelaOutraRequisicao));
        when(usuarioRepository.findByGoogleSubIsNull()).thenReturn(List.of());
        when(usuarioRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        Usuario resultado = service.obterUsuarioAtual(principal);

        assertThat(resultado).isSameAs(usuarioCriadoPelaOutraRequisicao);
    }
}
