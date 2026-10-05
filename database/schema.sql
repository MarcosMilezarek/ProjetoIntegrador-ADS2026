-- Schema MySQL - Sistema de Processo Seletivo
-- Mantido aqui como referencia geral do modelo de dados.
-- A fonte da verdade para provisionar o banco agora e o Flyway, em
-- backend/src/main/resources/db/migration (roda automaticamente no startup da aplicacao).
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
-- curriculo (1:1 com usuario de perfil 'candidato')
-- Concentra dados pessoais, contato e conteudo do candidato.
-- ---------------------------------------------------------------
CREATE TABLE curriculo (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT UNSIGNED                                        NOT NULL,
    data_nascimento DATE,
    sexo            ENUM('feminino', 'masculino', 'outro', 'nao_informado'),
    cidade          VARCHAR(100),
    uf              CHAR(2),
    numero_contato  VARCHAR(20),
    perfil_linkedin VARCHAR(255),
    competencias    TEXT,
    certificacoes   TEXT,
    resumo          TEXT,
    atualizado_em   DATETIME                                               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_curriculo_usuario (usuario_id),
    CONSTRAINT fk_curriculo_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------
-- curriculo_formacao (1:N)
-- ---------------------------------------------------------------
CREATE TABLE curriculo_formacao (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    curriculo_id BIGINT UNSIGNED NOT NULL,
    curso        VARCHAR(150)    NOT NULL,
    instituicao  VARCHAR(150)    NOT NULL,
    data_inicio  DATE            NOT NULL,
    -- nula quando o curso esta em andamento
    data_termino DATE,
    CONSTRAINT fk_formacao_curriculo
        FOREIGN KEY (curriculo_id) REFERENCES curriculo (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_formacao_curriculo ON curriculo_formacao (curriculo_id);

-- ---------------------------------------------------------------
-- curriculo_experiencia (1:N)
-- ---------------------------------------------------------------
CREATE TABLE curriculo_experiencia (
    id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    curriculo_id         BIGINT UNSIGNED NOT NULL,
    cargo                VARCHAR(150)    NOT NULL,
    empresa              VARCHAR(150)    NOT NULL,
    data_contratacao     DATE            NOT NULL,
    data_demissao        DATE,
    trabalho_atual       BOOLEAN         NOT NULL DEFAULT FALSE,
    descricao_atividades TEXT,
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
-- curriculo_arquivo (1:1) - PDF do curriculo; o binario fica em disco
-- ---------------------------------------------------------------
CREATE TABLE curriculo_arquivo (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    curriculo_id    BIGINT UNSIGNED NOT NULL,
    nome_original   VARCHAR(255)    NOT NULL,
    nome_armazenado VARCHAR(255)    NOT NULL,
    content_type    VARCHAR(100)    NOT NULL,
    tamanho_bytes   INT UNSIGNED    NOT NULL,
    enviado_em      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_arquivo_curriculo (curriculo_id),
    CONSTRAINT fk_arquivo_curriculo
        FOREIGN KEY (curriculo_id) REFERENCES curriculo (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_arquivo_tamanho
        CHECK (tamanho_bytes <= 5242880)
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
    requisitos_obrigatorios TEXT,
    requisitos_desejaveis   TEXT,
    requisitos_diferenciais TEXT,
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
    usuario_id       BIGINT UNSIGNED                                                            NOT NULL,
    vaga_id          BIGINT UNSIGNED                                                             NOT NULL,
    status           ENUM('inscrito', 'em_triagem', 'entrevista', 'aprovado', 'reprovado',
                          'contratado', 'cancelado')                                              NOT NULL DEFAULT 'inscrito',
    entrevista_em    DATETIME                                                                    NULL,  -- em UTC
    presenca         ENUM('pendente', 'confirmado')                                              NULL,  -- nula sem entrevista
    presenca_confirmada_em DATETIME                                                              NULL,  -- em UTC
    data_candidatura DATETIME                                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_candidatura_usuario_vaga (usuario_id, vaga_id),
    CONSTRAINT fk_candidatura_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_candidatura_vaga
        FOREIGN KEY (vaga_id) REFERENCES vaga (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_candidatura_vaga ON candidatura (vaga_id);
CREATE INDEX idx_candidatura_usuario ON candidatura (usuario_id);

-- ---------------------------------------------------------------
-- documento
-- ---------------------------------------------------------------
CREATE TABLE documento (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    candidatura_id BIGINT UNSIGNED                        NOT NULL,
    -- lista fechada (Documento.Tipo); nulo so em envio antigo de texto livre sem correspondencia
    tipo           ENUM('rg', 'cpf', 'ctps', 'titulo_eleitor', 'comprovante_residencia',
                        'comprovante_escolaridade', 'foto_3x4', 'pis_pasep',
                        'certidao_nascimento_casamento', 'dados_bancarios',
                        'certificado_reservista')               NULL,
    tipo_informado VARCHAR(100)                            NULL,  -- texto livre de antes da V5
    formato        ENUM('pdf', 'docx')                     NOT NULL,
    status         ENUM('pendente', 'aprovado', 'recusado') NOT NULL DEFAULT 'pendente',  -- revisao do RH
    arquivo_url    VARCHAR(500)                             NOT NULL,
    tamanho_bytes  INT UNSIGNED                             NOT NULL,
    data_envio     DATETIME                                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_documento_candidatura_tipo (candidatura_id, tipo),
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

-- ---------------------------------------------------------------
-- notificacao (entregue em tempo real por SSE e guardada para depois)
-- ---------------------------------------------------------------
CREATE TABLE notificacao (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT UNSIGNED  NOT NULL,
    tipo       ENUM('candidatura', 'nova_vaga') NOT NULL DEFAULT 'candidatura',
    referencia_id BIGINT UNSIGNED NULL,  -- id da candidatura ou da vaga, conforme o tipo
    titulo     VARCHAR(150)     NOT NULL,
    mensagem   VARCHAR(500)     NOT NULL,
    lida       BOOLEAN          NOT NULL DEFAULT FALSE,
    criado_em  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notificacao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_notificacao_usuario ON notificacao (usuario_id, lida);

-- ---------------------------------------------------------------
-- funcionario (candidatura contratada)
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

-- ---------------------------------------------------------------
-- analise_candidatura (triagem por IA: aderencia, pontos positivos e negativos; V7 e V8)
-- ---------------------------------------------------------------
CREATE TABLE analise_candidatura (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    candidatura_id   BIGINT UNSIGNED                              NOT NULL,
    status           ENUM('PENDENTE', 'CONCLUIDA', 'FALHA')       NOT NULL DEFAULT 'PENDENTE',
    aderencia        TINYINT UNSIGNED                             NULL,  -- porcentagem 0 a 100; so com CONCLUIDA
    pontos_positivos TEXT                                         NULL,  -- lista JSON de textos
    pontos_negativos TEXT                                         NULL,  -- lista JSON de textos
    modelo           VARCHAR(150)                                 NULL,
    data_analise     DATETIME                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_analise_candidatura (candidatura_id),
    CONSTRAINT fk_analise_candidatura_candidatura
        FOREIGN KEY (candidatura_id) REFERENCES candidatura (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_analise_aderencia CHECK (aderencia IS NULL OR aderencia BETWEEN 0 AND 100)
) ENGINE = InnoDB;
