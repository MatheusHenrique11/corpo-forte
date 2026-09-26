package com.corpoforte.tracker.social;

import com.corpoforte.tracker.usuario.Usuario;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Busca de contas, fora do UsuarioRepository porque aplica a regra de
 * bloqueio (que mora em social, e usuario nao pode depender de social).
 * Repository somente leitura sobre a mesma entidade.
 */
public interface BuscaDeContasRepository extends Repository<Usuario, Long> {

    /**
     * Prefixo do username ou do nome, sem diferenciar maiusculas (indices
     * text_pattern_ops da V16), so' contas com onboarding (sem username nao
     * ha perfil publico) e sem bloqueio com quem busca. O prefixo chega ja
     * escapado - "_" e' valido em username e e' curinga no like.
     */
    @Query("select u from Usuario u where u.username is not null "
            + "and (lower(u.username) like :prefixo escape '!' or lower(u.nome) like :prefixo escape '!') and "
            + RegraDeBloqueio.SEM_BLOQUEIO_COM_A_CONTA + " order by u.username")
    List<Usuario> buscarPorPrefixo(@Param("prefixo") String prefixo, @Param("quemVe") Long quemVe, Limit limite);
}
