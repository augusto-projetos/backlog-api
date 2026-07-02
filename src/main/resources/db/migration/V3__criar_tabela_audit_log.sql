-- ================================================================
-- V3 - Cria a tabela audit_log para o Log de Auditoria do Admin
-- ================================================================

CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL       NOT NULL,
    acao        VARCHAR(60)     NOT NULL,
    descricao   TEXT            NOT NULL,
    detalhe     VARCHAR(1000),
    alvo_tipo   VARCHAR(50),
    alvo_id     BIGINT,
    alvo_nome   VARCHAR(255),
    ip          VARCHAR(50),
    criado_em   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_audit_log_acao      ON audit_log (acao);
CREATE INDEX IF NOT EXISTS idx_audit_log_criado_em ON audit_log (criado_em DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_alvo_tipo ON audit_log (alvo_tipo);
CREATE INDEX IF NOT EXISTS idx_audit_log_alvo_id   ON audit_log (alvo_id);
