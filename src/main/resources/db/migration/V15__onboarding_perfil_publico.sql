-- Fase 12. Identidade publica da conta (username, bio, foto) e o marcador
-- de onboarding. Tudo nasce nullable/false: a conta existe desde o
-- primeiro login, antes de a pessoa escolher username.
alter table usuario add column username varchar(30);
alter table usuario add column bio varchar(160);
alter table usuario add column foto_url varchar(500);
alter table usuario add column onboarding_concluido boolean not null default false;

-- Contas que ja existem ja usam o app com dado de verdade: entram como
-- onboarding concluido, sem serem barradas na primeira chamada depois do
-- deploy. Ganham um username gerado ('atleta' + id: unico, e no formato
-- permitido) que a pessoa pode trocar depois. Testado em MigracaoOnboardingIT.
update usuario set onboarding_concluido = true, username = 'atleta' || id;

-- Username unico sem diferenciar maiusculas: "Joao" e "joao" nunca sao
-- duas contas. O indice e' em lower(username), o mesmo lower() que as
-- buscas por username usam.
create unique index uk_usuario_username on usuario (lower(username));

-- Conta com onboarding concluido sempre tem username: e' o que permite
-- chegar ao perfil publico dela. Garantido no banco, nao so' no codigo.
alter table usuario add constraint ck_usuario_onboarding_com_username
    check (not onboarding_concluido or username is not null);

-- Posts de um autor em ordem de feed (perfil publico, paginado por
-- cursor): mesmo keyset (criado_em, id) do feed, filtrado por usuario.
create index idx_post_usuario_criado_em_id on post (usuario_id, criado_em desc, id desc);
