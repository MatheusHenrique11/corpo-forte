package com.corpoforte.tracker;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para testes de integracao (contexto Spring completo + banco real).
 * Sobe um Postgres via Testcontainers em vez de H2: as migrations do Flyway
 * usam recursos especificos do Postgres (bigserial, double precision) e o
 * ddl-auto e' "validate" - testar contra outro banco esconderia
 * incompatibilidade real que so apareceria em produção.
 *
 * O container e' compartilhado entre todas as classes de teste de
 * integracao: inicializado uma unica vez no bloco estatico (nao gerenciado
 * pela extensao @Testcontainers de proposito, pra nao ser derrubado depois
 * da primeira classe de teste) e encerrado pelo Ryuk do Testcontainers
 * quando a JVM termina. Isso evita pagar o custo de subir um Postgres novo
 * a cada classe de teste.
 */
@SpringBootTest
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
