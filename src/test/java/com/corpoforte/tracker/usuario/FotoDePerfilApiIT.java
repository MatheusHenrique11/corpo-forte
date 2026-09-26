package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.arquivos.ImagensDeTeste;
import com.corpoforte.tracker.feed.Post;
import com.corpoforte.tracker.feed.PostRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Foto de perfil propria (Fase 15), no lugar da do Google. */
@AutoConfigureMockMvc
@Transactional
class FotoDePerfilApiIT extends IntegrationTestBase {

    private static final String FOTO_DO_GOOGLE = "https://lh3.googleusercontent.com/a/foto-do-google";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private PostRepository postRepository;

    private Usuario conta;

    @BeforeEach
    void criarConta() {
        conta = contaComOnboarding(OidcTestUsers.principal("sub-perfil-foto", "Fulana", "perfil-foto@exemplo.com"));
        usuarioAtualService.obterOuCriar(loginDoGoogle(FOTO_DO_GOOGLE));
    }

    @Test
    void fotoEnviadaValeEmTodoLugarQueMostraAConta() throws Exception {
        trocar(ImagensDeTeste.jpeg(1000, 1000))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl", startsWith("http://localhost/arquivos/perfis/")));
        postRepository.save(new Post(conta.getId(), "post", LocalDateTime.now()));

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(conta)))
                .andExpect(jsonPath("$.fotoUrl", startsWith("http://localhost/arquivos/perfis/")));
        mockMvc.perform(get("/api/v1/feed/descobrir").header(HttpHeaders.AUTHORIZATION, bearer(conta)))
                .andExpect(jsonPath("$.itens[0].autor.fotoUrl", startsWith("http://localhost/arquivos/perfis/")));
        mockMvc.perform(get("/api/v1/usuarios/" + conta.getUsername()).header(HttpHeaders.AUTHORIZATION, bearer(conta)))
                .andExpect(jsonPath("$.fotoUrl", startsWith("http://localhost/arquivos/perfis/")));
    }

    /** O login continua atualizando a foto do Google por baixo, mas ela nao
     * passa por cima da enviada - e volta a valer quando a enviada sai. */
    @Test
    void loginNaoSobrescreveAFotoEnviadaERemoverVoltaAoGoogle() throws Exception {
        trocar(ImagensDeTeste.jpeg(200, 200)).andExpect(status().isOk());

        usuarioAtualService.obterOuCriar(loginDoGoogle("https://lh3.googleusercontent.com/a/foto-nova-do-google"));
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(conta)))
                .andExpect(jsonPath("$.fotoUrl", startsWith("http://localhost/arquivos/perfis/")));

        mockMvc.perform(delete("/api/v1/perfil/foto").header(HttpHeaders.AUTHORIZATION, bearer(conta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").value("https://lh3.googleusercontent.com/a/foto-nova-do-google"));
    }

    @Test
    void trocarDeNovoApagaOArquivoAnteriorEFotoRedimensionada() throws Exception {
        String primeira = JsonPath.read(trocar(ImagensDeTeste.jpeg(2000, 1000))
                .andReturn().getResponse().getContentAsString(), "$.fotoUrl");
        Path arquivoDaPrimeira = noDisco(primeira);
        assertThat(ImagensDeTeste.ler(Files.readAllBytes(arquivoDaPrimeira)).getWidth()).isEqualTo(512);

        trocar(ImagensDeTeste.png(100, 100)).andExpect(status().isOk());

        assertThat(arquivoDaPrimeira).doesNotExist();
    }

    @Test
    void arquivoQueNaoEImagemDa400() throws Exception {
        trocar("texto".getBytes())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Envie fotos em JPEG ou PNG"));
    }

    private ResultActions trocar(byte[] conteudo) throws Exception {
        return mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/perfil/foto")
                .file(new MockMultipartFile("foto", "eu.jpg", "image/jpeg", conteudo))
                .header(HttpHeaders.AUTHORIZATION, bearer(conta)));
    }

    private static DadosLogin loginDoGoogle(String foto) {
        return new DadosLogin(Provedor.GOOGLE, "sub-perfil-foto", "perfil-foto@exemplo.com", true, "Fulana", foto);
    }

    private static Path noDisco(String url) {
        return Path.of(DIRETORIO_DE_ARQUIVOS, url.substring("http://localhost/arquivos/".length(), url.indexOf('?')));
    }
}
