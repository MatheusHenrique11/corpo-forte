package com.corpoforte.tracker.peso;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia do registro de peso: mesmo papel que UsuarioAtualService tem
 * pro perfil e AvaliacaoFisicaService pra avaliacao fisica. O Controller
 * nunca chama o repository direto.
 */
@Service
public class RegistroPesoService {

    private final RegistroPesoRepository registroPesoRepository;

    public RegistroPesoService(RegistroPesoRepository registroPesoRepository) {
        this.registroPesoRepository = registroPesoRepository;
    }

    public RegistroPeso salvar(Long usuarioId, LocalDate data, double pesoKg) {
        RegistroPeso registro = registroPesoRepository.findByUsuarioIdAndData(usuarioId, data)
                .orElseGet(() -> new RegistroPeso(usuarioId, data, pesoKg));

        registro.atualizarPeso(pesoKg);

        return registroPesoRepository.save(registro);
    }

    public List<RegistroPeso> listarDoUsuario(Long usuarioId) {
        return registroPesoRepository.findByUsuarioIdOrderByDataDesc(usuarioId);
    }

    public Optional<RegistroPeso> obterMaisRecenteDoUsuario(Long usuarioId) {
        return listarDoUsuario(usuarioId).stream().findFirst();
    }
}
