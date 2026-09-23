package com.corpoforte.tracker;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Pra testar migration que MOVE dado de conta real: roda o Flyway ate uma
 * versao num schema proprio, deixa o teste inserir linhas no formato
 * antigo, e aplica o resto. Conexao propria, fora do pool da aplicacao, e
 * o schema e' apagado no close - o banco dos outros testes nao e' tocado.
 */
public final class SchemaDeMigracao implements AutoCloseable {

    private final String schema;
    private final DriverManagerDataSource conexao;
    private final JdbcTemplate jdbc;

    public SchemaDeMigracao(DataSource poolDaAplicacao, String schema) {
        HikariDataSource pool = (HikariDataSource) poolDaAplicacao;
        this.schema = schema;
        this.conexao = new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
        this.jdbc = new JdbcTemplate(conexao);
    }

    public void migrarAte(String versao) {
        Flyway.configure().dataSource(conexao).schemas(schema).locations("classpath:db/migration")
                .target(versao).load().migrate();
    }

    public void migrarTudo() {
        Flyway.configure().dataSource(conexao).schemas(schema).locations("classpath:db/migration")
                .load().migrate();
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    /** Nome qualificado: as consultas do teste nao dependem do search_path. */
    public String tabela(String nome) {
        return schema + "." + nome;
    }

    @Override
    public void close() {
        jdbc.execute("drop schema if exists " + schema + " cascade");
    }
}
