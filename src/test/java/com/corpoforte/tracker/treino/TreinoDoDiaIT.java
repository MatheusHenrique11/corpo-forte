package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisica;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaService;
import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Sorteado uma vez por dia, fica fixo ate amanha" e a sincronizacao do
 * checklist sao os pontos de acoplamento que um teste unitario do gerador
 * nao cobre (mesmo raciocinio do RegistroPesoIntegrationIT da Fase 3).
 */
@AutoConfigureMockMvc
@Transactional
class TreinoDoDiaIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private AvaliacaoFisicaService avaliacaoFisicaService;

    @Autowired
    private TreinoDoDiaService treinoDoDiaService;

    private Usuario usuarioComEquipamentoCompleto() {
        Usuario usuario = usuarioAtualService.obterOuCriarPadrao();
        usuario.atualizarEquipamentos(Set.of(Equipamento.values()));
        return usuarioAtualService.salvar(usuario);
    }

    @Test
    void geraOTreinoUmaVezEDevolveOsMesmosItensNaSegundaChamadaNoMesmoDia() {
        Usuario usuario = usuarioComEquipamentoCompleto();
        AvaliacaoFisica avaliacao = avaliacaoFisicaService.salvar(usuario.getId(), 10, 15, 40, 20, 30, 50);

        TreinoDoDiaView primeira = treinoDoDiaService.obterOuGerarDoDia(usuario, avaliacao);
        TreinoDoDiaView segunda = treinoDoDiaService.obterOuGerarDoDia(usuario, avaliacao);

        assertThat(primeira.itens()).hasSize(6);
        List<Long> idsPrimeira = primeira.itens().stream().map(TreinoItemView::itemId).toList();
        List<Long> idsSegunda = segunda.itens().stream().map(TreinoItemView::itemId).toList();
        assertThat(idsSegunda).containsExactlyInAnyOrderElementsOf(idsPrimeira);
    }

    @Test
    void alternarConclusaoPersisteOEstadoDoChecklist() {
        Usuario usuario = usuarioComEquipamentoCompleto();
        AvaliacaoFisica avaliacao = avaliacaoFisicaService.salvar(usuario.getId(), 10, 15, 40, 20, 30, 50);
        TreinoDoDiaView treino = treinoDoDiaService.obterOuGerarDoDia(usuario, avaliacao);
        Long itemId = treino.itens().get(0).itemId();

        treinoDoDiaService.alternarConclusao(itemId);

        TreinoDoDiaView atualizado = treinoDoDiaService.obterOuGerarDoDia(usuario, avaliacao);
        boolean concluido = atualizado.itens().stream()
                .filter(item -> item.itemId().equals(itemId))
                .findFirst().orElseThrow().concluido();
        assertThat(concluido).isTrue();
    }

    @Test
    void semAvaliacaoFisicaMostraMensagemEmVezDeGerarTreino() throws Exception {
        usuarioAtualService.obterOuCriarPadrao();

        mockMvc.perform(get("/treino-do-dia"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("avaliação física")));
    }

    @Test
    void concluirViaControllerRedirecionaDeVoltaProTreinoDoDia() throws Exception {
        Usuario usuario = usuarioComEquipamentoCompleto();
        AvaliacaoFisica avaliacao = avaliacaoFisicaService.salvar(usuario.getId(), 10, 15, 40, 20, 30, 50);
        Long itemId = treinoDoDiaService.obterOuGerarDoDia(usuario, avaliacao).itens().get(0).itemId();

        mockMvc.perform(post("/treino-do-dia/itens/" + itemId + "/concluir"))
                .andExpect(status().is3xxRedirection());
    }
}
