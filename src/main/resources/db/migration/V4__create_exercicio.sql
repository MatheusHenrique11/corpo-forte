create table exercicio (
    id                      bigserial primary key,
    nome                    varchar(120) not null,
    movimento               varchar(30) not null,
    nivel                   varchar(30) not null,
    equipamento_necessario  varchar(30) not null
);

create table usuario_equipamento (
    usuario_id   bigint not null references usuario(id),
    equipamento  varchar(30) not null,
    primary key (usuario_id, equipamento)
);
