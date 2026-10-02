-- Presenca na entrevista, revisao de documentos, tipo da notificacao e funcionarios.
-- Nenhum registro e apagado: so colunas novas (com padrao para as linhas existentes)
-- e uma tabela nova.

-- ---------------------------------------------------------------
-- 1. Presenca do candidato na entrevista marcada. Nula enquanto nao ha entrevista.
--    A data e hora da confirmacao fica em UTC, como entrevista_em.
-- ---------------------------------------------------------------
ALTER TABLE candidatura
    ADD COLUMN presenca ENUM('pendente', 'confirmado') NULL AFTER entrevista_em,
    ADD COLUMN presenca_confirmada_em DATETIME NULL AFTER presenca;

-- entrevistas ja marcadas passam a aguardar a confirmacao
UPDATE candidatura SET presenca = 'pendente' WHERE entrevista_em IS NOT NULL;

-- ---------------------------------------------------------------
-- 2. Revisao do RH sobre cada documento enviado. Os que ja existem ficam pendentes.
-- ---------------------------------------------------------------
ALTER TABLE documento
    ADD COLUMN status ENUM('pendente', 'aprovado', 'recusado') NOT NULL DEFAULT 'pendente' AFTER formato;

-- ---------------------------------------------------------------
-- 3. Tipo da notificacao e registro relacionado (candidatura ou vaga, conforme o tipo).
--    As notificacoes que ja existem eram todas de candidatura; a referencia delas fica nula.
-- ---------------------------------------------------------------
ALTER TABLE notificacao
    ADD COLUMN tipo ENUM('candidatura', 'nova_vaga') NOT NULL DEFAULT 'candidatura' AFTER usuario_id,
    ADD COLUMN referencia_id BIGINT UNSIGNED NULL AFTER tipo;

-- ---------------------------------------------------------------
-- 4. Funcionario: candidatura contratada. Inativar nao apaga nada.
--    CASCADE como nas outras tabelas filhas de candidatura.
-- ---------------------------------------------------------------
CREATE TABLE funcionario (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    candidatura_id   BIGINT UNSIGNED                 NOT NULL,
    status           ENUM('ativo', 'inativo')        NOT NULL DEFAULT 'ativo',
    data_contratacao DATETIME                        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_funcionario_candidatura (candidatura_id),
    CONSTRAINT fk_funcionario_candidatura
        FOREIGN KEY (candidatura_id) REFERENCES candidatura (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- candidaturas ja contratadas viram funcionarios (sem isso sumiriam da lista de candidatos e nao
-- apareceriam em lugar nenhum); a data vem do historico quando existe
INSERT INTO funcionario (candidatura_id, data_contratacao)
SELECT c.id,
       COALESCE((SELECT MAX(h.data_alteracao) FROM historico_status h
                 WHERE h.candidatura_id = c.id AND h.status_novo = 'contratado'), CURRENT_TIMESTAMP)
FROM candidatura c
WHERE c.status = 'contratado';
