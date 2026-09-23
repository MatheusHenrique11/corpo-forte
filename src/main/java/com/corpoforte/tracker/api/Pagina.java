package com.corpoforte.tracker.api;

import java.util.List;
import java.util.function.Function;

/**
 * Formato de TODA lista da API: { "itens": [...], "proximoCursor": "..." }.
 * proximoCursor null = nao ha mais itens. Lista pequena e fechada (catalogo
 * de exercicios) usa o mesmo envelope com proximoCursor sempre null, pra
 * que paginar uma delas no futuro seja mudanca aditiva, nao troca de
 * formato.
 */
public record Pagina<T>(List<T> itens, String proximoCursor) {

    public static final int TAMANHO_PADRAO = 20;

    public static <T> Pagina<T> completa(List<T> itens) {
        return new Pagina<>(itens, null);
    }

    /**
     * Recebe o resultado de uma busca feita com tamanho + 1: o item a mais
     * so' existe pra saber se ha proxima pagina, sem um count separado. Ele
     * nao entra nos itens, e o cursor aponta pro ultimo item que entrou.
     */
    public static <T> Pagina<T> deBuscaComUmAMais(List<T> buscados, int tamanho, Function<T, Cursor> cursorDoItem) {
        if (buscados.size() <= tamanho) {
            return new Pagina<>(List.copyOf(buscados), null);
        }
        List<T> itens = List.copyOf(buscados.subList(0, tamanho));
        return new Pagina<>(itens, cursorDoItem.apply(itens.get(tamanho - 1)).codificar());
    }

    public <R> Pagina<R> mapear(Function<T, R> conversor) {
        return new Pagina<>(itens.stream().map(conversor).toList(), proximoCursor);
    }

    /** Pra conversao que precisa da pagina inteira de uma vez (ex.: buscar
     * autores de todos os itens numa consulta so', sem N+1). */
    public <R> Pagina<R> mapearTodos(Function<List<T>, List<R>> conversor) {
        return new Pagina<>(conversor.apply(itens), proximoCursor);
    }
}
