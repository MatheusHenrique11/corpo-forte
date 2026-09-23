-- Fase 13. Seguir e' assimetrico e sem aprovacao: uma linha = "seguidor
-- segue seguido". Deixar de seguir apaga a linha, como a curtida.
--
-- As duas regras moram no banco, nao so' no service: um par so' uma vez
-- (duplo clique, duas abas) e ninguem segue a si mesmo. on delete cascade
-- nas duas pontas: conta apagada sai das listas de todo mundo.
create table seguimento (
    id           bigserial primary key,
    seguidor_id  bigint not null references usuario(id) on delete cascade,
    seguido_id   bigint not null references usuario(id) on delete cascade,
    criado_em    timestamp not null,
    constraint uk_seguimento_par unique (seguidor_id, seguido_id),
    constraint ck_seguimento_nao_segue_a_si check (seguidor_id <> seguido_id)
);

-- Listas de seguidores e de seguindo, paginadas pelo mesmo keyset
-- (criado_em, id) do resto da API: quem seguiu por ultimo aparece primeiro.
create index idx_seguimento_seguido on seguimento (seguido_id, criado_em desc, id desc);
create index idx_seguimento_seguidor on seguimento (seguidor_id, criado_em desc, id desc);

-- Busca de usuario por prefixo. text_pattern_ops e' o que deixa o
-- "like 'abc%'" usar o indice independente da collation do banco; o indice
-- unico da V15 em lower(username) nao serve pra like.
create index idx_usuario_username_prefixo on usuario (lower(username) text_pattern_ops);
create index idx_usuario_nome_prefixo on usuario (lower(nome) text_pattern_ops);
