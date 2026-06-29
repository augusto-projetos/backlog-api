-- ================================================================
-- V2 - Cria a tabela atividade_log para a Linha do Tempo
-- ================================================================

CREATE TABLE IF NOT EXISTS atividade_log (
    id          BIGSERIAL       NOT NULL,
    user_id     BIGINT          NOT NULL,
    tipo        VARCHAR(50)     NOT NULL,
    descricao   TEXT            NOT NULL,
    detalhe     VARCHAR(500),
    item_titulo VARCHAR(255),
    criado_em   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_atividade_log_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_atividade_log_user_id   ON atividade_log (user_id);
CREATE INDEX IF NOT EXISTS idx_atividade_log_criado_em ON atividade_log (criado_em DESC);