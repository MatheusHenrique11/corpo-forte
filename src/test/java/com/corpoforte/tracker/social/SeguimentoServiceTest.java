package com.corpoforte.tracker.social;

import com.corpoforte.tracker.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unitario com repository mockado, pelo mesmo motivo do PostServiceTest:
 * o caso principal e' uma corrida entre duas requisicoes. */
class SeguimentoServiceTest {

    private final SeguimentoRepository seguimentoRepository = mock(SeguimentoRepository.class);
    private final SeguimentoService service = new SeguimentoService(seguimentoRepository, mock(UsuarioRepository.class));

    /** Duplo clique no "seguir": a segunda requisicao esbarra no unique e
     * isso nao pode virar 500 - o seguimento que a pessoa queria ja existe. */
    @Test
    void seguirConcorrenteQueColideNaConstraintNaoVira500() {
        when(seguimentoRepository.findBySeguidorIdAndSeguidoId(1L, 2L)).thenReturn(Optional.empty());
        when(seguimentoRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatCode(() -> service.seguir(1L, 2L)).doesNotThrowAnyException();
    }

    @Test
    void seguirQuemJaSegueNaoGravaDeNovo() {
        when(seguimentoRepository.findBySeguidorIdAndSeguidoId(1L, 2L))
                .thenReturn(Optional.of(new Seguimento(1L, 2L, LocalDateTime.now())));

        service.seguir(1L, 2L);

        verify(seguimentoRepository, never()).save(any());
    }

    @Test
    void seguirAPropriaContaDa400SemGravar() {
        assertThatThrownBy(() -> service.seguir(1L, 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(seguimentoRepository, never()).save(any());
    }

    @Test
    void deixarDeSeguirQuemNaoSegueNaoEErro() {
        when(seguimentoRepository.findBySeguidorIdAndSeguidoId(1L, 2L)).thenReturn(Optional.empty());

        assertThatCode(() -> service.deixarDeSeguir(1L, 2L)).doesNotThrowAnyException();

        verify(seguimentoRepository, never()).delete(any());
    }
}
