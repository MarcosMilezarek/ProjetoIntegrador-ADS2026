-- Aderencia do candidato a vaga, em porcentagem inteira (0 a 100), dada pela IA na triagem.
-- Nenhum registro e apagado: so uma coluna nova, nula para as analises que ja existem
-- (e para as que ficam PENDENTE ou FALHA).

ALTER TABLE analise_candidatura
    ADD COLUMN aderencia TINYINT UNSIGNED NULL AFTER status,
    ADD CONSTRAINT chk_analise_aderencia CHECK (aderencia IS NULL OR aderencia BETWEEN 0 AND 100);
