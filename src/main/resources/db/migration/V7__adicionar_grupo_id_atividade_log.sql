-- ================================================================
-- V7 - Adiciona grupo_id na atividade_log, para juntar na Linha do
-- Tempo vários eventos que aconteceram na mesma ação do usuário
-- ================================================================

ALTER TABLE atividade_log
    ADD COLUMN IF NOT EXISTS grupo_id VARCHAR(36) NULL;

CREATE INDEX IF NOT EXISTS idx_atividade_log_grupo_id ON atividade_log (grupo_id);
