-- ================================================================
-- V8 - Adiciona client_request_id (chave de idempotência) na tabela item
--
-- O front-end gera uma chave por abertura do formulário de cadastro e a
-- envia no POST /itens. Se a mesma requisição chegar duas vezes (duplo
-- clique, retry de rede/proxy, aba duplicada), o índice único abaixo
-- impede que o item seja gravado duas vezes.
--
-- Itens antigos ficam com NULL, e o índice único aceita vários NULLs.
-- ================================================================

ALTER TABLE item
    ADD COLUMN IF NOT EXISTS client_request_id VARCHAR(64) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_item_user_client_request_id
    ON item (user_id, client_request_id);
