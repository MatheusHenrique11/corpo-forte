package com.corpoforte.tracker.avaliacao;

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

    public AvaliacaoFisicaService(AvaliacaoFisicaRepository avaliacaoFisicaRepository) {
        this.avaliacaoFisicaRepository = avaliacaoFisicaRepository;
    }

    public Optional<AvaliacaoFisica> obterMaisRecenteDoUsuario(Long usuarioId) {
        return listarHistorico(usuarioId).stream().findFirst();
    }

    /** Mais recente primeiro. */
    public List<AvaliacaoFisica> listarHistorico(Long usuarioId) {
        return avaliacaoFisicaRepository.findByUsuarioIdOrderByDataAvaliacaoDesc(usuarioId);
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
