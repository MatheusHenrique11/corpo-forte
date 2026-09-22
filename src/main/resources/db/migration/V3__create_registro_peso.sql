create table registro_peso (
    id          bigserial primary key,
    usuario_id  bigint not null references usuario(id),
    data        date not null,
    peso_kg     double precision not null,
    unique (usuario_id, data)
);
