package com.corpoforte.tracker.config;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisica;
import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaService;
import com.corpoforte.tracker.treino.TreinoDoDiaService;
import com.corpoforte.tracker.treino.TreinoDoDiaView;
import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoria (Fase 6, revisada depois de uma primeira varredura incompleta)
 * de todo endpoint que recebe ID de recurso vindo do cliente - o vetor
 * classico de IDOR (Insecure Direct Object Reference): um usuario
 * autenticado adivinha/incrementa o ID de outro pra ler ou mexer no
 * recurso alheio.
 *
 * A primeira varredura so procurou @PathVariable, que é o jeito mais
 * óbvio de um ID chegar mas não o mais comum num app com formulário
 * server-side - o clássico é um <input type="hidden" name="exercicioId">
 * num @ModelAttribute, ou um @RequestParam solto. Varredura completa,
 * cobrindo os quatro vetores:
 *
 *   @PathVariable    - grep em todos os controllers
 *   @RequestParam    - grep em todos os controllers: os únicos que existem
 *                       são NivelTreino/MovimentoPadrao/Equipamento em
 *                       ExercicioController (filtro sobre catálogo
 *                       compartilhado, não ID de entidade de outro usuário)
 *                       e Set<Equipamento> em EquipamentoController (grava
 *                       no próprio usuário resolvido pelo login, não lê
 *                       por ID). Nenhum é um ID de recurso.
 *   @ModelAttribute  - os 3 forms do app (PerfilForm, AvaliacaoFisicaForm,
 *                       RegistroPesoForm): nenhum campo termina em "Id" -
 *                       só valores (nome, altura, reps, data, peso etc.),
 *                       zero hidden input de ID.
 *   @RequestBody     - zero ocorrências no projeto inteiro (é tudo
 *                       Thymeleaf server-side, nenhuma API JSON).
 *
 * Levantamento completo, os 12 endpoints do app (grep por
 * @GetMapping/@PostMapping em todos os controllers):
 *
 *   GET  /                                             sem ID
 *   GET  /perfil                                       sem ID (usuario atual, resolvido pelo login)
 *   POST /perfil                                       sem ID (form sem campo Id)
 *   GET  /avaliacao                                    sem ID
 *   POST /avaliacao                                    sem ID (form sem campo Id)
 *   GET  /peso                                         sem ID
 *   POST /peso                                         sem ID (upsert por data, form sem campo Id)
 *   GET  /equipamentos                                 sem ID
 *   POST /equipamentos                                 sem ID (Set<Equipamento>, nao ID de entidade)
 *   GET  /exercicios                                   sem ID (@RequestParam so' de enum de filtro; catalogo compartilhado, nao e' dado por usuario)
 *   GET  /treino-do-dia                                sem ID
 *   POST /treino-do-dia/itens/{itemId}/concluir        RECEBE ID (@PathVariable)  <- unico endpoint desta lista
 *
 * Conclusao: so 1 dos 12 endpoints aceita um ID de recurso vindo do
 * cliente, confirmado nos quatro vetores (não só @PathVariable). Os outros
 * 11 operam exclusivamente sobre "o usuario atual" (Usuario resolvido via
 * UsuarioAtualService.obterUsuarioAtual(principal)) ou sobre enums de
 * filtro sem significado de ID, sem nenhum ID de entidade de outro usuario
 * exposto pro cliente manipular - entao nao tem outro endpoint pra
 * escrever teste de acesso cruzado.
 *
 * Quando uma fase futura adicionar um endpoint novo que receba ID por
 * qualquer um dos quatro vetores acima (ex.: editar um registro de peso
 * especifico por ID, remover um item do catalogo, um campo hidden
 * "itemId" num form), atualizar esta lista e acrescentar um teste de
 * acesso cruzado aqui, no mesmo padrao do que ja existe pra
 * /treino-do-dia/itens/{id}.
 */
@AutoConfigureMockMvc
@Transactional
class EndpointsComIdIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private AvaliacaoFisicaService avaliacaoFisicaService;

    @Autowired
    private TreinoDoDiaService treinoDoDiaService;

    private final OidcUser usuarioA = OidcTestUsers.principal("sub-endpoint-id-a", "Usuaria A", "a@exemplo.com");
    private final OidcUser usuarioB = OidcTestUsers.principal("sub-endpoint-id-b", "Usuario B", "b@exemplo.com");

    /**
     * POST /treino-do-dia/itens/{itemId}/concluir - o unico endpoint com ID
     * da lista acima. Usuario B tenta concluir um item que pertence ao
     * treino do usuario A; precisa de 404 (nao 200/302 de sucesso), e o
     * item de A precisa continuar intacto depois da tentativa.
     */
    @Test
    void concluirItemDeTreinoComIdDeOutroUsuarioDevolve404ENaoAlteraOItem() throws Exception {
        Usuario a = usuarioAtualService.obterUsuarioAtual(usuarioA);
        a.atualizarEquipamentos(Set.of(Equipamento.values()));
        usuarioAtualService.salvar(a);
        usuarioAtualService.obterUsuarioAtual(usuarioB);

        AvaliacaoFisica avaliacaoDeA = avaliacaoFisicaService.salvar(a.getId(), 10, 15, 40, 20, 30, 50);
        TreinoDoDiaView treinoDeA = treinoDoDiaService.obterOuGerarDoDia(a, avaliacaoDeA);
        Long itemDeA = treinoDeA.itens().get(0).itemId();

        mockMvc.perform(post("/treino-do-dia/itens/" + itemDeA + "/concluir")
                        .with(oidcLogin().oidcUser(usuarioB)).with(csrf()))
                .andExpect(status().isNotFound());

        TreinoDoDiaView treinoDeAAposTentativa = treinoDoDiaService.obterOuGerarDoDia(a, avaliacaoDeA);
        boolean aindaNaoConcluido = treinoDeAAposTentativa.itens().stream()
                .filter(item -> item.itemId().equals(itemDeA))
                .findFirst().orElseThrow().concluido();
        assertThat(aindaNaoConcluido).isFalse();
    }

    /**
     * Mesmo endpoint, ID que nao existe em lugar nenhum (nem de A nem de
     * B) - tambem tem que ser 404, nao 500/exception vazando detalhe
     * interno.
     */
    @Test
    void concluirItemComIdInexistenteDevolve404() throws Exception {
        usuarioAtualService.obterUsuarioAtual(usuarioA);

        mockMvc.perform(post("/treino-do-dia/itens/999999/concluir")
                        .with(oidcLogin().oidcUser(usuarioA)).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
