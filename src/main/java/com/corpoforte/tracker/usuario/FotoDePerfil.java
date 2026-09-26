package com.corpoforte.tracker.usuario;

import com.corpoforte.tracker.arquivos.ArmazenamentoArquivos;
import com.corpoforte.tracker.arquivos.ProcessadorDeImagem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Foto de perfil (Fase 15). Qual mostrar: a enviada pela pessoa (URL
 * assinada), se existir; senao a do Google. O login continua atualizando a
 * do Google por baixo (Usuario.atualizarFoto), e ela volta a valer se a
 * pessoa remover a propria - mas nunca passa por cima da enviada.
 *
 * Todo lugar que mostra uma conta (feed, perfil, listas, /me) resolve a
 * foto por url(...), depois de ja ter decidido que quem pede pode ver a
 * conta.
 */
@Component
public class FotoDePerfil {

    /** Foto de perfil aparece pequena: 512 px no maior lado bastam. */
    static final int LADO_MAXIMO = 512;

    private static final Logger log = LoggerFactory.getLogger(FotoDePerfil.class);

    private final ArmazenamentoArquivos armazenamento;
    private final ProcessadorDeImagem processadorDeImagem;
    private final UsuarioRepository usuarioRepository;

    public FotoDePerfil(ArmazenamentoArquivos armazenamento, ProcessadorDeImagem processadorDeImagem,
                        UsuarioRepository usuarioRepository) {
        this.armazenamento = armazenamento;
        this.processadorDeImagem = processadorDeImagem;
        this.usuarioRepository = usuarioRepository;
    }

    public String url(Usuario usuario) {
        return usuario.getFotoChave() != null
                ? armazenamento.urlAssinada(usuario.getFotoChave())
                : usuario.getFotoUrl();
    }

    /** Mesmo processamento das fotos de post: tipo pelos bytes, sem EXIF. O
     * arquivo anterior sai depois que a conta ja aponta pro novo. */
    public Usuario trocar(Usuario usuario, byte[] original) {
        byte[] foto = processadorDeImagem.processar(original, LADO_MAXIMO, 0).principal();
        String chave = "perfis/" + UUID.randomUUID() + ".jpg";
        armazenamento.gravar(chave, foto);
        String anterior = usuario.definirFotoPropria(chave);
        Usuario salvo = usuarioRepository.save(usuario);
        apagarSemFalhar(anterior);
        return salvo;
    }

    /** Volta pra foto do Google (ou nenhuma). Idempotente. */
    public Usuario remover(Usuario usuario) {
        String anterior = usuario.definirFotoPropria(null);
        Usuario salvo = usuarioRepository.save(usuario);
        apagarSemFalhar(anterior);
        return salvo;
    }

    private void apagarSemFalhar(String chave) {
        if (chave == null) {
            return;
        }
        try {
            armazenamento.apagar(chave);
        } catch (RuntimeException e) {
            log.warn("Nao foi possivel apagar a foto de perfil anterior {}", chave, e);
        }
    }
}
