-- Fase 10. A identidade de login sai de usuario.google_sub e vai pra uma
-- tabela propria: uma linha por (provedor, sub), apontando pro usuario.
-- Com uma coluna por provedor, cada provedor de login novo seria mais uma
-- coluna unique nullable em usuario e mais um ramo em cada consulta de
-- login; aqui e' so' mais um valor em "provedor".
--
-- unique (provedor, sub) herda a garantia que google_sub unique dava desde
-- a V1: duas contas nunca resolvem pro mesmo login, nem numa corrida entre
-- dois primeiros logins simultaneos.
create table identidade_externa (
    id          bigserial primary key,
    usuario_id  bigint not null references usuario(id) on delete cascade,
    provedor    varchar(20) not null,
    sub         varchar(255) not null,
    criado_em   timestamp not null,
    unique (provedor, sub)
);

create index idx_identidade_externa_usuario_id on identidade_externa (usuario_id);

-- Migracao de dado real: toda conta ja vinculada ao Google (Fase 6)
-- continua logando na MESMA conta depois do deploy. Sem isso, o proximo
-- login de cada usuario cairia no fluxo de "primeiro login" e criaria uma
-- conta nova vazia. Testado em MigracaoIdentidadeExternaIT.
insert into identidade_externa (usuario_id, provedor, sub, criado_em)
select id, 'GOOGLE', google_sub, now()
from usuario
where google_sub is not null;

alter table usuario drop column google_sub;
