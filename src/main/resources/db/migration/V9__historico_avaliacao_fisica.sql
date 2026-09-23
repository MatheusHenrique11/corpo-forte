-- Ate a Fase 8, avaliacao_fisica tinha uma linha por usuario (sobrescrita a
-- cada novo teste). A Fase 8 passou a pedir reavaliacao ao fim de cada ciclo
-- de 8 semanas - e obedecer esse aviso apagava a medicao anterior, destruindo
-- justamente a comparacao "eu fazia 10 barras, agora faco 15".
--
-- Passa a ser uma avaliacao POR DIA por usuario (mesma convencao de
-- registro_peso e treino_do_dia): corrigir um numero errado no mesmo dia
-- atualiza a linha do dia em vez de poluir o historico com uma medicao falsa.
alter table avaliacao_fisica drop constraint avaliacao_fisica_usuario_id_key;

alter table avaliacao_fisica
    add constraint avaliacao_fisica_usuario_id_data_key unique (usuario_id, data_avaliacao);
