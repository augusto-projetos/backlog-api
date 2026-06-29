-- ================================================================
-- V2 - Cria a tabela atividade_log para a Linha do Tempo
-- ================================================================

CREATE TABLE IF NOT EXISTS atividade_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    tipo       VARCHAR(50)  NOT NULL,
    descricao  TEXT         NOT NULL,
    detalhe    VARCHAR(500),
    item_titulo VARCHAR(255),
    criado_em  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_atividade_log_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_atividade_log_user_id (user_id),
    INDEX idx_atividade_log_criado_em (criado_em DESC)
);