package com.corpoforte.tracker.usuario;

/**
 * Quem pode ver um post. Mora em usuario (vocabulario compartilhado, como
 * Equipamento e NivelTreino) porque e' tambem a preferencia padrao da
 * conta, e usuario nao pode depender de feed.
 */
public enum Visibilidade {
    /** Qualquer conta, menos as com bloqueio. */
    PUBLICO,
    /** So' quem segue o autor. */
    SEGUIDORES,
    /** So' o autor. */
    SOMENTE_EU
}
