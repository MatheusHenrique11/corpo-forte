package com.corpoforte.tracker.onboarding;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.api.PermitidoSemOnboarding;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onboarding (Fase 12): conta nova nasce barrada na API ate trocar os
 * dados inventados pelos reais.
 */
@AutoConfigureMockMvc
@Transactional
class OnboardingApiIT extends IntegrationTestBase {

    private static final String TIPO_PENDENTE = "urn:corpo-forte:problema:onboarding-pendente";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeamentos;

    private Usuario pendente;

    @BeforeEach
    void criarContaNova() {
        pendente = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-onboarding", "Fulana", "onboarding@exemplo.com"));
    }

    @Test
    void contaNovaNasceComOnboardingPendente() throws Exception {
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(pendente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingConcluido").value(false))
                .andExpect(jsonPath("$.username").doesNotExist());
    }

    /**
     * Varre TODAS as rotas da API registradas no Spring, em vez de uma lista
     * escrita a mao: rota nova entra neste teste sozinha. Toda rota sem
     * @PermitidoSemOnboarding (e fora de /auth, que nem tem conta) tem que
     * responder onboarding-pendente pra conta pendente - antes de validar
     * corpo ou procurar recurso.
     */
    @Test
    void todaRotaDaApiNaoLiberadaRespondeOnboardingPendente() throws Exception {
        List<String> barradas = new ArrayList<>();
        for (Rota rota : rotasDaApi()) {
            if (rota.liberada()) {
                continue;
            }
            chamar(rota)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value(TIPO_PENDENTE));
            barradas.add(rota.toString());
        }
        // sanidade: a varredura achou as rotas das fases anteriores
        assertThat(barradas).contains("GET /api/v1/perfil", "GET /api/v1/feed/descobrir", "POST /api/v1/posts",
                "GET /api/v1/usuarios/{username}", "PUT /api/v1/perfil/publico");
    }

    /** Liberar rota e' decisao consciente: a lista de liberadas so' muda
     * junto com este teste. */
    @Test
    void soAContaOOnboardingEAChecagemDeUsernameSaoLiberados() {
        assertThat(rotasDaApi().stream().filter(Rota::liberada).map(Rota::toString))
                .containsExactlyInAnyOrder("GET /api/v1/me", "POST /api/v1/onboarding",
                        "GET /api/v1/usernames/{username}/disponivel");
    }

    @Test
    void telasWebNaoSaoBarradas() throws Exception {
        mockMvc.perform(get("/perfil").with(oidcLogin().oidcUser(
                        OidcTestUsers.principal("sub-onboarding", "Fulana", "onboarding@exemplo.com"))))
                .andExpect(status().isOk());
    }

    /**
     * Os dados inventados (100 kg, 178 cm, 27 anos) saem, os reais entram,
     * e o peso vira o primeiro registro de peso - nao um valor solto no
     * perfil (fonte unica desde a Fase 3).
     */
    @Test
    void concluirTrocaOsDadosInventadosEOPesoViraOPrimeiroRegistro() throws Exception {
        onboarding("fulana.silva", 72.5)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingConcluido").value(true))
                .andExpect(jsonPath("$.username").value("fulana.silva"));

        mockMvc.perform(get("/api/v1/perfil").header(HttpHeaders.AUTHORIZATION, bearer(pendente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fulana Silva"))
                .andExpect(jsonPath("$.alturaCm").value(165.0))
                .andExpect(jsonPath("$.idade").value(31))
                .andExpect(jsonPath("$.pesoKg").value(72.5));
        mockMvc.perform(get("/api/v1/pesos").header(HttpHeaders.AUTHORIZATION, bearer(pendente)))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].data").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.itens[0].pesoKg").value(72.5));
    }

    @Test
    void onboardingSoAconteceUmaVez() throws Exception {
        onboarding("fulana.silva", 72.5).andExpect(status().isOk());

        onboarding("outro.nome", 80)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:corpo-forte:problema:onboarding-ja-concluido"));
    }

