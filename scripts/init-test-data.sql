-- ============================================
-- SCRIPT DE INICIALIZAÇÃO DE DADOS DE TESTE
-- ============================================
-- Este script é executado automaticamente quando o PostgreSQL sobe pela primeira vez
-- Cria dados de teste para facilitar o desenvolvimento

-- NOTA: As migrations do Flyway criarão as tabelas automaticamente
-- Este script apenas insere dados de teste APÓS as migrations

-- Aguardar migrations (este script roda antes do Flyway)
-- Por isso, comentamos a inserção de dados aqui
-- Os dados de teste devem ser inseridos via API ou script separado

-- ============================================
-- EXTENSÕES NECESSÁRIAS
-- ============================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- CONFIGURAÇÕES DE TIMEZONE
-- ============================================
SET timezone = 'America/Sao_Paulo';

-- ============================================
-- LOG
-- ============================================
DO $$
BEGIN
    RAISE NOTICE 'Database ai_health_agent initialized successfully!';
    RAISE NOTICE 'Timezone: %', current_setting('TIMEZONE');
    RAISE NOTICE 'Flyway migrations will run next...';
END $$;

