package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.arquivos.ArmazenamentoArquivos;
import com.corpoforte.tracker.usuario.FotoDePerfil;
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

/**
 * Unitario com repositories mockados, mesma escolha (e mesmo motivo) do
 * UsuarioAtualServiceTest da Fase 6: o caso principal aqui e' uma CORRIDA
 * entre duas requisicoes, e reproduzir isso de verdade num *IT exigiria
 * duas threads disputando a mesma conexao - determinismo baixo pra provar
 * algo que o mock prova direto. Pior: num teste @Transactional a violacao
 * de constraint envenena a transacao do proprio teste, entao ele nem
 * chegaria a verificar o comportamento.
 */
class PostServiceTest {

    private final PostRepository postRepository = mock(PostRepository.class);
    private final ComentarioRepository comentarioRepository = mock(ComentarioRepository.class);
    private final CurtidaRepository curtidaRepository = mock(CurtidaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

    private final PostService service = new PostService(postRepository, comentarioRepository, curtidaRepository,
            usuarioRepository, mock(FotoPostRepository.class), mock(ArmazenamentoArquivos.class),
            mock(FotoDePerfil.class));

    /**
     * Duplo clique no botao de curtir (ou duas abas): as duas requisicoes
     * nao acham curtida existente e as duas tentam inserir. A segunda bate
     * no unique(post_id, usuario_id) - e isso nao pode virar 500, porque o
     * estado final que o usuario queria ja esta gravado.
     */
    @Test
    void curtidaConcorrenteQueColideNaConstraintNaoVira500() {
        when(postRepository.buscarVisivel(1L, 7L)).thenReturn(Optional.of(new Post(9L, "post", LocalDateTime.now())));
        when(curtidaRepository.findByPostIdAndUsuarioId(1L, 7L)).thenReturn(Optional.empty());
        when(curtidaRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatCode(() -> service.alternarCurtida(1L, 7L)).doesNotThrowAnyException();
    }

    @Test
    void curtirDeNovoDescurteEmVezDeInserirSegundaLinha() {
        Curtida jaCurtido = new Curtida(1L, 7L, LocalDateTime.now());
        when(postRepository.buscarVisivel(1L, 7L)).thenReturn(Optional.of(new Post(9L, "post", LocalDateTime.now())));
        when(curtidaRepository.findByPostIdAndUsuarioId(1L, 7L)).thenReturn(Optional.of(jaCurtido));

        service.alternarCurtida(1L, 7L);

        verify(curtidaRepository).delete(jaCurtido);
        verify(curtidaRepository, never()).save(any());
    }

    @Test
    void curtirPostInexistenteDa404SemGravarNada() {
        when(postRepository.buscarVisivel(404L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.alternarCurtida(404L, 7L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        excecao -> assertThat(excecao.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(curtidaRepository, never()).save(any());
    }

    @Test
    void apagarPostDeOutroUsuarioDa404SemApagar() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(new Post(7L, "post do 7", LocalDateTime.now())));

        assertThatThrownBy(() -> service.apagarPost(1L, 8L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        excecao -> assertThat(excecao.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(postRepository, never()).delete(any());
    }

    /** Autor do post (7) modera a conversa no proprio post: apaga o
     * comentario que outra pessoa (8) escreveu. */
    @Test
    void autorDoPostApagaComentarioDeOutraPessoa() {
        Comentario comentarioDo8 = new Comentario(1L, 8L, "comentario do 8", LocalDateTime.now());
        when(comentarioRepository.findById(50L)).thenReturn(Optional.of(comentarioDo8));
        when(postRepository.findById(1L)).thenReturn(Optional.of(new Post(7L, "post do 7", LocalDateTime.now())));

        service.apagarComentario(50L, 7L);

        verify(comentarioRepository).delete(comentarioDo8);
    }

    /** Nem autor do comentario (8) nem do post (7): terceiro (9) recebe o
     * mesmo 404 de comentario inexistente. */
    @Test
    void terceiroNaoApagaComentarioDeOutrosEmPostDeOutros() {
        when(comentarioRepository.findById(50L))
                .thenReturn(Optional.of(new Comentario(1L, 8L, "comentario do 8", LocalDateTime.now())));
        when(postRepository.findById(1L)).thenReturn(Optional.of(new Post(7L, "post do 7", LocalDateTime.now())));

        assertThatThrownBy(() -> service.apagarComentario(50L, 9L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        excecao -> assertThat(excecao.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(comentarioRepository, never()).delete(any());
    }

    @Test
    void comentarEmPostInexistenteDa404SemGravarNada() {
        when(postRepository.buscarVisivel(404L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.comentar(404L, 7L, "boa!"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        excecao -> assertThat(excecao.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(comentarioRepository, never()).save(any());
    }

    /** API (PUT): cliente que repete a requisicao depois de falha de rede
     * nao pode desfazer a curtida, como o toggle da tela faria. */
    @Test
    void curtirOQueJaEstaCurtidoNaoMudaNada() {
        when(postRepository.buscarVisivel(1L, 7L)).thenReturn(Optional.of(new Post(9L, "post", LocalDateTime.now())));
        when(curtidaRepository.findByPostIdAndUsuarioId(1L, 7L))
                .thenReturn(Optional.of(new Curtida(1L, 7L, LocalDateTime.now())));

        service.curtir(1L, 7L);

        verify(curtidaRepository, never()).delete(any());
        verify(curtidaRepository, never()).save(any());
    }

    @Test
    void curtirConcorrenteQueColideNaConstraintNaoVira500() {
        when(postRepository.buscarVisivel(1L, 7L)).thenReturn(Optional.of(new Post(9L, "post", LocalDateTime.now())));
        when(curtidaRepository.findByPostIdAndUsuarioId(1L, 7L)).thenReturn(Optional.empty());
        when(curtidaRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatCode(() -> service.curtir(1L, 7L)).doesNotThrowAnyException();
    }

    @Test
    void descurtirOQueNaoEstaCurtidoNaoEErro() {
        when(postRepository.buscarVisivel(1L, 7L)).thenReturn(Optional.of(new Post(9L, "post", LocalDateTime.now())));
        when(curtidaRepository.findByPostIdAndUsuarioId(1L, 7L)).thenReturn(Optional.empty());

        assertThatCode(() -> service.descurtir(1L, 7L)).doesNotThrowAnyException();

        verify(curtidaRepository, never()).delete(any());
    }

    @Test
    void curtirEDescurtirPostInexistenteDa404() {
        when(postRepository.buscarVisivel(404L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.curtir(404L, 7L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.descurtir(404L, 7L)).isInstanceOf(ResponseStatusException.class);
    }
}