    @Test
    void usernameDeOutraContaResponde409ENaoConcluiNada() throws Exception {
        contaComOnboarding(OidcTestUsers.principal("sub-dono-do-nome", "Dono", "dono@exemplo.com"));

        onboarding("sub_dono_do_nome", 72.5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:corpo-forte:problema:username-indisponivel"));
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(pendente)))
                .andExpect(jsonPath("$.onboardingConcluido").value(false));
    }

    @Test
    void validaUsernameEOsMesmosCamposDoPerfilEDoPeso() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding")
                        .header(HttpHeaders.AUTHORIZATION, bearer(pendente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Fulana", "username": "admin", "alturaCm": 50, "idade": 31,
                                 "objetivo": "GANHO_MASSA", "nivel": "INICIANTE", "pesoKg": 10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", containsInAnyOrder("username", "alturaCm", "pesoKg")));

        mockMvc.perform(post("/api/v1/onboarding")
                        .header(HttpHeaders.AUTHORIZATION, bearer(pendente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Fulana", "username": "Fulana!", "alturaCm": 165, "idade": 31,
                                 "objetivo": "GANHO_MASSA", "nivel": "INICIANTE", "pesoKg": 70}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].mensagem")
                        .value("Use de 3 a 30 caracteres: letras minúsculas, números, _ e ."));
    }

    @Test
    void disponibilidadeDeUsernameExplicaOMotivo() throws Exception {
        Usuario outra = contaComOnboarding(OidcTestUsers.principal("sub-outra-conta", "Outra", "outra@exemplo.com"));

        disponivel("nome.livre").andExpect(jsonPath("$.disponivel").value(true))
                .andExpect(jsonPath("$.motivo").doesNotExist());
        disponivel("ab").andExpect(jsonPath("$.motivo").value("FORMATO_INVALIDO"));
        disponivel("admin").andExpect(jsonPath("$.motivo").value("RESERVADO"));
        disponivel(outra.getUsername()).andExpect(jsonPath("$.disponivel").value(false))
                .andExpect(jsonPath("$.motivo").value("EM_USO"));

        // o nome que ja e' da propria conta nao conta como "em uso"
        mockMvc.perform(get("/api/v1/usernames/" + outra.getUsername() + "/disponivel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(outra)))
                .andExpect(jsonPath("$.disponivel").value(true));
    }

    private ResultActions onboarding(String username, double pesoKg) throws Exception {
        return mockMvc.perform(post("/api/v1/onboarding")
                .header(HttpHeaders.AUTHORIZATION, bearer(pendente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome": "Fulana Silva", "username": "%s", "alturaCm": 165, "idade": 31,
                         "objetivo": "GANHO_MASSA", "nivel": "INTERMEDIARIO", "pesoKg": %s}
                        """.formatted(username, pesoKg)));
    }

    private ResultActions disponivel(String username) throws Exception {
        return mockMvc.perform(get("/api/v1/usernames/" + username + "/disponivel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(pendente)))
                .andExpect(status().isOk());
    }

    /** Cada rota com o tipo de corpo que ela aceita (JSON ou, desde a Fase
     * 15, multipart) - senao o 415 viria antes do bloqueio. */
    private ResultActions chamar(Rota rota) throws Exception {
        String url = rota.caminho().replaceAll("\\{[^}]+}", "1");
        HttpMethod metodo = HttpMethod.valueOf(rota.metodo().name());
        if (rota.multipart()) {
            return mockMvc.perform(multipart(metodo, url).header(HttpHeaders.AUTHORIZATION, bearer(pendente)));
        }
        return mockMvc.perform(request(metodo, url)
                .header(HttpHeaders.AUTHORIZATION, bearer(pendente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
    }

    private List<Rota> rotasDaApi() {
        List<Rota> rotas = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> mapeamento : mapeamentos.getHandlerMethods().entrySet()) {
            HandlerMethod metodo = mapeamento.getValue();
            boolean liberada = metodo.hasMethodAnnotation(PermitidoSemOnboarding.class)
                    || metodo.getBeanType().isAnnotationPresent(PermitidoSemOnboarding.class);
            Set<RequestMethod> verbos = mapeamento.getKey().getMethodsCondition().getMethods();
            boolean multipart = mapeamento.getKey().getConsumesCondition().getConsumableMediaTypes()
                    .contains(MediaType.MULTIPART_FORM_DATA);
            for (String caminho : mapeamento.getKey().getPatternValues()) {
                if (!caminho.startsWith("/api/v1/") || caminho.startsWith("/api/v1/auth/")) {
                    continue;
                }
                verbos.forEach(verbo -> rotas.add(new Rota(verbo, caminho, liberada, multipart)));
            }
        }
        return rotas;
    }

    private record Rota(RequestMethod metodo, String caminho, boolean liberada, boolean multipart) {
        @Override
        public String toString() {
            return metodo + " " + caminho;
        }
    }
}
