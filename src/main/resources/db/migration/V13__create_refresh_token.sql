-- Fase 10. Refresh token da API: o valor em si so' existe no cliente; aqui
-- fica apenas o hash SHA-256 (hex, 64 caracteres). Um vazamento desta
-- tabela nao entrega sessao nenhuma - mesmo raciocinio de nunca guardar
-- senha em texto puro.
--
-- revogado_em em vez de apagar a linha na rotacao: um token ja usado
-- (rotacionado) que aparece de novo e' sinal de roubo, e so' da pra
-- reconhecer isso se a linha continuar existindo. Logout, ao contrario,
-- apaga a linha. on delete cascade: conta apagada leva as sessoes junto.
create table refresh_token (
    id          bigserial primary key,
    usuario_id  bigint not null references usuario(id) on delete cascade,
    token_hash  varchar(64) not null unique,
    criado_em   timestamp not null,
    expira_em   timestamp not null,
    revogado_em timestamp
);

create index idx_refresh_token_usuario_id on refresh_token (usuario_id);
