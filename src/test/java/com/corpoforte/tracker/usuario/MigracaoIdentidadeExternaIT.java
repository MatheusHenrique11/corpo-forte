package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.SchemaDeMigracao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
 * antigo e so' entao aplica o resto (SchemaDeMigracao).
 */
class MigracaoIdentidadeExternaIT extends IntegrationTestBase {

    @Autowired
    private DataSource dataSource;

    @Test
    void v12LevaOVinculoGoogleDeCadaContaParaIdentidadeExterna() {
        try (SchemaDeMigracao banco = new SchemaDeMigracao(dataSource, "migracao_identidade_externa")) {
            banco.migrarAte("11");
            Long vinculada = inserirUsuarioNoFormatoAntigo(banco, "Conta Vinculada", "vinculada@exemplo.com",
                    "sub-ja-vinculado");
            inserirUsuarioNoFormatoAntigo(banco, "Conta Local", null, null);

            banco.migrarTudo();

            List<Map<String, Object>> identidades = banco.jdbc().queryForList(
                    "select usuario_id, provedor, sub from " + banco.tabela("identidade_externa"));
            assertThat(identidades).singleElement().satisfies(identidade -> {
                assertThat(((Number) identidade.get("usuario_id")).longValue()).isEqualTo(vinculada);
                assertThat(identidade.get("provedor")).isEqualTo("GOOGLE");
                assertThat(identidade.get("sub")).isEqualTo("sub-ja-vinculado");
            });
            // nenhuma conta perdida, e a conta local sem login continua orfa
            assertThat(banco.jdbc().queryForObject("select count(*) from " + banco.tabela("usuario"), Integer.class))
                    .isEqualTo(2);
            assertThat(banco.jdbc().queryForObject(
                    "select count(*) from information_schema.columns "
                            + "where table_schema = 'migracao_identidade_externa' and table_name = 'usuario' "
                            + "and column_name = 'google_sub'",
                    Integer.class)).isZero();
        }
    }

    private static Long inserirUsuarioNoFormatoAntigo(SchemaDeMigracao banco, String nome, String email,
                                                      String googleSub) {
        return banco.jdbc().queryForObject("insert into " + banco.tabela("usuario")
                        + " (nome, email, google_sub, peso_kg, altura_cm, idade, objetivo, nivel) "
                        + "values (?, ?, ?, 80, 178, 30, 'PERDA_GORDURA', 'INICIANTE') returning id",
                Long.class, nome, email, googleSub);
    }
}
