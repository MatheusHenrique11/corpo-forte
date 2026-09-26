package com.corpoforte.tracker.atividade;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.exercicio.ExercicioRepository;
import com.corpoforte.tracker.treino.TreinoDoDia;
import com.corpoforte.tracker.treino.TreinoDoDiaRepository;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * As regras da atividade que o banco segura sozinho, inseridas direto pelo
 * repository (contornando o service), no mesmo molde de
 * TreinoDoDiaRepositoryIT: o existsByTreinoDoDiaId do service nao e'
 * atomico, entao duas finalizacoes simultaneas so' sao barradas pelo
 * unique.
 */
@Transactional
class AtividadeRepositoryIT extends IntegrationTestBase {

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private AtividadeSerieRepository atividadeSerieRepository;

    @Autowired
    private TreinoDoDiaRepository treinoDoDiaRepository;

    @Autowired
    private ExercicioRepository exercicioRepository;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Autowired
    private EntityManager entityManager;

    private Usuario usuario;

    @BeforeEach
    void criarUsuario() {
        usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-atividade-constraint", "Usuaria Teste", "atividade-bd@exemplo.com"));
    }

    @Test
    void bancoRejeitaDuasAtividadesDoMesmoTreinoDoDia() {
        TreinoDoDia treino = treinoDoDiaRepository.saveAndFlush(new TreinoDoDia(usuario.getId(), LocalDate.now()));

        atividadeRepository.saveAndFlush(atividade(OrigemAtividade.TREINO_DO_DIA, treino.getId()));

        assertThatThrownBy(() -> atividadeRepository.saveAndFlush(atividade(OrigemAtividade.TREINO_DO_DIA, treino.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** O unique de treino_do_dia_id nao pode travar treino livre: todos tem
     * a coluna nula, e null nunca colide com null no Postgres. */
    @Test
    void variosTreinosLivresNoMesmoDiaSaoPermitidos() {
        atividadeRepository.saveAndFlush(atividade(OrigemAtividade.LIVRE, null));
        atividadeRepository.saveAndFlush(atividade(OrigemAtividade.LIVRE, null));

        assertThat(atividadeRepository.findAll())
                .filteredOn(atividade -> atividade.getUsuarioId().equals(usuario.getId()))
                .hasSize(2);
    }

    @Test
    void bancoRejeitaDuasSeriesNaMesmaPosicao() {
        Atividade atividade = atividadeRepository.saveAndFlush(atividade(OrigemAtividade.LIVRE, null));
        Long exercicioId = exercicioRepository.findAll().get(0).getId();

        atividadeSerieRepository.saveAndFlush(new AtividadeSerie(atividade.getId(), exercicioId, 0, 10));

        assertThatThrownBy(() -> atividadeSerieRepository.saveAndFlush(
                new AtividadeSerie(atividade.getId(), exercicioId, 0, 8)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** Apagar e' apagar: as series saem pelo cascade, sem delete em laco. */
    @Test
    void apagarAtividadeApagaAsSeriesPeloCascade() {
        Atividade atividade = atividadeRepository.saveAndFlush(atividade(OrigemAtividade.LIVRE, null));
        Long exercicioId = exercicioRepository.findAll().get(0).getId();
        atividadeSerieRepository.saveAndFlush(new AtividadeSerie(atividade.getId(), exercicioId, 0, 10));
        atividadeSerieRepository.saveAndFlush(new AtividadeSerie(atividade.getId(), exercicioId, 1, 10));

        atividadeRepository.delete(atividade);
        entityManager.flush();
        entityManager.clear();

        assertThat(atividadeSerieRepository.findAll())
                .noneMatch(serie -> serie.getAtividadeId().equals(atividade.getId()));
    }

    private Atividade atividade(OrigemAtividade origem, Long treinoDoDiaId) {
        return new Atividade(usuario.getId(), LocalDate.now(), origem, treinoDoDiaId, null, null, null,
                LocalDateTime.now());
    }
}
