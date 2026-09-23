-- Fase 7c. Apagar post passa a existir, e apagar e' hard delete: o que o
-- usuario apaga sai do banco, junto com o que so' existe por causa dele
-- (privacidade/LGPD - nao existe coluna "deletado"). Comentario e curtida nao tem
-- sentido sem o post, entao a FK passa a cascatear - quem garante que
-- nada fica orfao e' o banco, nao um delete em laco no service que alguem
-- pode esquecer de atualizar quando mais tabelas passarem a pendurar em
-- post.
--
-- Os nomes das constraints sao os gerados pelo Postgres (V10 declarou as
-- FKs inline, sem nome explicito) - mesmo caso do V9. Nome errado aqui
-- nao e' bug sutil: o Flyway falha e a aplicacao nao sobe, e o mvn verify
-- (V1->V11 contra Postgres real) e' quem confirma.
alter table comentario drop constraint comentario_post_id_fkey;
alter table comentario
    add constraint comentario_post_id_fkey foreign key (post_id) references post(id) on delete cascade;

alter table curtida drop constraint curtida_post_id_fkey;
alter table curtida
    add constraint curtida_post_id_fkey foreign key (post_id) references post(id) on delete cascade;
