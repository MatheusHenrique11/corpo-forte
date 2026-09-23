package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisica;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaRepository;
import com.corpoforte.tracker.exercicio.MovimentoPadrao;
import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O acoplamento real da Fase 8: o checklist da Fase 5 (TreinoItem.concluido)
 * alimenta a progressao de volume. PeriodizacaoServiceTest cobre a regra de
 * contagem isolada; aqui e' o caminho inteiro - treino concluido no banco ->
 * treino de hoje gerado com mais repeticoes.
 *
 * As avaliacoes sao salvas direto pelo repository (nao pelo
 * AvaliacaoFisicaService, que sempre grava a data de hoje) porque os
 * cenarios precisam de uma avaliacao mais VELHA que os treinos: o ciclo e'
 * ancorado em dataAvaliacao, entao treino anterior a ela pertence ao ciclo
 * passado.
 *
 * Numeros de referencia: A=10 em puxar vertical -> volume inicial 24,
 * incremento 3. Semana 1 = 24/3 = 8 reps; uma semana progredida =
 * (24+3)/3 = 9 reps.
 */
@Transactional
class PeriodizacaoIT extends IntegrationTestBase {

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private AvaliacaoFisicaRepository avaliacaoFisicaRepository;

    @Autowired
    private TreinoDoDiaService treinoDoDiaService;

    @Autowired
    private TreinoDoDiaRepository treinoDoDiaRepository;

    @Autowired
    private TreinoItemRepository treinoItemRepository;

    private final OidcUser principal =
            OidcTestUsers.principal("sub-periodizacao", "Usuaria Teste", "periodizacao@exemplo.com");

    private Usuario usuarioComEquipamentoCompleto() {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(principal);
        usuario.atualizarEquipamentos(Set.of(Equipamento.values()));
        return usuarioAtualService.salvar(usuario);
    }

    private AvaliacaoFisica avaliacaoFeitaHa(Usuario usuario, int semanas) {
        return avaliacaoFisicaRepository.save(new AvaliacaoFisica(
                usuario.getId(), 10, 15, 40, 20, 30, 50, LocalDate.now().minusWeeks(semanas)));
    }

    private void concluirTreinoEm(Usuario usuario, LocalDate data) {
        TreinoDoDia treino = treinoDoDiaRepository.save(new TreinoDoDia(usuario.getId(), data));
        TreinoItem item = treinoItemRepository.save(new TreinoItem(treino.getId(), 1L, 3, 8));
        treinoDoDiaService.alternarConclusao(usuario.getId(), item.getId());
    }

    private int repeticoesDePuxarVertical(TreinoDoDiaView treino) {
        return treino.itens().stream()
                .filter(item -> item.movimento() == MovimentoPadrao.PUXAR_VERTICAL)
                .findFirst()
                .orElseThrow()
                .repeticoes();
    }

    @Test
    void semTreinoConcluidoOVolumeFicaNoInicial() {
        Usuario usuario = usuarioComEquipamentoCompleto();
        AvaliacaoFisica avaliacao = avaliacaoFeitaHa(usuario, 3);

        TreinoDoDiaView treino = treinoDoDiaService.obterOuGerarDoDia(usuario, avaliacao);

        assertThat(treino.ciclo().semanaAtual()).isEqualTo(1);
        assertThat(treino.ciclo().precisaReavaliar()).isFalse();
        assertThat(repeticoesDePuxarVertical(treino)).isEqualTo(8);
    }

    @Test
    void treinoConcluidoNaSemanaPassadaAumentaOVolumeDestaSemana() {
        Usuario usuario = usuarioComEquipamentoCompleto();
        AvaliacaoFisica avaliacao = avaliacaoFeitaHa(usuario, 3);

        concluirTreinoEm(usuario, LocalDate.now().minusWeeks(1));

        TreinoDoDiaView treino = treinoDoDiaService.obterOuGerarDoDia(usuario, avaliacao);

        assertThat(treino.ciclo().semanaAtual()).isEqualTo(2);
        assertThat(repeticoesDePuxarVertical(treino)).isEqualTo(9);
    }

    /**
     * Semana sem nenhum item concluido nao progride nada - a decisao de
     * produto desta fase vista de ponta a ponta, com dado real no banco.
     */
    @Test
    void treinoGeradoMasNaoConcluidoNaSemanaPassadaNaoProgride() {
        Usuario usuario = usuarioComEquipamentoCompleto();
        AvaliacaoFisica avaliacao = avaliacaoFeitaHa(usuario, 3);

        // treino existe na semana passada, mas nenhum item foi marcado
        treinoDoDiaRepository.save(new TreinoDoDia(usuario.getId(), LocalDate.now().minusWeeks(1)));

        TreinoDoDiaView treino = treinoDoDiaService.obterOuGerarDoDia(usuario, avaliacao);

        assertThat(treino.ciclo().semanaAtual()).isEqualTo(1);
        assertThat(repeticoesDePuxarVertical(treino)).isEqualTo(8);
    }

    /**
     * Refazer a avaliacao reinicia o ciclo: treino concluido ANTES da
     * medicao atual pertence ao ciclo passado e nao progride o novo. E' o
     * mecanismo de reset da Fase 8 - sem campo nem logica extra, so a
     * ancora em dataAvaliacao.
     */
    @Test
    void treinoConcluidoAntesDaAvaliacaoNaoContaNoCicloNovo() {
        Usuario usuario = usuarioComEquipamentoCompleto();

        concluirTreinoEm(usuario, LocalDate.now().minusWeeks(3));
        AvaliacaoFisica avaliacaoRefeita = avaliacaoFeitaHa(usuario, 1);

        TreinoDoDiaView treino = treinoDoDiaService.obterOuGerarDoDia(usuario, avaliacaoRefeita);

        assertThat(treino.ciclo().semanaAtual()).isEqualTo(1);
        assertThat(repeticoesDePuxarVertical(treino)).isEqualTo(8);
    }
}
