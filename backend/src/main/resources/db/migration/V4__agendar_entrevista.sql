-- Data e hora da entrevista marcada pelo RH, sempre em UTC.
-- O fuso de exibicao (America/Sao_Paulo) e aplicado na saida.

ALTER TABLE candidatura ADD COLUMN entrevista_em DATETIME NULL AFTER status;
