package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.api.Cursor;
import com.corpoforte.tracker.api.Pagina;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia da avaliacao fisica: mesmo papel que UsuarioAtualService tem
 * pro perfil. O Controller nunca chama o repository direto.
 *
 * Desde a Fase 9 guarda historico (uma avaliacao por dia por usuario), e
 * "a avaliacao do usuario" passou a significar "a mais recente" - dai o
 * nome obterMaisRecenteDoUsuario, igual ao que RegistroPesoService ja usa.
 */
@Service
public class AvaliacaoFisicaService {

    private final AvaliacaoFisicaRepository avaliacaoFisicaRepository;
    private final AvaliacaoFisicaCalculoService avaliacaoFisicaCalculoService;
    private final AvaliacaoFisicaComparacaoService avaliacaoFisicaComparacaoService;

    public AvaliacaoFisicaService(AvaliacaoFisicaRepository avaliacaoFisicaRepository,
                                  AvaliacaoFisicaCalculoService avaliacaoFisicaCalculoService,
                                  AvaliacaoFisicaComparacaoService avaliacaoFisicaComparacaoService) {
        this.avaliacaoFisicaRepository = avaliacaoFisicaRepository;
        this.avaliacaoFisicaCalculoService = avaliacaoFisicaCalculoService;
        this.avaliacaoFisicaComparacaoService = avaliacaoFisicaComparacaoService;
    }

    public Optional<AvaliacaoFisica> obterMaisRecenteDoUsuario(Long usuarioId) {
        return listarHistorico(usuarioId).stream().findFirst();
    }

    /** Mais recente primeiro. */
    public List<AvaliacaoFisica> listarHistorico(Long usuarioId) {
        return avaliacaoFisicaRepository.findByUsuarioIdOrderByDataAvaliacaoDesc(usuarioId);
    }

    /** Mesmo historico, paginado por cursor (API). */
    public Pagina<AvaliacaoFisica> paginaDoHistorico(Long usuarioId, Cursor cursor, int tamanho) {
        Limit limite = Limit.of(tamanho + 1);
        List<AvaliacaoFisica> buscadas = cursor == null
                ? avaliacaoFisicaRepository.findByUsuarioIdOrderByDataAvaliacaoDesc(usuarioId, limite)
                : avaliacaoFisicaRepository.findByUsuarioIdAndDataAvaliacaoLessThanOrderByDataAvaliacaoDesc(
                        usuarioId, cursor.comoData(), limite);
        return Pagina.deBuscaComUmAMais(buscadas, tamanho,
                avaliacao -> Cursor.apos(avaliacao.getDataAvaliacao(), avaliacao.getId()));
    }

    /** Volumes (B, C, D) de uma avaliacao; nunca persistidos, sempre
     * recalculados (Fase 2). */
    public List<AvaliacaoFisicaItemResultado> resultadoDe(AvaliacaoFisica avaliacao) {
        return avaliacaoFisicaCalculoService.calcular(
                avaliacao.getRepsPuxarVertical(), avaliacao.getRepsEmpurrarVertical(),
                avaliacao.getRepsPernasBilateral(), avaliacao.getRepsPuxarHorizontal(),
                avaliacao.getRepsEmpurrarHorizontal(), avaliacao.getRepsPernasUnilateral());
    }

    /**
     * Avaliacao mais recente contra a anterior. Vazio com menos de duas:
     * nao ha o que comparar. Mora aqui (e nao no controller da tela, onde
     * nasceu na Fase 9) porque a tela e a API precisam da mesma regra.
     */
    public Optional<ComparacaoAvaliacoes> compararUltimas(Long usuarioId) {
        List<AvaliacaoFisica> ultimas = avaliacaoFisicaRepository.findByUsuarioIdOrderByDataAvaliacaoDesc(
                usuarioId, Limit.of(2));
        if (ultimas.size() < 2) {
            return Optional.empty();
        }
        AvaliacaoFisica atual = ultimas.get(0);
        AvaliacaoFisica anterior = ultimas.get(1);
        return Optional.of(new ComparacaoAvaliacoes(atual.getDataAvaliacao(), anterior.getDataAvaliacao(),
                avaliacaoFisicaComparacaoService.comparar(resultadoDe(atual), resultadoDe(anterior))));
    }

    /**
     * Upsert por dia: refazer a avaliacao hoje corrige a medicao de hoje;
     * refazer amanha cria uma entrada nova no historico.
     */
    public AvaliacaoFisica salvar(Long usuarioId, int repsPuxarVertical, int repsEmpurrarVertical,
                                   int repsPernasBilateral, int repsPuxarHorizontal, int repsEmpurrarHorizontal,
                                   int repsPernasUnilateral) {
        LocalDate hoje = LocalDate.now();

        AvaliacaoFisica avaliacao = avaliacaoFisicaRepository.findByUsuarioIdAndDataAvaliacao(usuarioId, hoje)
                .orElseGet(() -> new AvaliacaoFisica(usuarioId, 0, 0, 0, 0, 0, 0, hoje));

        avaliacao.atualizar(repsPuxarVertical, repsEmpurrarVertical, repsPernasBilateral,
                repsPuxarHorizontal, repsEmpurrarHorizontal, repsPernasUnilateral, hoje);

        return avaliacaoFisicaRepository.save(avaliacao);
    }
}
