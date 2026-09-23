package com.corpoforte.tracker.usuario;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Formato e nomes reservados do username - o que da pra decidir sem banco.
 * Usado pela Bean Validation (@UsernameValido) e pela consulta de
 * disponibilidade, pra as duas nunca discordarem. "Ja esta em uso" precisa
 * do banco e mora no UsernameService.
 *
 * So' minusculas: "Joao" e "joao" seriam a mesma pessoa pra quem le, e o
 * indice unico em lower(username) (V15) segura isso tambem no banco.
 */
public final class RegraUsername {

    private static final Pattern FORMATO = Pattern.compile("[a-z0-9_.]{3,30}");

    /** Nomes que se confundiriam com o proprio sistema ou com rota da API. */
    private static final Set<String> RESERVADOS = Set.of(
            "admin", "administrador", "adm", "root", "sistema", "suporte", "ajuda", "oficial",
            "moderador", "moderacao", "seguranca", "corpoforte", "corpo_forte", "corpo.forte",
            "api", "auth", "login", "logout", "me", "feed", "perfil", "usuarios", "usernames",
            "onboarding", "config", "configuracoes", "conta", "sobre", "termos", "privacidade",
            "dev", "www", "null", "undefined");

    private RegraUsername() {
    }

    public static Optional<MotivoUsernameIndisponivel> problemaSemConsultarBanco(String candidato) {
        if (candidato == null || !FORMATO.matcher(candidato).matches()) {
            return Optional.of(MotivoUsernameIndisponivel.FORMATO_INVALIDO);
        }
        if (RESERVADOS.contains(candidato)) {
            return Optional.of(MotivoUsernameIndisponivel.RESERVADO);
        }
        return Optional.empty();
    }
}
