package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Persistencia do feed: mesmo papel que os outros XService tem pras suas
 * entidades. O Controller nunca chama os repositories direto.
 */
@Service
public class PostService {

    private final PostRepository postRepository;
    private final UsuarioRepository usuarioRepository;

    public PostService(PostRepository postRepository, UsuarioRepository usuarioRepository) {
        this.postRepository = postRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Post criar(Long usuarioId, String texto) {
        return postRepository.save(new Post(usuarioId, texto, LocalDateTime.now()));
    }

    /**
     * Junta os posts (de todo mundo, de proposito - ver Post.java) com o
     * nome de cada autor, buscando os Usuario dos usuarioId distintos numa
     * unica consulta (sem N+1) - mesmo padrao que TreinoDoDiaService usa
     * pra juntar TreinoItem com o catalogo de Exercicio.
     */
    public List<PostView> listarFeed() {
        List<Post> posts = postRepository.findAllByOrderByCriadoEmDesc();

        List<Long> usuarioIds = posts.stream().map(Post::getUsuarioId).distinct().toList();
        Map<Long, String> nomePorUsuarioId = usuarioRepository.findAllById(usuarioIds).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNome));

        return posts.stream()
                .map(post -> new PostView(post.getId(), nomePorUsuarioId.get(post.getUsuarioId()),
                        post.getTexto(), post.getCriadoEm()))
                .toList();
    }
}
