-- ============================================
-- TABELA: checkin_schedules
-- Armazena agendamentos de check-ins proativos
-- ============================================
CREATE TABLE checkin_schedules (
    -- Identificação
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Multi-tenancy
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    
    -- Configuração do Agendamento
    schedule_type VARCHAR(50) NOT NULL, -- 'DAILY', 'WEEKLY', 'CUSTOM'
    time_of_day TIME NOT NULL,          -- Ex: '09:00:00', '20:00:00'
    days_of_week INTEGER[],             -- [1,2,3,4,5] = Seg-Sex, NULL = todos os dias
    timezone VARCHAR(50) DEFAULT 'America/Sao_Paulo',
    
    -- Personalização da Mensagem
    custom_message TEXT,                -- Mensagem customizada (opcional)
    use_ai_generation BOOLEAN DEFAULT true, -- Se true, usa LLM para gerar mensagem
    
    -- Controle de Execução
    is_active BOOLEAN DEFAULT true,
    last_execution_at TIMESTAMP,
    next_execution_at TIMESTAMP,        -- Calculado automaticamente
    
    -- Rate Limiting
    max_messages_per_day INTEGER DEFAULT 3,
    messages_sent_today INTEGER DEFAULT 0,
    last_reset_date DATE DEFAULT CURRENT_DATE,
    
    -- Auditoria
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),            -- ID do psicólogo (B2B) ou 'system' (B2C)
    
    -- Constraints
    CONSTRAINT unique_patient_schedule UNIQUE (patient_id, schedule_type, time_of_day)
);

-- Índices para performance
CREATE INDEX idx_checkin_schedules_account ON checkin_schedules(account_id);
CREATE INDEX idx_checkin_schedules_patient ON checkin_schedules(patient_id);
CREATE INDEX idx_checkin_schedules_next_execution ON checkin_schedules(next_execution_at) 
    WHERE is_active = true;
CREATE INDEX idx_checkin_schedules_active ON checkin_schedules(is_active, next_execution_at);

-- ============================================
-- TABELA: checkin_executions
-- Histórico de execuções de check-ins
-- ============================================
CREATE TABLE checkin_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Relacionamentos
    schedule_id UUID NOT NULL REFERENCES checkin_schedules(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    
    -- Detalhes da Execução
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,        -- 'SUCCESS', 'FAILED', 'SKIPPED'
    failure_reason TEXT,
    
    -- Mensagem Enviada
    message_sent TEXT,
    message_id VARCHAR(255),            -- ID da Evolution API
    
    -- Resposta do Paciente
    patient_responded BOOLEAN DEFAULT false,
    response_received_at TIMESTAMP,
    
    -- Auditoria
    execution_duration_ms INTEGER
);

-- Índices
CREATE INDEX idx_checkin_executions_schedule ON checkin_executions(schedule_id);
CREATE INDEX idx_checkin_executions_account ON checkin_executions(account_id);
CREATE INDEX idx_checkin_executions_patient ON checkin_executions(patient_id);
CREATE INDEX idx_checkin_executions_date ON checkin_executions(executed_at);
CREATE INDEX idx_checkin_executions_status ON checkin_executions(status);

-- ============================================
-- TABELA: shedlock
-- Lock distribuído para múltiplas instâncias
-- ============================================
CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

-- ============================================
-- ALTERAÇÕES NA TABELA accounts (Opcional)
-- Configurações padrão de check-in por tenant
-- ============================================
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS default_checkin_time TIME DEFAULT '09:00:00';
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS default_checkin_enabled BOOLEAN DEFAULT false;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS max_checkins_per_day INTEGER DEFAULT 3;

-- ============================================
-- COMENTÁRIOS
-- ============================================
COMMENT ON TABLE checkin_schedules IS 'Agendamentos de check-ins proativos por paciente';
COMMENT ON TABLE checkin_executions IS 'Histórico de execuções de check-ins';
COMMENT ON TABLE shedlock IS 'Lock distribuído para jobs agendados (ShedLock)';

COMMENT ON COLUMN checkin_schedules.schedule_type IS 'Tipo de agendamento: DAILY, WEEKLY, CUSTOM';
COMMENT ON COLUMN checkin_schedules.days_of_week IS 'Array de dias da semana (1=Seg, 7=Dom)';
COMMENT ON COLUMN checkin_schedules.use_ai_generation IS 'Se true, usa IA para gerar mensagem; se false, usa custom_message';
COMMENT ON COLUMN checkin_schedules.next_execution_at IS 'Próxima execução calculada automaticamente';
COMMENT ON COLUMN checkin_schedules.max_messages_per_day IS 'Limite de mensagens por dia (rate limiting)';

COMMENT ON COLUMN checkin_executions.status IS 'Status da execução: SUCCESS, FAILED, SKIPPED';
COMMENT ON COLUMN checkin_executions.message_id IS 'ID da mensagem retornado pela Evolution API';
COMMENT ON COLUMN checkin_executions.patient_responded IS 'Se o paciente respondeu à mensagem proativa';

