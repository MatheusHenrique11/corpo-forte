package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaItemResultado;
import com.corpoforte.tracker.exercicio.Exercicio;
import com.corpoforte.tracker.exercicio.MovimentoPadrao;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 3 series fixas; repeticoes = volume inicial (Fase 2) dividido por 3,
 * arredondado - mesmo Math.round ja usado em AvaliacaoFisicaCalculoService,
 * consistente. Quando um padrao de movimento nao tem nenhum exercicio
 * compativel (nivel + equipamento do usuario), o padrao e' pulado em vez de
 * travar a geracao inteira - ver movimentosSemOpcao no resultado.
 *
 * Sem Spring/banco no meio (Random e' so um campo, nao injetado): da pra
 * testar chamando gerar(...) direto, mesmo molde dos outros
 * XCalculoService do projeto.
 */
@Service
public class TreinoDoDiaGeradorService {

    private static final int SERIES_FIXAS = 3;

    private final Random random = new Random();

    public TreinoGerado gerar(List<AvaliacaoFisicaItemResultado> volumes,
                               Map<MovimentoPadrao, List<Exercicio>> candidatosPorMovimento) {
        List<ItemGerado> itens = new ArrayList<>();
        List<MovimentoPadrao> semOpcao = new ArrayList<>();

        for (MovimentoPadrao movimento : MovimentoPadrao.values()) {
            List<Exercicio> candidatos = candidatosPorMovimento.getOrDefault(movimento, List.of());

            if (candidatos.isEmpty()) {
                semOpcao.add(movimento);
                continue;
            }

            Exercicio escolhido = candidatos.get(random.nextInt(candidatos.size()));
            int volumeInicial = volumeInicialDoMovimento(volumes, movimento);
            int repeticoes = (int) Math.round(volumeInicial / (double) SERIES_FIXAS);

            itens.add(new ItemGerado(movimento, escolhido, SERIES_FIXAS, repeticoes));
        }

        return new TreinoGerado(itens, semOpcao);
    }

    private int volumeInicialDoMovimento(List<AvaliacaoFisicaItemResultado> volumes, MovimentoPadrao movimento) {
        return volumes.stream()
                .filter(item -> item.movimento() == movimento)
                .findFirst()
                .map(AvaliacaoFisicaItemResultado::volumeInicial)
                .orElseThrow(() -> new IllegalArgumentException("Sem volume calculado para " + movimento));
    }
}
