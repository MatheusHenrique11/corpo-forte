create table avaliacao_fisica (
    id                      bigserial primary key,
    usuario_id              bigint not null unique references usuario(id),
    data_avaliacao          date not null,
    reps_puxar_vertical     integer not null,
    reps_empurrar_vertical  integer not null,
    reps_pernas_bilateral   integer not null,
    reps_puxar_horizontal   integer not null,
    reps_empurrar_horizontal integer not null,
    reps_pernas_unilateral  integer not null
);
