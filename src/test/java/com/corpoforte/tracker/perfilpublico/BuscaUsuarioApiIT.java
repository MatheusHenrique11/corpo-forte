package com.corpoforte.tracker.perfilpublico;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class BuscaUsuarioApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    private Usuario quemBusca;

    @BeforeEach
    void criarContas() {
        quemBusca = contaComOnboarding(OidcTestUsers.principal("sub-busca", "Quem Busca", "busca@exemplo.com"));
        contaComOnboarding(OidcTestUsers.principal("joana_silva", "Joana Silva", "joana@exemplo.com"));
        contaComOnboarding(OidcTestUsers.principal("joaox", "Joao Xavier", "joaox@exemplo.com"));
        contaComOnboarding(OidcTestUsers.principal("marcos", "Joaquim Marcos", "marcos@exemplo.com"));
    }

    @Test
    void achaPeloComecoDoUsernameOuDoNomeSemDiferenciarMaiusculas() throws Exception {
        // "JOA": username joana_silva e joaox; nome "Joaquim Marcos" (username marcos)
        assertThat(usernames("JOA")).containsExactly("joana_silva", "joaox", "marcos");
        assertThat(usernames("marc")).containsExactly("marcos");
    }

    /** "_" e' valido em username e e' curinga no like: "joa_" nao pode achar
     * "joaox". */
    @Test
    void curingasDoLikeSaoLiterais() throws Exception {
        assertThat(usernames("joa_")).isEmpty();
        assertThat(usernames("joana_")).containsExactly("joana_silva");
        assertThat(usernames("%")).isEmpty();
    }

    /** Conta sem onboarding nao tem username nem perfil publico pra abrir. */
    @Test
    void contaSemOnboardingNaoApareceNaBusca() throws Exception {
        usuarioAtualService.obterUsuarioAtual(OidcTestUsers.principal("sub-pendente", "Joaninha", "p@exemplo.com"));

        assertThat(usernames("joaninha")).isEmpty();
    }

    @Test
    void noMaximo20ResultadosEBuscaVaziaNaoLista() throws Exception {
        for (int i = 0; i < 22; i++) {
            contaComOnboarding(OidcTestUsers.principal("lote_" + i, "Lote " + i, "lote" + i + "@exemplo.com"));
        }

        assertThat(usernames("lote")).hasSize(20);
        assertThat(usernames("   ")).isEmpty();
    }

    private List<String> usernames(String busca) throws Exception {
        String resposta = mockMvc.perform(get("/api/v1/usuarios").param("busca", busca)
                        .header(HttpHeaders.AUTHORIZATION, bearer(quemBusca)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.itens[*].username");
    }
}
