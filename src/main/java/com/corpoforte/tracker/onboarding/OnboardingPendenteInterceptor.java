package com.corpoforte.tracker.onboarding;

import com.corpoforte.tracker.api.PermitidoSemOnboarding;
import com.corpoforte.tracker.api.ProblemaApi;
import com.corpoforte.tracker.usuario.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Barra a API pra conta que ainda nao fez o onboarding: toda rota
 * responde 409 onboarding-pendente, menos as marcadas com
 * @PermitidoSemOnboarding. O cliente usa esse "type" pra saber que tem que
 * mostrar o cadastro inicial.
 *
 * Interceptor, e nao uma checagem dentro de cada controller: rota nova
 * nasce barrada sem ninguem lembrar de nada. So' age em metodo de
 * controller (HandlerMethod) - rota inexistente continua dando 404. Sem
 * access token (rotas de /auth) nao ha conta pra checar; conta que nao
 * existe mais segue pro controller, que responde 401.
 *
 * So' a API: as telas web continuam como estavam, sem onboarding.
 */
@Component
public class OnboardingPendenteInterceptor implements HandlerInterceptor {

    private final UsuarioRepository usuarioRepository;

    public OnboardingPendenteInterceptor(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo) || liberado(metodo)) {
            return true;
        }

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacao instanceof JwtAuthenticationToken token)) {
            return true;
        }

        boolean pendente = usuarioRepository.findOnboardingConcluidoPorId(Long.valueOf(token.getName()))
                .map(concluido -> !concluido)
                .orElse(false);
        if (pendente) {
            throw ProblemaApi.onboardingPendente();
        }
        return true;
    }

    private static boolean liberado(HandlerMethod metodo) {
        return metodo.hasMethodAnnotation(PermitidoSemOnboarding.class)
                || metodo.getBeanType().isAnnotationPresent(PermitidoSemOnboarding.class);
    }
}
