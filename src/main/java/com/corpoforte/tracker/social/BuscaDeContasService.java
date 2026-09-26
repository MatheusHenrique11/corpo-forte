package com.corpoforte.tracker.social;

import com.corpoforte.tracker.usuario.Usuario;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuscaDeContasService {

    private final BuscaDeContasRepository buscaDeContasRepository;

    public BuscaDeContasService(BuscaDeContasRepository buscaDeContasRepository) {
        this.buscaDeContasRepository = buscaDeContasRepository;
    }

    /**
     * Contas cujo username ou nome comeca com o texto buscado. Os curingas
     * do like (% e _) e o caractere de escape viram literais: buscar
     * "joao_" nao pode achar "joaox".
     */
    public List<Usuario> buscarPorPrefixo(String busca, Long quemVe, int limite) {
        String texto = busca == null ? "" : busca.trim().toLowerCase();
        if (texto.isEmpty()) {
            return List.of();
        }
        String escapado = texto.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return buscaDeContasRepository.buscarPorPrefixo(escapado + "%", quemVe, Limit.of(limite));
    }
}
