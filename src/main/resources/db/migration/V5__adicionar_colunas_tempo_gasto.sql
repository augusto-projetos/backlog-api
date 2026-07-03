-- ================================================================
-- V5 - Adiciona colunas de tempo gasto na tabela item
--    - renomeia coluna de desbloqueio de conquistas (user_conquistas)
-- ================================================================

ALTER TABLE item
    ADD COLUMN IF NOT EXISTS duracao_minutos INT NULL;

ALTER TABLE item
    ADD COLUMN IF NOT EXISTS horas_jogadas INT NULL;

ALTER TABLE user_conquistas
    RENAME COLUMN desbloqueda_em TO desbloqueada_em;
