package com.corpoforte.tracker.arquivos;

/**
 * Onde as imagens moram. Quem usa so' conhece esta interface: a
 * implementacao em disco (ArmazenamentoEmDisco) serve dev e teste, e outra
 * implementacao nao muda nada fora dela - inclusive o contrato da API,
 * porque URL assinada com expiracao e' o modelo que servicos de
 * armazenamento de objetos ja usam pra dar acesso temporario a um arquivo.
 *
 * Chave = caminho relativo gerado pelo servidor ("posts/<uuid>.jpg"), nunca
 * um nome vindo do cliente.
 */
public interface ArmazenamentoArquivos {

    void gravar(String chave, byte[] conteudo);

    /** Nao falha se ja nao existir: apagar e' idempotente. */
    void apagar(String chave);

    /**
     * URL que da acesso ao arquivo por tempo limitado, sem cabecalho de
     * autenticacao - <img src> e cache de imagem nao mandam Authorization.
     * So' deve ser gerada DEPOIS de decidir que quem pede pode ver o
     * conteudo: a URL em si e' a permissao.
     */
    String urlAssinada(String chave);
}
