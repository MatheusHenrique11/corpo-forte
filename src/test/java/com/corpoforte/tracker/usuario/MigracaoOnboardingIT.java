package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.SchemaDeMigracao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A V15 marca toda conta que ja existia como onboarding concluido e gera
 * username pra ela. Se nao marcasse, quem ja usa o app seria barrado na
 * primeira chamada depois do deploy, com o historico inteiro la dentro.
 */
class MigracaoOnboardingIT extends IntegrationTestBase {

    @Autowired
    private DataSource dataSource;

    @Test
    void v15ConcluiOOnboardingDasContasQueJaExistiamComUsernameGerado() {
        try (SchemaDeMigracao banco = new SchemaDeMigracao(dataSource, "migracao_onboarding")) {
            banco.migrarAte("14");
            Long existente = banco.jdbc().queryForObject("insert into " + banco.tabela("usuario")
                    + " (nome, email, peso_kg, altura_cm, idade, objetivo, nivel) "
                    + "values ('Conta Antiga', 'antiga@exemplo.com', 82, 178, 30, 'PERDA_GORDURA', 'INICIANTE') "
                    + "returning id", Long.class);

            banco.migrarTudo();

            Map<String, Object> conta = banco.jdbc().queryForMap(
                    "select username, onboarding_concluido from " + banco.tabela("usuario") + " where id = ?",
                    existente);
            assertThat(conta.get("onboarding_concluido")).isEqualTo(true);
            assertThat(conta.get("username")).isEqualTo("atleta" + existente);
            // e o username gerado passa na mesma regra de quem escolhe o nome
            assertThat(RegraUsername.problemaSemConsultarBanco("atleta" + existente)).isEmpty();

            // conta criada depois da migration nasce pendente
            Map<String, Object> nova = banco.jdbc().queryForMap("insert into " + banco.tabela("usuario")
                    + " (nome, peso_kg, altura_cm, idade, objetivo, nivel) "
                    + "values ('Conta Nova', 100, 178, 27, 'PERDA_GORDURA', 'INICIANTE') "
                    + "returning username, onboarding_concluido");
            assertThat(nova.get("onboarding_concluido")).isEqualTo(false);
            assertThat(nova.get("username")).isNull();
        }
    }
}
