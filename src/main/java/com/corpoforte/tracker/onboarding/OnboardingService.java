package com.corpoforte.tracker.onboarding;

import com.corpoforte.tracker.api.ProblemaApi;
import com.corpoforte.tracker.peso.RegistroPesoService;
import com.corpoforte.tracker.usuario.UsernameService;
import com.corpoforte.tracker.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class OnboardingService {

    private final UsernameService usernameService;
    private final RegistroPesoService registroPesoService;

    public OnboardingService(UsernameService usernameService, RegistroPesoService registroPesoService) {
        this.usernameService = usernameService;
        this.registroPesoService = registroPesoService;
    }

    /**
     * Troca os dados inventados da conta nova pelos reais, numa transacao
     * so': ou a conta sai com perfil, username e primeiro registro de peso,
     * ou continua pendente inteira.
     *
     * O peso vira o primeiro RegistroPeso (que tambem atualiza
     * Usuario.pesoKg), e nao um valor solto no perfil: a fonte unica de peso
     * desde a Fase 3 continua sendo o registro.
     */
    @Transactional
    public Usuario concluir(Usuario usuario, OnboardingForm form) {
        if (usuario.isOnboardingConcluido()) {
            throw ProblemaApi.onboardingJaConcluido();
        }
        usernameService.exigirLivre(form.getUsername(), usuario.getId());

        usuario.concluirOnboarding(form.getNome(), form.getUsername(), form.getAlturaCm(), form.getIdade(),
                form.getObjetivo(), form.getNivel());
        Usuario salvo = usernameService.salvarComUsernameUnico(usuario);

        registroPesoService.registrar(salvo, LocalDate.now(), form.getPesoKg());
        return salvo;
    }
}
