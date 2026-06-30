-- ================================================================
-- V4 - Cria a tabela system_config para gerenciamento do sistema
-- ================================================================

CREATE TABLE IF NOT EXISTS system_config (
    chave       VARCHAR(100)    NOT NULL,
    valor       VARCHAR(500)    NOT NULL DEFAULT 'false',
    descricao   VARCHAR(500),
    atualizado_em TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chave)
);

-- Alavancas padrão (todas desativadas)
INSERT INTO system_config (chave, valor, descricao) VALUES
    ('MODO_MANUTENCAO',     'false', 'Exibe aviso de manutenção em andamento antes da home'),
    ('MODO_INSTAVEL',       'false', 'Exibe aviso de instabilidade antes da home'),
    ('SISTEMA_BLOQUEADO',   'false', 'Bloqueia todo o sistema exceto o index'),
    ('MODO_READONLY',       'false', 'Desativa criação e edição de itens para todos os usuários'),
    ('NOVIDADES',           '',      'Mensagem de novidades/comunicado exibida na home (vazio = oculta)')
ON CONFLICT (chave) DO NOTHING;