-- Fase 14. Visibilidade por post e bloqueio entre contas.
--
-- Todo post que ja existe continua PUBLICO (era o unico comportamento ate
-- aqui), e toda conta comeca com PUBLICO como padrao pros proximos posts.
alter table post add column visibilidade varchar(20) not null default 'PUBLICO';
alter table usuario add column visibilidade_padrao varchar(20) not null default 'PUBLICO';

-- "bloqueador bloqueou bloqueado". O efeito e' nos dois sentidos (nenhum
-- dos dois ve o conteudo do outro), mas a linha guarda quem bloqueou: so'
-- ele pode desbloquear. Mesmas regras de banco do seguimento (V16).
create table bloqueio (
    id             bigserial primary key,
    bloqueador_id  bigint not null references usuario(id) on delete cascade,
    bloqueado_id   bigint not null references usuario(id) on delete cascade,
    criado_em      timestamp not null,
    constraint uk_bloqueio_par unique (bloqueador_id, bloqueado_id),
    constraint ck_bloqueio_nao_bloqueia_a_si check (bloqueador_id <> bloqueado_id)
);

-- A regra de visibilidade pergunta "existe bloqueio entre A e B" nos dois
-- sentidos: o unique cobre (bloqueador, bloqueado), este cobre o inverso.
create index idx_bloqueio_bloqueado on bloqueio (bloqueado_id, bloqueador_id);
-- lista "quem eu bloqueei", mais recente primeiro
create index idx_bloqueio_bloqueador_criado_em on bloqueio (bloqueador_id, criado_em desc, id desc);
