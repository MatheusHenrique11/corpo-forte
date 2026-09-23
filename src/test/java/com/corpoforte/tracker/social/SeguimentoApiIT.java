package com.corpoforte.tracker.social;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class SeguimentoApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeguimentoRepository seguimentoRepository;

    private Usuario ana;
    private Usuario bruno;

    @BeforeEach
    void criarContas() {
        ana = contaComOnboarding(OidcTestUsers.principal("sub-ana", "Ana", "ana@exemplo.com"));
        bruno = contaComOnboarding(OidcTestUsers.principal("sub-bruno", "Bruno", "bruno@exemplo.com"));
    }

    /** Assimetrico: Bruno segue Ana, e Ana nao passa a seguir Bruno. */
    @Test
    void seguirEDeixarDeSeguirSaoIdempotentesEAssimetricos() throws Exception {
        seguimento("PUT", bruno, "sub_ana").andExpect(status().isNoContent());
        seguimento("PUT", bruno, "sub_ana").andExpect(status().isNoContent());

        perfil("sub_ana", bruno)
                .andExpect(jsonPath("$.contagens.seguidores").value(1))
                .andExpect(jsonPath("$.contagens.seguindo").value(0))
                .andExpect(jsonPath("$.seguidoPorMim").value(true));
        perfil("sub_bruno", ana)
                .andExpect(jsonPath("$.contagens.seguindo").value(1))
                .andExpect(jsonPath("$.seguidoPorMim").value(false));

        seguimento("DELETE", bruno, "sub_ana").andExpect(status().isNoContent());
        seguimento("DELETE", bruno, "sub_ana").andExpect(status().isNoContent());
        perfil("sub_ana", bruno)
                .andExpect(jsonPath("$.contagens.seguidores").value(0))
                .andExpect(jsonPath("$.seguidoPorMim").value(false));
    }

    @Test
    void seguirAPropriaContaDa400EUsernameInexistente404() throws Exception {
        seguimento("PUT", ana, "sub_ana").andExpect(status().isBadRequest());
        seguimento("PUT", ana, "ninguem_tem").andExpect(status().isNotFound());
        seguimento("DELETE", ana, "ninguem_tem").andExpect(status().isNotFound());
    }

    /**
     * 21 seguidores: duas paginas, quem seguiu por ultimo primeiro, e
     * seguidoPorMim marcando as contas que QUEM PEDE segue (nao a dona da
     * lista).
     */
    @Test
    void listaDeSeguidoresPaginadaComOBotaoDeQuemVe() throws Exception {
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 12, 0);
        List<Usuario> seguidores = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            Usuario seguidor = contaComOnboarding(OidcTestUsers.principal(
                    "sub-seguidor-" + i, "Seguidor " + i, "seguidor" + i + "@exemplo.com"));
            seguimentoRepository.save(new Seguimento(seguidor.getId(), ana.getId(), base.plusMinutes(i)));
            seguidores.add(seguidor);
        }
        // Bruno segue so' o seguidor mais recente
        seguimentoRepository.save(new Seguimento(bruno.getId(), seguidores.get(20).getId(), base));

        String pagina1 = lista("/api/v1/usuarios/sub_ana/seguidores", null);
        String pagina2 = lista("/api/v1/usuarios/sub_ana/seguidores", JsonPath.read(pagina1, "$.proximoCursor"));

        assertThat(JsonPath.<List<String>>read(pagina1, "$.itens[*].username"))
                .hasSize(20).first().isEqualTo("sub_seguidor_20");
        assertThat(JsonPath.<List<Boolean>>read(pagina1, "$.itens[*].seguidoPorMim"))
                .containsExactly(true, false, false, false, false, false, false, false, false, false,
                        false, false, false, false, false, false, false, false, false, false);
        assertThat(JsonPath.<List<String>>read(pagina2, "$.itens[*].username")).containsExactly("sub_seguidor_0");
    }

    @Test
    void listaDeSeguindoMostraQuemAContaSegue() throws Exception {
        seguimento("PUT", ana, "sub_bruno").andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/usuarios/sub_ana/seguindo").header(HttpHeaders.AUTHORIZATION, bearer(bruno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].username").value("sub_bruno"))
                .andExpect(jsonPath("$.itens[0].pesoKg").doesNotExist());
    }

    private ResultActions seguimento(String verbo, Usuario quem, String username) throws Exception {
        String rota = "/api/v1/usuarios/" + username + "/seguimento";
        return mockMvc.perform((verbo.equals("PUT") ? put(rota) : delete(rota))
                .header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    private ResultActions perfil(String username, Usuario quemVe) throws Exception {
        return mockMvc.perform(get("/api/v1/usuarios/" + username).header(HttpHeaders.AUTHORIZATION, bearer(quemVe)))
                .andExpect(status().isOk());
    }

    private String lista(String rota, String cursor) throws Exception {
        var requisicao = get(rota).header(HttpHeaders.AUTHORIZATION, bearer(bruno));
        if (cursor != null) {
            requisicao.param("cursor", cursor);
        }
        return mockMvc.perform(requisicao).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
