package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.usuario.Visibilidade;
import jakarta.validation.constraints.Size;

/**
 * Campos de texto do post multipart. Diferente do PostForm, o texto aqui
 * e' opcional: post de foto pode vir sem legenda (a regra "texto ou foto"
 * fica no PublicacaoComFotosService, que sabe se vieram fotos).
 */
public class PostComFotosForm {

    @Size(max = 500, message = "Máximo de 500 caracteres")
    private String texto;

    private Visibilidade visibilidade;

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Visibilidade getVisibilidade() {
        return visibilidade;
    }

    public void setVisibilidade(Visibilidade visibilidade) {
        this.visibilidade = visibilidade;
    }
}
