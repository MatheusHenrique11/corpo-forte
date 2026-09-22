create table usuario (
    id                  bigserial primary key,
    nome                varchar(120) not null,
    email               varchar(180) unique,
    google_sub          varchar(120) unique,
    role                varchar(20)  not null default 'USER',
    peso_kg             double precision not null,
    altura_cm           double precision not null,
    idade               integer not null,
    objetivo            varchar(30) not null,
    nivel               varchar(30) not null,
    data_inicio_ciclo   date not null
);
