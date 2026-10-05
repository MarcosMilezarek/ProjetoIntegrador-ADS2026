-- Triagem de curriculo por IA: uma analise por candidatura, com pontos positivos e negativos.
-- Nenhum registro e apagado: so uma tabela nova. A analise_ia (reservada ao RF15, com pontuacao
-- obrigatoria) fica como esta; as candidaturas anteriores a esta migration simplesmente nao tem analise.
-- pontos_positivos e pontos_negativos guardam uma lista JSON de textos.

CREATE TABLE analise_candidatura (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    candidatura_id   BIGINT UNSIGNED                              NOT NULL,
    status           ENUM('PENDENTE', 'CONCLUIDA', 'FALHA')       NOT NULL DEFAULT 'PENDENTE',
    pontos_positivos TEXT                                         NULL,
    pontos_negativos TEXT                                         NULL,
    modelo           VARCHAR(150)                                 NULL,
    data_analise     DATETIME                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_analise_candidatura (candidatura_id),
    CONSTRAINT fk_analise_candidatura_candidatura
        FOREIGN KEY (candidatura_id) REFERENCES candidatura (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;
