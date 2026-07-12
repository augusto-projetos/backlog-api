-- ================================================================
-- V6 - Adiciona colunas de progresso e tempo assistido para Séries
-- ================================================================

ALTER TABLE item
    ADD COLUMN IF NOT EXISTS temporada_atual INT NULL;

ALTER TABLE item
    ADD COLUMN IF NOT EXISTS episodio_atual INT NULL;

ALTER TABLE item
    ADD COLUMN IF NOT EXISTS duracao_total_minutos INT NULL;
