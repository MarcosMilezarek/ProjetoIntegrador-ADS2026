-- Requisitos da vaga separados por peso: obrigatorios, desejaveis e diferenciais (um requisito por linha).
-- Nenhum registro e apagado: so colunas novas. O conteudo atual de requisitos passa a valer como obrigatorio
-- (o legado nao tem como ser classificado); desejaveis e diferenciais comecam vazios (NULL).
-- A coluna requisitos continua na tabela, mas deixa de ser lida e escrita; a remocao fica para depois.

ALTER TABLE vaga ADD COLUMN requisitos_obrigatorios TEXT NULL AFTER requisitos;
ALTER TABLE vaga ADD COLUMN requisitos_desejaveis   TEXT NULL AFTER requisitos_obrigatorios;
ALTER TABLE vaga ADD COLUMN requisitos_diferenciais TEXT NULL AFTER requisitos_desejaveis;

UPDATE vaga SET requisitos_obrigatorios = requisitos;
