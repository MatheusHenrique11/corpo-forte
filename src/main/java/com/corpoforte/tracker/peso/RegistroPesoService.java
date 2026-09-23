package com.corpoforte.tracker.peso;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Pagina;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia do registro de peso: mesmo papel que UsuarioAtualService tem
 * pro perfil e AvaliacaoFisicaService pra avaliacao fisica. O Controller
 * nunca chama o repository direto.
 */
@Service
public class RegistroPesoService {

    private final RegistroPesoRepository registroPesoRepository;
    private final RegistroPesoCalculoService registroPesoCalculoService;
    private final UsuarioRepository usuarioRepository;

    public RegistroPesoService(RegistroPesoRepository registroPesoRepository,
                               RegistroPesoCalculoService registroPesoCalculoService,
                               UsuarioRepository usuarioRepository) {
        this.registroPesoRepository = registroPesoRepository;
        this.registroPesoCalculoService = registroPesoCalculoService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra o peso E mantem Usuario.pesoKg igual ao registro mais
     * recente POR DATA (Fase 3) - um registro retroativo nao sobrescreve o
     * peso atual. Essa sincronizacao morava no controller da tela; desceu
     * pra ca pra tela e API nao divergirem, e ganhou uma transacao: sem
     * ela, uma falha entre as duas gravacoes deixaria o perfil com um peso
     * que a tendencia semanal nunca viu.
     */
    @Transactional
    public RegistroPeso registrar(Usuario usuario, LocalDate data, double pesoKg) {
        RegistroPeso registro = salvar(usuario.getId(), data, pesoKg);

        obterMaisRecenteDoUsuario(usuario.getId()).ifPresent(maisRecente -> {
            usuario.atualizarPeso(maisRecente.getPesoKg());
            usuarioRepository.save(usuario);
        });

        return registro;
    }

    /** So' o upsert do dia, sem sincronizar o perfil - quem registra peso
     * de verdade chama registrar. */
    public RegistroPeso salvar(Long usuarioId, LocalDate data, double pesoKg) {
        RegistroPeso registro = registroPesoRepository.findByUsuarioIdAndData(usuarioId, data)
                .orElseGet(() -> new RegistroPeso(usuarioId, data, pesoKg));

        registro.atualizarPeso(pesoKg);

        return registroPesoRepository.save(registro);
    }

    public List<RegistroPeso> listarDoUsuario(Long usuarioId) {
        return registroPesoRepository.findByUsuarioIdOrderByDataDesc(usuarioId);
    }

    /** Mesmo historico, mais recente primeiro, paginado por cursor (API). */
    public Pagina<RegistroPeso> paginaDoUsuario(Long usuarioId, Cursor cursor, int tamanho) {
        Limit limite = Limit.of(tamanho + 1);
        List<RegistroPeso> buscados = cursor == null
                ? registroPesoRepository.findByUsuarioIdOrderByDataDesc(usuarioId, limite)
                : registroPesoRepository.findByUsuarioIdAndDataLessThanOrderByDataDesc(
                        usuarioId, cursor.comoData(), limite);
        return Pagina.deBuscaComUmAMais(buscados, tamanho, registro -> Cursor.apos(registro.getData(), registro.getId()));
    }

    public Optional<RegistroPeso> obterMaisRecenteDoUsuario(Long usuarioId) {
        return listarDoUsuario(usuarioId).stream().findFirst();
    }

    /** Vazio sem nenhum registro: nao ha semana nenhuma pra tirar media. */
    public Optional<TendenciaSemanal> tendenciaDoUsuario(Long usuarioId) {
        List<RegistroPesoPonto> pontos = listarDoUsuario(usuarioId).stream()
                .map(registro -> new RegistroPesoPonto(registro.getData(), registro.getPesoKg()))
                .toList();
        return pontos.isEmpty() ? Optional.empty() : Optional.of(registroPesoCalculoService.calcularTendencia(pontos));
    }
}
