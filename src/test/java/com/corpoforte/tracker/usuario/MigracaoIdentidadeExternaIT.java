package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.IntegrationTestBase;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A V12 e' a primeira migration do projeto que MOVE dado de usuario real
 * (usuario.google_sub -> identidade_externa) em vez de so' criar ou alterar
 * estrutura. Se o INSERT ... SELECT errasse, cada conta ja vinculada ao
 * Google cairia no fluxo de primeiro login depois do deploy e ganharia uma
 * conta nova vazia - e o mvn verify normal nao pegaria, porque roda a
 * cadeia inteira num banco sem nenhum usuario.
 *
 * Por isso este teste para o Flyway na V11, insere contas no formato
 * antigo e so' entao aplica o resto. Roda num schema proprio, com conexao
 * propria (fora do pool da aplicacao), e apaga o schema no fim: o banco que
 * os outros testes usam nao e' tocado.
 */
class MigracaoIdentidadeExternaIT extends IntegrationTestBase {

    private static final String SCHEMA = "migracao_identidade_externa";

    @Autowired
    private DataSource dataSource;

    private DriverManagerDataSource conexaoPropria;
    private JdbcTemplate jdbc;

    @BeforeEach
    void conectar() {
        HikariDataSource pool = (HikariDataSource) dataSource;
        conexaoPropria = new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
        jdbc = new JdbcTemplate(conexaoPropria);
    }

    @AfterEach
    void apagarSchema() {
        jdbc.execute("drop schema if exists " + SCHEMA + " cascade");
    }

    @Test
    void v12LevaOVinculoGoogleDeCadaContaParaIdentidadeExterna() {
        flyway().target("11").load().migrate();
        Long vinculada = inserirUsuarioNoFormatoAntigo("Conta Vinculada", "vinculada@exemplo.com", "sub-ja-vinculado");
        inserirUsuarioNoFormatoAntigo("Conta Local", null, null);

        flyway().load().migrate();

        List<Map<String, Object>> identidades =
                jdbc.queryForList("select usuario_id, provedor, sub from " + SCHEMA + ".identidade_externa");
        assertThat(identidades).singleElement().satisfies(identidade -> {
            assertThat(((Number) identidade.get("usuario_id")).longValue()).isEqualTo(vinculada);
            assertThat(identidade.get("provedor")).isEqualTo("GOOGLE");
            assertThat(identidade.get("sub")).isEqualTo("sub-ja-vinculado");
        });
        // nenhuma conta perdida, e a conta local sem login continua orfa
        assertThat(jdbc.queryForObject("select count(*) from " + SCHEMA + ".usuario", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = ? and table_name = 'usuario' and column_name = 'google_sub'",
                Integer.class, SCHEMA)).isZero();
    }

    private Long inserirUsuarioNoFormatoAntigo(String nome, String email, String googleSub) {
        return jdbc.queryForObject("insert into " + SCHEMA + ".usuario "
                        + "(nome, email, google_sub, peso_kg, altura_cm, idade, objetivo, nivel) "
                        + "values (?, ?, ?, 80, 178, 30, 'PERDA_GORDURA', 'INICIANTE') returning id",
                Long.class, nome, email, googleSub);
    }

    private FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(conexaoPropria)
                .schemas(SCHEMA)
                .locations("classpath:db/migration");
    }
}
