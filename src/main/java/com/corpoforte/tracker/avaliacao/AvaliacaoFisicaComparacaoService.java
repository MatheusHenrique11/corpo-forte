package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Compara duas avaliacoes por padrao de movimento - o pagamento do ciclo
 * inteiro: medir, treinar 8 semanas, remedir e ver "10 -> 15 (+5)".
 *
 * Recebe dois List<AvaliacaoFisicaItemResultado> (o que
 * AvaliacaoFisicaCalculoService ja devolve) em vez das entidades JPA: o
 * record ja e' por movimento e ja carrega repeticoesMaximas, entao nao
 * precisa de record de entrada novo nem acopla este service puro a
 * persistencia - mesmo cuidado que levou RegistroPesoCalculoService a
 * receber RegistroPesoPonto em vez de RegistroPeso.
 */
@Service
public class AvaliacaoFisicaComparacaoService {

    public List<ComparacaoItem> comparar(List<AvaliacaoFisicaItemResultado> atual,
                                          List<AvaliacaoFisicaItemResultado> anterior) {
        return Arrays.stream(MovimentoPadrao.values())
                .map(movimento -> {
                    int repsAtual = repeticoesDoMovimento(atual, movimento);
                    int repsAnterior = repeticoesDoMovimento(anterior, movimento);
                    return new ComparacaoItem(movimento, repsAnterior, repsAtual, repsAtual - repsAnterior);
                })
                .toList();
    }

    private int repeticoesDoMovimento(List<AvaliacaoFisicaItemResultado> itens, MovimentoPadrao movimento) {
        return itens.stream()
                .filter(item -> item.movimento() == movimento)
                .findFirst()
                .map(AvaliacaoFisicaItemResultado::repeticoesMaximas)
                .orElseThrow(() -> new IllegalArgumentException("Sem medicao para " + movimento));
    }
}
