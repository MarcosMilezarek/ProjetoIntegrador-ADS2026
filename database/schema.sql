-- Schema MySQL - Sistema de Processo Seletivo
-- Ordem de criacao respeita as dependencias de chave estrangeira.

CREATE DATABASE IF NOT EXISTS selecao_rh
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE selecao_rh;

-- ---------------------------------------------------------------
-- usuario
-- ---------------------------------------------------------------
CREATE TABLE usuario (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(150)                                  NOT NULL,
    email         VARCHAR(150)                                  NOT NULL,
    senha_hash    VARCHAR(255)                                  NOT NULL,
    perfil        ENUM('candidato', 'rh', 'administrador')      NOT NULL,
    status        ENUM('ativo', 'inativo', 'bloqueado')         NOT NULL DEFAULT 'ativo',
    criado_em     DATETIME                                      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME                                      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_usuario_email (email)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------
-- candidato (extensao 1:1 de usuario com perfil = 'candidato')
-- ---------------------------------------------------------------
CREATE TABLE candidato (
    usuario_id     BIGINT UNSIGNED PRIMARY KEY,
    telefone       VARCHAR(20),
    cidade         VARCHAR(100),
    uf             CHAR(2),
    data_nascimento DATE,
    linkedin_url   VARCHAR(255),
    CONSTRAINT fk_candidato_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------
-- curriculo (1:1 com candidato)
-- ---------------------------------------------------------------
CREATE TABLE curriculo (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    candidato_id  BIGINT UNSIGNED                              NOT NULL,
    formacao      TEXT,
    experiencias  TEXT,
    competencias  TEXT,
    resumo        TEXT,
    atualizado_em DATETIME                                     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_curriculo_candidato (candidato_id),
    CONSTRAINT fk_curriculo_candidato
        FOREIGN KEY (candidato_id) REFERENCES candidato (usuario_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------
-- vaga
-- ---------------------------------------------------------------
CREATE TABLE vaga (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    rh_id         BIGINT UNSIGNED                                       NOT NULL,
    titulo        VARCHAR(150)                                          NOT NULL,
    descricao     TEXT                                                  NOT NULL,
    requisitos    TEXT,
    local         VARCHAR(150),
    modalidade    ENUM('presencial', 'remoto', 'hibrido')               NOT NULL,
    tipo_contrato ENUM('clt', 'pj', 'estagio', 'temporario')            NOT NULL,
    status        ENUM('rascunho', 'aberta', 'encerrada')               NOT NULL DEFAULT 'rascunho',
    prazo         DATE,
    criado_em     DATETIME                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME                                              NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vaga_rh
        FOREIGN KEY (rh_id) REFERENCES usuario (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_vaga_status ON vaga (status);

-- ---------------------------------------------------------------
-- candidatura
-- ---------------------------------------------------------------
CREATE TABLE candidatura (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    candidato_id     BIGINT UNSIGNED                                                            NOT NULL,
    vaga_id          BIGINT UNSIGNED                                                             NOT NULL,
    status           ENUM('inscrito', 'em_triagem', 'entrevista', 'aprovado', 'reprovado',
                          'contratado', 'cancelado')                                              NOT NULL DEFAULT 'inscrito',
    data_candidatura DATETIME                                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_candidatura_candidato_vaga (candidato_id, vaga_id),
    CONSTRAINT fk_candidatura_candidato
        FOREIGN KEY (candidato_id) REFERENCES candidato (usuario_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_candidatura_vaga
        FOREIGN KEY (vaga_id) REFERENCES vaga (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_candidatura_vaga ON candidatura (vaga_id);
CREATE INDEX idx_candidatura_candidato ON candidatura (candidato_id);

-- ---------------------------------------------------------------
-- documento
-- ---------------------------------------------------------------
CREATE TABLE documento (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    candidatura_id BIGINT UNSIGNED                        NOT NULL,
    tipo           VARCHAR(100)                            NOT NULL,
    formato        ENUM('pdf', 'docx')                     NOT NULL,
    arquivo_url    VARCHAR(500)                             NOT NULL,
    tamanho_bytes  INT UNSIGNED                             NOT NULL,
    data_envio     DATETIME                                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documento_candidatura
        FOREIGN KEY (candidatura_id) REFERENCES candidatura (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_documento_tamanho
        CHECK (tamanho_bytes <= 5242880)
) ENGINE = InnoDB;

CREATE INDEX idx_documento_candidatura ON documento (candidatura_id);

-- ---------------------------------------------------------------
-- analise_ia
-- ---------------------------------------------------------------
CREATE TABLE analise_ia (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    candidatura_id BIGINT UNSIGNED                 NOT NULL,
    pontuacao      DECIMAL(5, 2)                   NOT NULL,
    resumo         TEXT,
    data_analise   DATETIME                        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_analise_candidatura
        FOREIGN KEY (candidatura_id) REFERENCES candidatura (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_analise_pontuacao
        CHECK (pontuacao BETWEEN 0 AND 100)
) ENGINE = InnoDB;

CREATE INDEX idx_analise_candidatura ON analise_ia (candidatura_id);

-- ---------------------------------------------------------------
-- historico_status
-- ---------------------------------------------------------------
CREATE TABLE historico_status (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    candidatura_id   BIGINT UNSIGNED                                                            NOT NULL,
    status_anterior  ENUM('inscrito', 'em_triagem', 'entrevista', 'aprovado', 'reprovado',
                          'contratado', 'cancelado'),
    status_novo      ENUM('inscrito', 'em_triagem', 'entrevista', 'aprovado', 'reprovado',
                          'contratado', 'cancelado')                                              NOT NULL,
    usuario_id       BIGINT UNSIGNED                                                             NOT NULL,
    observacao       VARCHAR(500),
    data_alteracao   DATETIME                                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historico_candidatura
        FOREIGN KEY (candidatura_id) REFERENCES candidatura (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_historico_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_historico_candidatura ON historico_status (candidatura_id);
