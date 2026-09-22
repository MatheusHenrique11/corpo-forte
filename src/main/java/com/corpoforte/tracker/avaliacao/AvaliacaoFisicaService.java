package com.corpoforte.tracker.avaliacao;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Persistencia da avaliacao fisica: mesmo papel que UsuarioAtualService tem
 * pro perfil. O Controller nunca chama o repository direto.
 */
@Service
public class AvaliacaoFisicaService {

    private final AvaliacaoFisicaRepository avaliacaoFisicaRepository;

    public AvaliacaoFisicaService(AvaliacaoFisicaRepository avaliacaoFisicaRepository) {
        this.avaliacaoFisicaRepository = avaliacaoFisicaRepository;
    }

    public Optional<AvaliacaoFisica> obterDoUsuario(Long usuarioId) {
        return avaliacaoFisicaRepository.findByUsuarioId(usuarioId);
    }

    public AvaliacaoFisica salvar(Long usuarioId, int repsPuxarVertical, int repsEmpurrarVertical,
                                   int repsPernasBilateral, int repsPuxarHorizontal, int repsEmpurrarHorizontal,
                                   int repsPernasUnilateral) {
        AvaliacaoFisica avaliacao = avaliacaoFisicaRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> new AvaliacaoFisica(usuarioId, 0, 0, 0, 0, 0, 0, LocalDate.now()));

        avaliacao.atualizar(repsPuxarVertical, repsEmpurrarVertical, repsPernasBilateral,
                repsPuxarHorizontal, repsEmpurrarHorizontal, repsPernasUnilateral, LocalDate.now());

        return avaliacaoFisicaRepository.save(avaliacao);
    }
}
