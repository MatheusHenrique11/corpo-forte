package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * "Uma curtida por usuario por post" tem que ser constraint de banco, nao
 * so' o find-antes-de-salvar do PostService - mesmo padrao ja usado pra
 * registro_peso (Fase 3), treino_do_dia (Fase 5), login Google
 * (Fase 6) e avaliacao_fisica (Fase 9). Insere direto pelo repository,
 * contornando alternarCurtida de proposito: e' o que prova que duas
 * requisicoes concorrentes nao conseguem gravar duas curtidas.
 */
@Transactional
class CurtidaRepositoryIT extends IntegrationTestBase {

    @Autowired
    private CurtidaRepository curtidaRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Test
    void bancoRejeitaDuasCurtidasDoMesmoUsuarioNoMesmoPost() {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-curtida-constraint", "Usuaria Teste", "curtida@exemplo.com"));
        Post post = postRepository.saveAndFlush(
                new Post(usuario.getId(), "Post pra curtir", LocalDateTime.now()));

        curtidaRepository.saveAndFlush(new Curtida(post.getId(), usuario.getId(), LocalDateTime.now()));

        assertThatThrownBy(() -> curtidaRepository.saveAndFlush(
                new Curtida(post.getId(), usuario.getId(), LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void doisUsuariosDiferentesCurtemOMesmoPost() {
        Usuario a = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-curtida-a", "Usuaria A", "curtida-a@exemplo.com"));
        Usuario b = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-curtida-b", "Usuario B", "curtida-b@exemplo.com"));
        Post post = postRepository.saveAndFlush(new Post(a.getId(), "Post popular", LocalDateTime.now()));

        curtidaRepository.saveAndFlush(new Curtida(post.getId(), a.getId(), LocalDateTime.now()));
        curtidaRepository.saveAndFlush(new Curtida(post.getId(), b.getId(), LocalDateTime.now()));

        List<ContagemPorPost> contagem =
                curtidaRepository.contarPorPost(List.of(post.getId()));

        assertThat(contagem).hasSize(1);
        assertThat(contagem.get(0).getTotal()).isEqualTo(2);
    }
}
