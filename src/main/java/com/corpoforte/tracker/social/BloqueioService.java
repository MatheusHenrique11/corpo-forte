package com.corpoforte.tracker.social;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.usuario.UsernameService;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Bloqueio (Fase 14): quem bloqueia deixa de existir pro bloqueado (404 no
 * perfil, nos posts, ao curtir, comentar e seguir) - e, mutuo em efeito,
 * o bloqueado some pra quem bloqueou. As consultas aplicam isso por
 * RegraDeBloqueio; aqui fica o gesto de bloquear e a checagem de uma conta.
 */
@Service
public class BloqueioService {

    private final BloqueioRepository bloqueioRepository;
    private final SeguimentoRepository seguimentoRepository;
    private final UsernameService usernameService;
    private final UsuarioRepository usuarioRepository;

    public BloqueioService(BloqueioRepository bloqueioRepository, SeguimentoRepository seguimentoRepository,
                           UsernameService usernameService, UsuarioRepository usuarioRepository) {
        this.bloqueioRepository = bloqueioRepository;
        this.seguimentoRepository = seguimentoRepository;
        this.usernameService = usernameService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Idempotente, e desfaz o seguir nos dois sentidos. Duas gravacoes
     * independentes, sem transacao em volta, de proposito: a corrida do
     * duplo clique (unique do banco) e' ignorada como no seguir, e isso so'
     * funciona fora de transacao - dentro dela, a violacao condenaria a
     * transacao inteira. As duas etapas sao idempotentes: repetir o pedido
     * termina o que tiver ficado pela metade.
     */
    public void bloquear(Long bloqueadorId, Long bloqueadoId) {
        if (bloqueadorId.equals(bloqueadoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível bloquear a própria conta");
        }
        if (bloqueioRepository.findByBloqueadorIdAndBloqueadoId(bloqueadorId, bloqueadoId).isEmpty()) {
            try {
                bloqueioRepository.save(new Bloqueio(bloqueadorId, bloqueadoId,
                        LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)));
            } catch (DataIntegrityViolationException e) {
                // ja bloqueado por uma requisicao concorrente - nada a fazer
            }
        }
        seguimentoRepository.apagarEntre(bloqueadorId, bloqueadoId);
    }

    /** Idempotente. So' desfaz o bloqueio feito por quem pede: se o outro
     * lado tambem bloqueou, esse continua valendo. */
    public void desbloquear(Long bloqueadorId, Long bloqueadoId) {
        bloqueioRepository.findByBloqueadorIdAndBloqueadoId(bloqueadorId, bloqueadoId)
                .ifPresent(bloqueioRepository::delete);
    }

    public boolean existeEntre(Long a, Long b) {
        return bloqueioRepository.existeEntre(a, b);
    }

    /**
     * A conta do username, se quem pede pode ve-la: com bloqueio em
     * qualquer sentido, o mesmo 404 de username inexistente - a resposta
     * nao confirma que a conta existe.
     */
    public Usuario contaVisivel(String username, Long quemVe) {
        return usernameService.buscarPorUsername(username)
                .filter(conta -> !bloqueioRepository.existeEntre(conta.getId(), quemVe))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Quem a conta bloqueou, bloqueio mais recente primeiro. */
    public Pagina<Usuario> paginaDeBloqueados(Long bloqueadorId, Cursor cursor, int tamanho) {
        Limit limite = Limit.of(tamanho + 1);
        List<Bloqueio> buscados = cursor == null
                ? bloqueioRepository.buscarDoBloqueador(bloqueadorId, limite)
                : bloqueioRepository.buscarDoBloqueadorApos(bloqueadorId, cursor.comoInstante(), cursor.id(), limite);
        return Pagina.deBuscaComUmAMais(buscados, tamanho, bloqueio -> Cursor.apos(bloqueio.getCriadoEm(), bloqueio.getId()))
                .mapearTodos(bloqueios -> {
                    List<Long> ids = bloqueios.stream().map(Bloqueio::getBloqueadoId).toList();
                    Map<Long, Usuario> porId = usuarioRepository.findAllById(ids).stream()
                            .collect(Collectors.toMap(Usuario::getId, Function.identity()));
                    return ids.stream().map(porId::get).toList();
                });
    }
}
