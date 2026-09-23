package com.corpoforte.tracker.usuario;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Contas que nenhum login alcanca - na pratica, a conta local de antes
     * do login existir (Fase 6), esperando o dono reivindicar. Era
     * findByGoogleSubIsNull ate a identidade sair da tabela usuario (V12).
     */
    @Query("select u from Usuario u where not exists "
            + "(select 1 from IdentidadeExterna i where i.usuarioId = u.id)")
    List<Usuario> findSemIdentidadeExterna();

    /** Buscas por username usam lower(), o mesmo do indice unico da V15. */
    @Query("select u from Usuario u where lower(u.username) = lower(:username)")
    Optional<Usuario> findPorUsername(@Param("username") String username);

    @Query("select u.id from Usuario u where lower(u.username) = lower(:username)")
    Optional<Long> findIdPorUsername(@Param("username") String username);

    /**
     * Busca por prefixo do username ou do nome, sem diferenciar maiusculas
     * (indices text_pattern_ops da V16). So' conta com onboarding: sem
     * username nao ha perfil publico pra abrir. O prefixo chega ja escapado
     * (UsernameService.buscarPorPrefixo) - "_" e' valido em username e e'
     * curinga no like.
     */
    @Query("select u from Usuario u where u.username is not null "
            + "and (lower(u.username) like :prefixo escape '!' or lower(u.nome) like :prefixo escape '!') "
            + "order by u.username")
    List<Usuario> buscarPorPrefixo(@Param("prefixo") String prefixo, Limit limite);

    /** So' a coluna, sem carregar a conta: e' consultado em toda requisicao
     * da API (OnboardingPendenteInterceptor). */
    @Query("select u.onboardingConcluido from Usuario u where u.id = :id")
    Optional<Boolean> findOnboardingConcluidoPorId(@Param("id") Long id);
}
