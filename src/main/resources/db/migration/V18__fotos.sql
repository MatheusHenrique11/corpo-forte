-- Fase 15. Fotos de post e foto de perfil propria. No banco fica so' a
-- chave do arquivo (caminho gerado pelo servidor); a URL de acesso e'
-- assinada na hora de montar cada resposta, nunca guardada.
create table foto_post (
    id                bigserial primary key,
    post_id           bigint not null references post(id) on delete cascade,
    posicao           integer not null,
    chave             varchar(100) not null,
    chave_miniatura   varchar(100) not null,
    largura           integer not null,
    altura            integer not null,
    criado_em         timestamp not null,
    constraint uk_foto_post_posicao unique (post_id, posicao),
    constraint ck_foto_post_posicao check (posicao between 0 and 3)
);

-- Foto de perfil enviada pela pessoa. Quando existe, vale no lugar da foto
-- do Google (foto_url), que o login continua atualizando por baixo.
alter table usuario add column foto_chave varchar(100);
