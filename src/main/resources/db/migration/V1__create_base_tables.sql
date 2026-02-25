-- ============================================
-- MIGRATION V1: Tabelas Base do Sistema
-- ============================================

-- ============================================
-- TABELA: accounts (Tenants)
-- ============================================
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cpf VARCHAR(11) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('B2C', 'B2B')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('TRIAL', 'ACTIVE', 'SUSPENDED', 'CANCELLED')),
    custom_prompt TEXT,
    limit_slots INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_account_cpf ON accounts(cpf);
CREATE INDEX idx_account_email ON accounts(email);
CREATE INDEX idx_account_type ON accounts(type);
CREATE INDEX idx_account_status ON accounts(status);

-- ============================================
-- TABELA: patients
-- ============================================
CREATE TABLE IF NOT EXISTS patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    whatsapp_number VARCHAR(15) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    birth_date DATE,
    diagnosis VARCHAR(255),
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_interaction_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_patient_whatsapp UNIQUE (whatsapp_number)
);

CREATE INDEX idx_patient_account ON patients(account_id);
CREATE INDEX idx_patient_whatsapp ON patients(whatsapp_number);
CREATE INDEX idx_patient_active ON patients(is_active);

-- ============================================
-- TABELA: health_logs
-- ============================================
CREATE TABLE IF NOT EXISTS health_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pain_level INTEGER CHECK (pain_level BETWEEN 0 AND 10),
    mood VARCHAR(50),
    sleep_quality VARCHAR(50),
    sleep_hours DOUBLE PRECISION,
    medication_taken BOOLEAN,
    medication_name VARCHAR(255),
    energy_level INTEGER CHECK (energy_level BETWEEN 0 AND 10),
    stress_level INTEGER CHECK (stress_level BETWEEN 0 AND 10),
    notes TEXT,
    raw_ai_extraction TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_health_logs_patient ON health_logs(patient_id);
CREATE INDEX idx_health_logs_account ON health_logs(account_id);
CREATE INDEX idx_health_logs_timestamp ON health_logs(timestamp);

-- ============================================
-- TABELA: chat_messages
-- ============================================
CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    whatsapp_message_id VARCHAR(255),
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_whatsapp_message UNIQUE (whatsapp_message_id)
);

CREATE INDEX idx_chat_patient_timestamp ON chat_messages(patient_id, timestamp DESC);
CREATE INDEX idx_chat_account_timestamp ON chat_messages(account_id, timestamp DESC);

-- ============================================
-- TABELA: alerts
-- ============================================
CREATE TABLE IF NOT EXISTS alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT false,
    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alerts_patient_created ON alerts(patient_id, created_at DESC);
CREATE INDEX idx_alerts_account_severity ON alerts(account_id, severity, acknowledged);
CREATE INDEX idx_alerts_type ON alerts(alert_type, acknowledged);

-- ============================================
-- TABELA: shedlock (Lock Distribuído)
-- ============================================
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

