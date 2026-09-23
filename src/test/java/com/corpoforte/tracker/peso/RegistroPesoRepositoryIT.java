package com.corpoforte.tracker.peso;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.OidcTestUsers;
import com.corpoforte.tracker.usuario.Usuario;
import com.corpoforte.tracker.usuario.UsuarioAtualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RegistroPesoService.salvar(...) evita duplicata fazendo upsert
 * (busca por usuarioId+data antes de inserir), mas isso so protege quem
 * passa por ele. Este teste confirma que "um peso por dia" tambem e' uma
 * constraint de verdade no banco (unique index criado na V3) - sem isso,
 * duas requisicoes concorrentes pra mesma data ainda conseguiriam criar
 * duas linhas, porque as duas passariam pelo find-antes-de-inserir do
 * service sem ver a outra ainda.
 *
 * Insere direto via repository (contornando o service de proposito) pra
 * garantir que quem segura essa regra e' o banco, nao so o codigo Java.
 */
@Transactional
class RegistroPesoRepositoryIT extends IntegrationTestBase {

    @Autowired
    private RegistroPesoRepository registroPesoRepository;

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @Test
    void bancoRejeitaDoisRegistrosNaMesmaDataParaOMesmoUsuario() {
        Usuario usuario = usuarioAtualService.obterUsuarioAtual(
                OidcTestUsers.principal("sub-registro-peso-constraint", "Usuaria Teste", "teste@exemplo.com"));
        LocalDate data = LocalDate.of(2026, 3, 1);

        registroPesoRepository.saveAndFlush(new RegistroPeso(usuario.getId(), data, 80.0));

        assertThatThrownBy(() ->
                registroPesoRepository.saveAndFlush(new RegistroPeso(usuario.getId(), data, 81.0))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
