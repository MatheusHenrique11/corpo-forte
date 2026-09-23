-- data_inicio_ciclo existia desde a V1 e nunca foi lido por nenhuma linha
-- do projeto. A Fase 8 (periodizacao) ancora o ciclo em
-- avaliacao_fisica.data_avaliacao, que ja e' atualizada quando o usuario
-- refaz a avaliacao - manter as duas colunas deixaria ambiguo qual data
-- governa o ciclo.
alter table usuario drop column data_inicio_ciclo;
