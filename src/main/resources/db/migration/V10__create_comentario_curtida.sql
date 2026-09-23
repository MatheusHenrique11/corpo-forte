-- Fase 7b. Primeiras tabelas do projeto que referenciam OUTRA entidade
-- (post) e nao o usuario direto: ate aqui toda tabela pendurava em
-- usuario_id, porque todo dado era isolado por usuario. Comentario e
-- curtida pertencem a um post que pode ser de outra pessoa - e' o mesmo
-- compartilhamento intencional que a Fase 7a documentou em Post.java.
--
-- usuario_id continua existindo nas duas: nao pra restringir quem le
-- (qualquer um le o feed inteiro), so' pra saber quem escreveu/curtiu.
create table comentario (
    id          bigserial primary key,
    post_id     bigint not null references post(id),
    usuario_id  bigint not null references usuario(id),
    texto       varchar(300) not null,
    criado_em   timestamp not null
);

create index idx_comentario_post_id on comentario (post_id);

-- unique(post_id, usuario_id): uma curtida por usuario por post. Curtir de
-- novo e' DESCURTIR (toggle no PostService), entao duas linhas pro mesmo
-- par nunca sao legitimas - e dois cliques simultaneos no botao (duplo
-- clique, duas abas) nao podem virar duas curtidas. Mesma logica dos
-- outros unique compostos do projeto: registro_peso, treino_do_dia e
-- avaliacao_fisica (Fase 9) - quem segura a regra e' o banco, nao so' o
-- find-antes-de-salvar do service.
create table curtida (
    id          bigserial primary key,
    post_id     bigint not null references post(id),
    usuario_id  bigint not null references usuario(id),
    criado_em   timestamp not null,
    unique (post_id, usuario_id)
);

create index idx_curtida_post_id on curtida (post_id);
