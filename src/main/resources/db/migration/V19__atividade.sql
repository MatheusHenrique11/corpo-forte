-- Fase 16. Treino realizado (atividade) e a medida de cada exercicio.
--
-- Isometrico e' medido em segundos, nao em repeticoes. O catalogo da V5
-- tinha cinco, tratados como repeticao ate aqui: classificados pelo nome
-- (seed fixo, os nomes nao mudam).
alter table exercicio add column medida varchar(20) not null default 'REPETICOES';
update exercicio set medida = 'SEGUNDOS' where nome in (
    'Apoio de ombros na parede',
    'Apoio invertido livre na parede',
    'Apoio invertido livre sem parede',
    'Agachamento na parede (wall sit)',
    'Front lever tuck (isometrico)'
);

-- Diario de treino: privado por natureza. treino_do_dia_id liga a
-- atividade ao treino gerado que ela finalizou (unico: um treino do dia
-- vira no maximo uma atividade); treino livre fica sem.
create table atividade (
    id                  bigserial primary key,
    usuario_id          bigint not null references usuario(id) on delete cascade,
    data                date not null,
    origem              varchar(20) not null,
    treino_do_dia_id    bigint references treino_do_dia(id) on delete set null,
    duracao_minutos     integer,
    esforco_percebido   integer,
    notas               varchar(500),
    criado_em           timestamp not null,
    constraint uk_atividade_treino_do_dia unique (treino_do_dia_id),
    constraint ck_atividade_origem check (origem in ('TREINO_DO_DIA', 'LIVRE')),
    constraint ck_atividade_esforco check (esforco_percebido between 1 and 10),
    constraint ck_atividade_duracao check (duracao_minutos > 0)
);

-- diario da conta, mais recente primeiro (keyset data, id)
create index idx_atividade_usuario_data on atividade (usuario_id, data desc, id desc);

-- Uma linha por serie feita, na ordem em que aconteceu. valor e' repeticao
-- ou segundo, conforme a medida do exercicio.
create table atividade_serie (
    id            bigserial primary key,
    atividade_id  bigint not null references atividade(id) on delete cascade,
    exercicio_id  bigint not null references exercicio(id),
    ordem         integer not null,
    valor         integer not null,
    constraint uk_atividade_serie_ordem unique (atividade_id, ordem),
    constraint ck_atividade_serie_valor check (valor > 0)
);
