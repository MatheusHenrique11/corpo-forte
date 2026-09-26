package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.arquivos.ImagensDeTeste;
import com.corpoforte.tracker.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fotos em post (Fase 15), da ida (multipart) a volta (URL assinada que um
 * <img> usa sem login).
 */
@AutoConfigureMockMvc
@Transactional
class FotosPostApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    private Usuario autora;

    @BeforeEach
    void criarConta() {
        autora = contaComOnboarding(OidcTestUsers.principal("sub-fotos-autora", "Autora", "fotos@exemplo.com"));
    }

    /**
     * O caminho inteiro: a foto sai do celular com GPS no EXIF, e o que a
     * URL devolvida entrega - sem login nenhum, como num <img> - e' um JPEG
     * sem EXIF.
     */
    @Test
    void publicarComFotosDevolveUrlsAssinadasQueEntregamJpegSemGps() throws Exception {
        String resposta = publicar(multipartDePost()
                .file(foto(ImagensDeTeste.jpegComExif(2000, 1000, 1)))
                .file(foto(ImagensDeTeste.png(300, 300)))
                .param("texto", "treino no parque"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fotos", hasSize(2)))
                .andExpect(jsonPath("$.fotos[0].url", startsWith("http://localhost/arquivos/posts/")))
                .andExpect(jsonPath("$.fotos[0].largura").value(1600))
                .andExpect(jsonPath("$.fotos[0].altura").value(800))
                .andReturn().getResponse().getContentAsString();

        byte[] entregue = baixar(JsonPath.read(resposta, "$.fotos[0].url"));
        assertThat(ImagensDeTeste.contem(entregue, ImagensDeTeste.MARCA_DO_GPS)).isFalse();
        assertThat(ImagensDeTeste.contem(entregue, "Exif")).isFalse();
        assertThat(ImagensDeTeste.ler(baixar(JsonPath.read(resposta, "$.fotos[0].urlMiniatura"))).getWidth())
                .isEqualTo(400);
    }

    @Test
    void postDeFotoPodeVirSemLegendaMasNaoSemNada() throws Exception {
        publicar(multipartDePost().file(foto(ImagensDeTeste.jpeg(100, 100))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value(""));

        publicar(multipartDePost().param("texto", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Escreva algo ou envie ao menos uma foto"));
    }

    /** Nada e' gravado quando o post e' recusado - nem post, nem arquivo. */
    @Test
    void maisDe4FotosOuArquivoQueNaoEImagemRecusaOPostInteiro() throws Exception {
        long postsAntes = postRepository.count();
        long arquivosAntes = arquivosEmDisco();

        MockMultipartHttpServletRequestBuilder cinco = multipartDePost();
        for (int i = 0; i < 5; i++) {
            cinco.file(foto(ImagensDeTeste.jpeg(50, 50)));
        }
        cinco.param("texto", "muitas");
        publicar(cinco).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No máximo 4 fotos por post"));

        // nome e Content-Type de foto, conteudo de texto
        publicar(multipartDePost()
                .file(foto(ImagensDeTeste.jpeg(50, 50)))
                .file(new MockMultipartFile("fotos", "foto.jpg", "image/jpeg",
                        "nao sou imagem".getBytes(StandardCharsets.UTF_8)))
                .param("texto", "disfarcado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Envie fotos em JPEG ou PNG"));

        assertThat(postRepository.count()).isEqualTo(postsAntes);
        assertThat(arquivosEmDisco()).isEqualTo(arquivosAntes);
    }

    /** A URL e' a permissao: sem a assinatura certa, 404 (e nao
     * redirecionamento pro login, que e' o que um <img> receberia se a
     * resposta passasse pela pagina de erro da chain web). */
    @Test
    void urlSemAssinaturaOuAdulteradaDa404() throws Exception {
        String url = JsonPath.read(publicar(multipartDePost().file(foto(ImagensDeTeste.jpeg(100, 100))))
                .andReturn().getResponse().getContentAsString(), "$.fotos[0].url");
        String semHost = url.substring("http://localhost".length());

        mockMvc.perform(get(semHost.substring(0, semHost.indexOf('?')))).andExpect(status().isNotFound());
        mockMvc.perform(get(semHost + "x")).andExpect(status().isNotFound());
        mockMvc.perform(get(semHost)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void apagarOPostApagaOsArquivos() throws Exception {
        String resposta = publicar(multipartDePost().file(foto(ImagensDeTeste.jpeg(100, 100))))
                .andReturn().getResponse().getContentAsString();
        Integer postId = JsonPath.read(resposta, "$.id");
        Path arquivo = noDisco(JsonPath.read(resposta, "$.fotos[0].url"));
        Path miniatura = noDisco(JsonPath.read(resposta, "$.fotos[0].urlMiniatura"));
        assertThat(arquivo).exists();
        assertThat(miniatura).exists();

        mockMvc.perform(delete("/api/v1/posts/" + postId).header(HttpHeaders.AUTHORIZATION, bearer(autora)))
                .andExpect(status().isNoContent());

        assertThat(arquivo).doesNotExist();
        assertThat(miniatura).doesNotExist();
    }

    /** URL de foto so' e' gerada pra quem pode ver o post: quem nao ve nao
     * recebe o post, e portanto nenhuma URL. */
    @Test
    void fotoDePostQueNaoPodeSerVistoNuncaGanhaUrl() throws Exception {
        String resposta = publicar(multipartDePost()
                .file(foto(ImagensDeTeste.jpeg(100, 100)))
                .param("visibilidade", "SOMENTE_EU"))
                .andExpect(jsonPath("$.visibilidade").value("SOMENTE_EU"))
                .andReturn().getResponse().getContentAsString();
        Integer postId = JsonPath.read(resposta, "$.id");
        Usuario outra = contaComOnboarding(OidcTestUsers.principal("sub-fotos-outra", "Outra", "fotos-outra@exemplo.com"));

        mockMvc.perform(get("/api/v1/feed/descobrir").header(HttpHeaders.AUTHORIZATION, bearer(outra)))
                .andExpect(jsonPath("$.itens").isEmpty());
        mockMvc.perform(get("/api/v1/posts/" + postId).header(HttpHeaders.AUTHORIZATION, bearer(outra)))
                .andExpect(status().isNotFound());
    }

    private MockMultipartHttpServletRequestBuilder multipartDePost() {
        return multipart("/api/v1/posts");
    }

    private ResultActions publicar(MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, bearer(autora)));
    }

    private static MockMultipartFile foto(byte[] conteudo) {
        return new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", conteudo);
    }

    private byte[] baixar(String url) throws Exception {
        return mockMvc.perform(get(url.substring("http://localhost".length())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private static Path noDisco(String url) {
        String chave = url.substring("http://localhost/arquivos/".length(), url.indexOf('?'));
        return Path.of(DIRETORIO_DE_ARQUIVOS, chave);
    }

    private static long arquivosEmDisco() throws Exception {
        Path posts = Path.of(DIRETORIO_DE_ARQUIVOS, "posts");
        if (!Files.exists(posts)) {
            return 0;
        }
        try (var arquivos = Files.list(posts)) {
            return arquivos.count();
        }
    }
}
