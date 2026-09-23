package com.corpoforte.tracker.usuario;

public enum MotivoUsernameIndisponivel {
    /** Fora de 3 a 30 caracteres [a-z0-9_.]. */
    FORMATO_INVALIDO,
    /** Nome do sistema (admin, api...), que ninguem pode usar. */
    RESERVADO,
    /** Ja e' de outra conta. */
    EM_USO
}
