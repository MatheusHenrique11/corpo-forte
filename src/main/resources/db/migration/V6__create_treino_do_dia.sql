create table treino_do_dia (
    id          bigserial primary key,
    usuario_id  bigint not null references usuario(id),
    data        date not null,
    unique (usuario_id, data)
);

create table treino_item (
    id               bigserial primary key,
    treino_do_dia_id bigint not null references treino_do_dia(id),
    exercicio_id     bigint not null references exercicio(id),
    series           integer not null,
    repeticoes       integer not null,
    concluido        boolean not null default false
);
