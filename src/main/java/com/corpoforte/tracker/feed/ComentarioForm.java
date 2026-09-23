package com.corpoforte.tracker.feed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Limite menor que o do PostForm (300 vs. 500) de proposito: comentario
 * aparece aninhado dentro do post no feed, entao texto longo demais
 * atrapalha a leitura do post original.
 *
 * So' tem "texto": o post comentado vem no @PathVariable da URL, nao num
 * hidden input - ver o comentario de EndpointsComIdIT sobre os vetores de
 * ID vindos do cliente.
 */
public class ComentarioForm {

    @NotBlank(message = "Escreva algo antes de comentar")
    @Size(max = 300, message = "Máximo de 300 caracteres")
    private String texto;

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}
