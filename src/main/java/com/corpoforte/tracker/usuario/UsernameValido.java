package com.corpoforte.tracker.usuario;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Formato e nome reservado (RegraUsername). null passa, como nas
 * anotacoes padrao: quem exige presenca e' o @NotBlank ao lado.
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UsernameValidoValidator.class)
public @interface UsernameValido {

    String message() default "Nome de usuário inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
