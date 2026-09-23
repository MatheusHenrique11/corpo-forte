package com.corpoforte.tracker.usuario;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Mensagem conforme o motivo, pro cliente mostrar embaixo do campo. */
public class UsernameValidoValidator implements ConstraintValidator<UsernameValido, String> {

    @Override
    public boolean isValid(String username, ConstraintValidatorContext contexto) {
        if (username == null) {
            return true;
        }
        return RegraUsername.problemaSemConsultarBanco(username)
                .map(motivo -> {
                    contexto.disableDefaultConstraintViolation();
                    contexto.buildConstraintViolationWithTemplate(mensagem(motivo)).addConstraintViolation();
                    return false;
                })
                .orElse(true);
    }

    private static String mensagem(MotivoUsernameIndisponivel motivo) {
        return motivo == MotivoUsernameIndisponivel.RESERVADO
                ? "Esse nome de usuário é reservado"
                : "Use de 3 a 30 caracteres: letras minúsculas, números, _ e .";
    }
}
