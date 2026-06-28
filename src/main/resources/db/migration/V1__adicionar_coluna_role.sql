-- ================================================================
-- V1 - Garante que a coluna 'role' existe na tabela users
-- ================================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(10) NOT NULL DEFAULT 'USER';
