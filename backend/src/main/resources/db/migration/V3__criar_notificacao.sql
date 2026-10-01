-- Notificacoes do portal (RH e candidato), entregues em tempo real por SSE e guardadas
-- aqui para aparecerem depois de recarregar a pagina ou num login futuro.
-- Excluir o usuario remove as notificacoes dele.

CREATE TABLE notificacao (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT UNSIGNED  NOT NULL,
    titulo     VARCHAR(150)     NOT NULL,
    mensagem   VARCHAR(500)     NOT NULL,
    lida       BOOLEAN          NOT NULL DEFAULT FALSE,
    criado_em  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notificacao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_notificacao_usuario ON notificacao (usuario_id, lida);
