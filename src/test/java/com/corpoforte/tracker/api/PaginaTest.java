package com.corpoforte.tracker.api;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginaTest {

    private static final LocalDate DIA = LocalDate.of(2026, 1, 1);

    @Test
    void buscaComUmAMaisGeraCursorDoUltimoItemQueEntrou() {
        Pagina<Integer> pagina = Pagina.deBuscaComUmAMais(List.of(1, 2, 3), 2, numero -> Cursor.apos(DIA, numero));

        assertThat(pagina.itens()).containsExactly(1, 2);
        assertThat(Cursor.decodificar(pagina.proximoCursor()).id()).isEqualTo(2);
    }

    /** Exatamente o tamanho da pagina: acabou, sem cursor - o cliente nao
     * precisa pedir uma pagina vazia pra descobrir isso. */
    @Test
    void semOItemAMaisNaoHaProximaPagina() {
        Pagina<Integer> pagina = Pagina.deBuscaComUmAMais(List.of(1, 2), 2, numero -> Cursor.apos(DIA, numero));

        assertThat(pagina.itens()).containsExactly(1, 2);
        assertThat(pagina.proximoCursor()).isNull();
    }

    @Test
    void mapearMantemOCursor() {
        Pagina<String> pagina = Pagina.deBuscaComUmAMais(List.of(1, 2, 3), 2, numero -> Cursor.apos(DIA, numero))
                .mapear(String::valueOf);

        assertThat(pagina.itens()).containsExactly("1", "2");
        assertThat(pagina.proximoCursor()).isNotNull();
    }
}
