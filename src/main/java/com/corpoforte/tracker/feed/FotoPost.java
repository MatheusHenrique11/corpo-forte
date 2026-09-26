package com.corpoforte.tracker.feed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Uma foto de um post (ate 4, posicao 0-3). Guarda so' as chaves dos
 * arquivos: a URL e' assinada a cada resposta, depois da checagem de
 * visibilidade do post. Sai junto com o post pelo cascade do banco; os
 * ARQUIVOS quem apaga e' o PostService.apagarPost.
 */
@Entity
@Table(name = "foto_post")
public class FotoPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false)
    private int posicao;

    @Column(nullable = false)
    private String chave;

    @Column(name = "chave_miniatura", nullable = false)
    private String chaveMiniatura;

    @Column(nullable = false)
    private int largura;

    @Column(nullable = false)
    private int altura;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected FotoPost() {
    }

    public FotoPost(Long postId, int posicao, String chave, String chaveMiniatura, int largura, int altura,
                    LocalDateTime criadoEm) {
        this.postId = postId;
        this.posicao = posicao;
        this.chave = chave;
        this.chaveMiniatura = chaveMiniatura;
        this.largura = largura;
        this.altura = altura;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public int getPosicao() {
        return posicao;
    }

    public String getChave() {
        return chave;
    }

    public String getChaveMiniatura() {
        return chaveMiniatura;
    }

    public int getLargura() {
        return largura;
    }

    public int getAltura() {
        return altura;
    }
}
