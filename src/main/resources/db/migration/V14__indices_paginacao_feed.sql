-- Fase 11. Indices da paginacao por cursor (keyset) do feed e dos
-- comentarios: "o que vem depois de (criado_em, id)" vira uma busca no
-- indice que para no tamanho da pagina, em vez de ordenar a tabela
-- inteira a cada pagina.
create index idx_post_criado_em_id on post (criado_em desc, id desc);

-- (post_id, criado_em, id) atende a pagina de comentarios de um post e os
-- comentarios mais recentes por post do feed. Cobre tambem a busca so' por
-- post_id, entao o indice simples da V10 fica redundante.
create index idx_comentario_post_criado_em_id on comentario (post_id, criado_em, id);
drop index idx_comentario_post_id;
