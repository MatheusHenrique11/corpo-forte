create table post (
    id          bigserial primary key,
    usuario_id  bigint not null references usuario(id),
    texto       varchar(500) not null,
    criado_em   timestamp not null
);
