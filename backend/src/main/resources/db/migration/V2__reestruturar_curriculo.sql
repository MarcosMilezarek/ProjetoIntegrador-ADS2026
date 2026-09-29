-- Reestruturacao do curriculo (RF04/RF05).
--
-- Motivacao: usuario -> candidato -> curriculo eram tres tabelas 1:1 para a mesma
-- pessoa, o que tornava os nomes indistinguiveis. A tabela candidato deixa de
-- existir: seus dados pessoais/contato passam para curriculo, que aponta direto
-- para usuario. Formacao e experiencia, antes campos TEXT livres, viram tabelas
-- filhas 1:N com campos tipados.
--
-- usuario     = conta de acesso (login, perfil, status)
-- curriculo   = dados do candidato (pessoais, contato, resumo, certificacoes)
-- curriculo_formacao / curriculo_experiencia / curriculo_arquivo = detalhes 1:N / 1:1

-- ---------------------------------------------------------------
-- 1. curriculo passa a referenciar usuario direto
-- ---------------------------------------------------------------
ALTER TABLE curriculo ADD COLUMN usuario_id BIGINT UNSIGNED NULL AFTER id;

-- candidato.usuario_id era a PK compartilhada, logo candidato_id == usuario.id
UPDATE curriculo SET usuario_id = candidato_id;

ALTER TABLE curriculo DROP FOREIGN KEY fk_curriculo_candidato;
ALTER TABLE curriculo DROP INDEX uk_curriculo_candidato;

-- ---------------------------------------------------------------
-- 2. dados pessoais e contato migram de candidato para curriculo
-- ---------------------------------------------------------------
ALTER TABLE curriculo
    ADD COLUMN data_nascimento DATE                                                     NULL AFTER usuario_id,
    ADD COLUMN sexo            ENUM('feminino', 'masculino', 'outro', 'nao_informado')  NULL AFTER data_nascimento,
    ADD COLUMN cidade          VARCHAR(100)                                             NULL AFTER sexo,
    ADD COLUMN uf              CHAR(2)                                                  NULL AFTER cidade,
    ADD COLUMN numero_contato  VARCHAR(20)                                              NULL AFTER uf,
    ADD COLUMN perfil_linkedin VARCHAR(255)                                             NULL AFTER numero_contato,
    ADD COLUMN certificacoes   TEXT                                                     NULL AFTER competencias;

UPDATE curriculo c
    JOIN candidato a ON a.usuario_id = c.usuario_id
SET c.data_nascimento = a.data_nascimento,
    c.cidade          = a.cidade,
    c.uf              = a.uf,
    c.numero_contato  = a.telefone,
    c.perfil_linkedin = a.linkedin_url;

ALTER TABLE curriculo
    MODIFY COLUMN usuario_id BIGINT UNSIGNED NOT NULL,
    ADD UNIQUE KEY uk_curriculo_usuario (usuario_id),
    ADD CONSTRAINT fk_curriculo_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE curriculo DROP COLUMN candidato_id;

-- formacao e experiencias viram tabelas filhas tipadas (itens 2.1 e 2.2)
ALTER TABLE curriculo DROP COLUMN formacao;
ALTER TABLE curriculo DROP COLUMN experiencias;

-- ---------------------------------------------------------------
-- 3. candidatura passa a referenciar usuario (candidato sera removida)
-- ---------------------------------------------------------------
ALTER TABLE candidatura DROP FOREIGN KEY fk_candidatura_candidato;
ALTER TABLE candidatura DROP INDEX uk_candidatura_candidato_vaga;
ALTER TABLE candidatura DROP INDEX idx_candidatura_candidato;
ALTER TABLE candidatura CHANGE COLUMN candidato_id usuario_id BIGINT UNSIGNED NOT NULL;
ALTER TABLE candidatura
    ADD UNIQUE KEY uk_candidatura_usuario_vaga (usuario_id, vaga_id),
    ADD CONSTRAINT fk_candidatura_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;
CREATE INDEX idx_candidatura_usuario ON candidatura (usuario_id);

DROP TABLE candidato;

-- ---------------------------------------------------------------
-- 4. formacao academica (item 2.2)
-- ---------------------------------------------------------------
CREATE TABLE curriculo_formacao (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    curriculo_id BIGINT UNSIGNED NOT NULL,
    curso        VARCHAR(150)    NOT NULL,
    instituicao  VARCHAR(150)    NOT NULL,
    data_inicio  DATE            NOT NULL,
    -- nula quando o curso esta em andamento (sem previsao de termino)
    data_termino DATE            NULL,
    CONSTRAINT fk_formacao_curriculo
        FOREIGN KEY (curriculo_id) REFERENCES curriculo (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_formacao_curriculo ON curriculo_formacao (curriculo_id);

-- ---------------------------------------------------------------
-- 5. experiencia profissional (item 2.1)
-- ---------------------------------------------------------------
CREATE TABLE curriculo_experiencia (
    id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    curriculo_id         BIGINT UNSIGNED NOT NULL,
    cargo                VARCHAR(150)    NOT NULL,
    empresa              VARCHAR(150)    NOT NULL,
    data_contratacao     DATE            NOT NULL,
    data_demissao        DATE            NULL,
    trabalho_atual       BOOLEAN         NOT NULL DEFAULT FALSE,
    descricao_atividades TEXT            NULL,
    CONSTRAINT fk_experiencia_curriculo
        FOREIGN KEY (curriculo_id) REFERENCES curriculo (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    -- emprego atual nao tem data de demissao
    CONSTRAINT chk_experiencia_trabalho_atual
        CHECK (trabalho_atual = FALSE OR data_demissao IS NULL),
    CONSTRAINT chk_experiencia_periodo
        CHECK (data_demissao IS NULL OR data_demissao >= data_contratacao)
) ENGINE = InnoDB;

CREATE INDEX idx_experiencia_curriculo ON curriculo_experiencia (curriculo_id);

-- ---------------------------------------------------------------
-- 6. arquivo PDF do curriculo (item 2.6)
-- ---------------------------------------------------------------
CREATE TABLE curriculo_arquivo (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    curriculo_id       BIGINT UNSIGNED NOT NULL,
    nome_original      VARCHAR(255)    NOT NULL,
    -- nome gerado em disco (UUID); o diretorio vem de app.upload.dir
    nome_armazenado    VARCHAR(255)    NOT NULL,
    content_type       VARCHAR(100)    NOT NULL,
    tamanho_bytes      INT UNSIGNED    NOT NULL,
    enviado_em         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_arquivo_curriculo (curriculo_id),
    CONSTRAINT fk_arquivo_curriculo
        FOREIGN KEY (curriculo_id) REFERENCES curriculo (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_arquivo_tamanho
        CHECK (tamanho_bytes <= 5242880)
) ENGINE = InnoDB;
