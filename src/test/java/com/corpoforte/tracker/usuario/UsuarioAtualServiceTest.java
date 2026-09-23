package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.OidcTestUsers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unitario (repositories mockados, sem Spring/banco) porque simular a
 * corrida de verdade via Testcontainers exigiria duas threads reais
 * disputando a mesma conexao - determinismo baixo pro que da pra provar so
 * com mocks.
 */
class UsuarioAtualServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final IdentidadeExternaRepository identidadeExternaRepository = mock(IdentidadeExternaRepository.class);
    private final UsuarioAtualService service = new UsuarioAtualService(
            usuarioRepository, identidadeExternaRepository, TransactionOperations.withoutTransaction(), "");

    @Test
    void loginConcorrenteQueColideNaConstraintBuscaDeNovoEmVezDeQuebrarCom500() {
        OidcUser principal = OidcTestUsers.principal("sub-corrida", "Fulana", "fulana@exemplo.com");
        Usuario usuarioCriadoPelaOutraRequisicao = new Usuario(
                "Fulana", 100, 178, 27, ObjetivoTreino.PERDA_GORDURA, NivelTreino.INICIANTE);

        // 1a busca: ninguem commitou ainda -> nao acha. 2a busca (apos
        // capturar a excecao): a outra requisicao ja commitou -> acha.
        when(identidadeExternaRepository.findByProvedorAndSub(Provedor.GOOGLE, "sub-corrida"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new IdentidadeExterna(42L, Provedor.GOOGLE, "sub-corrida", LocalDateTime.now())));
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(usuarioCriadoPelaOutraRequisicao));
        when(usuarioRepository.findSemIdentidadeExterna()).thenReturn(List.of());
        when(usuarioRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        // quem colide agora e' o unique(provedor, sub) de identidade_externa
        when(identidadeExternaRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        Usuario resultado = service.obterUsuarioAtual(principal);

        assertThat(resultado).isSameAs(usuarioCriadoPelaOutraRequisicao);
    }

    /**
     * Na corrida acima, a requisicao perdedora ja gravou um Usuario quando
     * a identidade colide. Se as duas gravacoes nao estiverem na mesma
     * transacao, esse Usuario fica no banco sem identidade nenhuma - e vira
     * "conta orfa reivindicavel" pro dono configurado.
     */
    @Test
    void usuarioEIdentidadeDoPrimeiroLoginSaoGravadosNaMesmaTransacao() {
        List<Boolean> gravacoesDentroDaTransacao = new ArrayList<>();
        boolean[] emTransacao = {false};
        TransactionOperations transacaoRegistrada = new TransactionOperations() {
            @Override
            public <T> T execute(TransactionCallback<T> acao) {
                emTransacao[0] = true;
                try {
                    return acao.doInTransaction(new SimpleTransactionStatus());
                } finally {
                    emTransacao[0] = false;
                }
            }
        };
        UsuarioAtualService comTransacao = new UsuarioAtualService(
                usuarioRepository, identidadeExternaRepository, transacaoRegistrada, "");

        when(identidadeExternaRepository.findByProvedorAndSub(any(), any())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any())).thenAnswer(invocacao -> {
            gravacoesDentroDaTransacao.add(emTransacao[0]);
            return invocacao.getArgument(0);
        });
        when(identidadeExternaRepository.save(any())).thenAnswer(invocacao -> {
            gravacoesDentroDaTransacao.add(emTransacao[0]);
            return invocacao.getArgument(0);
        });

        comTransacao.obterOuCriar(new DadosLogin(Provedor.GOOGLE, "sub-novo", "novo@exemplo.com", true, "Novo"));

        assertThat(gravacoesDentroDaTransacao).containsExactly(true, true);
    }

    @Test
    void loginSemNomeDoProvedorCriaContaComOInicioDoEmail() {
        assertThat(nomeDaContaCriadaPara(new DadosLogin(Provedor.GOOGLE, "sub-1", "fulana@exemplo.com", true, null)))
                .isEqualTo("fulana");
        assertThat(nomeDaContaCriadaPara(new DadosLogin(Provedor.GOOGLE, "sub-2", null, false, " ")))
                .isEqualTo("Atleta");
    }

    @Test
    void nomeMaiorQueAColunaECortadoEmVezDeQuebrarOPrimeiroLogin() {
        String nomeLongo = "A".repeat(200);

        assertThat(nomeDaContaCriadaPara(new DadosLogin(Provedor.GOOGLE, "sub-3", "a@exemplo.com", true, nomeLongo)))
                .hasSize(120);
    }

    private String nomeDaContaCriadaPara(DadosLogin login) {
        when(identidadeExternaRepository.findByProvedorAndSub(any(), any())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        service.obterOuCriar(login);

        ArgumentCaptor<Usuario> criado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, atLeastOnce()).save(criado.capture());
        return criado.getValue().getNome();
    }
}
