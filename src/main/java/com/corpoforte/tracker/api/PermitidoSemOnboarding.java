package com.corpoforte.tracker.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca a rota da API que funciona antes do onboarding (a conta, o proprio
 * onboarding e o que ele precisa). Todo o resto responde
 * onboarding-pendente sem precisar lembrar de nada: rota nova nasce
 * barrada, e liberar e' uma decisao visivel no proprio metodo, conferida
 * por OnboardingApiIT.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PermitidoSemOnboarding {
}
