package com.corpoforte.tracker.social;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Seguir (Fase 13): assimetrico e sem aprovacao - seguir alguem nao depende
 * da outra pessoa, e ela nao passa a seguir de volta.
 */
@Service
public class SeguimentoService {

    private final SeguimentoRepository seguimentoRepository;
    private final BloqueioRepository bloqueioRepository;
    private final UsuarioRepository usuarioRepository;

    public SeguimentoService(SeguimentoRepository seguimentoRepository, BloqueioRepository bloqueioRepository,
                             UsuarioRepository usuarioRepository) {
        this.seguimentoRepository = seguimentoRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Idempotente: seguir quem ja segue nao muda nada. O catch e' o duplo
     * clique (duas requisicoes passando pelo find sem achar nada): a segunda
     * esbarra no unique do banco, e o estado que a pessoa queria ja esta
     * gravado - mesmo tratamento da curtida (Fase 7b).
     */
    public void seguir(Long seguidorId, Long seguidoId) {
        if (seguidorId.equals(seguidoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível seguir a própria conta");
        }
        // com bloqueio (em qualquer sentido) a conta nem aparece pra quem
        // tenta seguir: mesmo 404 de conta inexistente
        if (bloqueioRepository.existeEntre(seguidorId, seguidoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (seguimentoRepository.findBySeguidorIdAndSeguidoId(seguidorId, seguidoId).isPresent()) {
            return;
        }
        try {
            seguimentoRepository.save(new Seguimento(seguidorId, seguidoId,
                    LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)));
        } catch (DataIntegrityViolationException e) {
            // ja seguido por uma requisicao concorrente - nada a fazer
        }
    }

    /** Idempotente: deixar de seguir quem nao segue nao e' erro. */
    public void deixarDeSeguir(Long seguidorId, Long seguidoId) {
        seguimentoRepository.findBySeguidorIdAndSeguidoId(seguidorId, seguidoId)
                .ifPresent(seguimentoRepository::delete);
    }

    public ContagemSocial contar(Long usuarioId) {
        return new ContagemSocial(seguimentoRepository.countBySeguidoId(usuarioId),
                seguimentoRepository.countBySeguidorId(usuarioId));
    }

    public boolean segue(Long seguidorId, Long seguidoId) {
        return seguimentoRepository.findBySeguidorIdAndSeguidoId(seguidorId, seguidoId).isPresent();
    }

    /** Quais das contas o usuario segue, numa consulta so' (pra marcar o
     * botao "seguindo" em qualquer lista de contas). */
    public Set<Long> quaisSegue(Long seguidorId, Collection<Long> usuarioIds) {
        return usuarioIds.isEmpty() ? Set.of() : new HashSet<>(seguimentoRepository.quaisSegue(seguidorId, usuarioIds));
    }

    /** Quem segue a conta, quem seguiu por ultimo primeiro, sem as contas
     * com bloqueio com quem ve. */
    public Pagina<Usuario> paginaDeSeguidores(Long usuarioId, Long quemVe, Cursor cursor, int tamanho) {
        Limit limite = Limit.of(tamanho + 1);
        List<Seguimento> buscados = cursor == null
                ? seguimentoRepository.buscarSeguidores(usuarioId, quemVe, limite)
                : seguimentoRepository.buscarSeguidoresApos(usuarioId, quemVe, cursor.comoInstante(), cursor.id(),
                        limite);
        return Pagina.deBuscaComUmAMais(buscados, tamanho, SeguimentoService::cursorDe)
                .mapearTodos(seguimentos -> usuariosNaOrdem(seguimentos, Seguimento::getSeguidorId));
    }

    /** Quem a conta segue, quem ela seguiu por ultimo primeiro, sem as
     * contas com bloqueio com quem ve. */
    public Pagina<Usuario> paginaDeSeguindo(Long usuarioId, Long quemVe, Cursor cursor, int tamanho) {
        Limit limite = Limit.of(tamanho + 1);
        List<Seguimento> buscados = cursor == null
                ? seguimentoRepository.buscarSeguindo(usuarioId, quemVe, limite)
                : seguimentoRepository.buscarSeguindoApos(usuarioId, quemVe, cursor.comoInstante(), cursor.id(),
                        limite);
        return Pagina.deBuscaComUmAMais(buscados, tamanho, SeguimentoService::cursorDe)
                .mapearTodos(seguimentos -> usuariosNaOrdem(seguimentos, Seguimento::getSeguidoId));
    }

    private static Cursor cursorDe(Seguimento seguimento) {
        return Cursor.apos(seguimento.getCriadoEm(), seguimento.getId());
    }

    /** Uma busca pras contas da pagina inteira, devolvidas na ordem dos
     * seguimentos (findAllById nao garante ordem). */
    private List<Usuario> usuariosNaOrdem(List<Seguimento> seguimentos, Function<Seguimento, Long> qualPonta) {
        List<Long> ids = seguimentos.stream().map(qualPonta).toList();
        Map<Long, Usuario> porId = usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        return ids.stream().map(porId::get).toList();
    }
}
