package com.corpoforte.tracker.feed;

import com.corpoforte.tracker.usuario.Visibilidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PostForm {

    @NotBlank(message = "Escreva algo antes de postar")
    @Size(max = 500, message = "Máximo de 500 caracteres")
    private String texto;

    /** Opcional: sem ela, vale o padrao da conta. A tela nao manda. */
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
