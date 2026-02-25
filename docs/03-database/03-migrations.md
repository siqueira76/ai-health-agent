# 3.3 Migrations (Flyway)

## 🔄 Versionamento de Banco de Dados

O projeto usa **Flyway** para gerenciar mudanças no schema do banco de forma versionada e rastreável.

---

## 📁 Estrutura de Migrations

```
src/main/resources/db/migration/
├── V1__create_base_tables.sql
├── V2__add_indexes.sql
├── V3__add_constraints.sql
├── V4__seed_initial_data.sql
└── V5__create_checkin_tables.sql
```

---

## 📝 Convenção de Nomenclatura

```
V{versão}__{descrição}.sql

Exemplos:
V1__create_base_tables.sql
V2__add_user_email_column.sql
V3__create_index_on_patients.sql
```

**Regras:**
- ✅ Prefixo `V` (versioned)
- ✅ Número sequencial (1, 2, 3...)
- ✅ Dois underscores `__` antes da descrição
- ✅ Descrição em snake_case
- ✅ Extensão `.sql`

---

## 📄 V1__create_base_tables.sql

```sql
-- ============================================
-- ACCOUNTS (Tenants)
-- ============================================
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(10) NOT NULL CHECK (account_type IN ('B2C', 'B2B')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('TRIAL', 'ACTIVE', 'SUSPENDED', 'CANCELLED')),
    custom_prompt TEXT,
    trial_ends_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- PATIENTS
-- ============================================
CREATE TABLE patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    whatsapp_number VARCHAR(20) UNIQUE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- HEALTH LOGS
-- ============================================
CREATE TABLE health_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    pain_level INTEGER CHECK (pain_level BETWEEN 0 AND 10),
    mood VARCHAR(50),
    sleep_quality VARCHAR(50),
    medications_taken TEXT,
    energy_level VARCHAR(50),
    stress_level VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- CHAT MESSAGES
-- ============================================
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    message_text TEXT NOT NULL,
    sender VARCHAR(20) NOT NULL CHECK (sender IN ('PATIENT', 'AI')),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    context_window INTEGER
);

-- ============================================
-- ALERTS
-- ============================================
CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- SHEDLOCK (Distributed Locks)
-- ============================================
CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
```

---

## 📄 V5__create_checkin_tables.sql

```sql
-- ============================================
-- CHECKIN SCHEDULES
-- ============================================
CREATE TABLE checkin_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    frequency VARCHAR(20) NOT NULL,
    time_of_day TIME NOT NULL,
    days_of_week VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true,
    next_execution_at TIMESTAMP,
    last_reset_date DATE,
    messages_sent_today INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- CHECKIN EXECUTIONS
-- ============================================
CREATE TABLE checkin_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID NOT NULL REFERENCES checkin_schedules(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX idx_checkin_schedules_next_exec 
    ON checkin_schedules(next_execution_at) 
    WHERE is_active = true;

CREATE INDEX idx_checkin_executions_schedule 
    ON checkin_executions(schedule_id, executed_at DESC);
```

---

## 🚀 Executando Migrations

### **Automático (ao iniciar aplicação)**

```properties
# application.properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

Flyway executa automaticamente ao subir a aplicação.

### **Manual (via Maven)**

```bash
# Executar migrations
./mvnw flyway:migrate

# Ver status
./mvnw flyway:info

# Limpar banco (CUIDADO!)
./mvnw flyway:clean
```

---

## 📊 Verificando Migrations

### **Tabela de Controle**

Flyway cria automaticamente a tabela `flyway_schema_history`:

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

**Colunas:**
- `installed_rank` - Ordem de execução
- `version` - Versão da migration
- `description` - Descrição
- `script` - Nome do arquivo
- `success` - Executou com sucesso?
- `installed_on` - Data/hora de execução

---

## 🔄 Adicionando Nova Migration

### **1. Criar arquivo**

```bash
touch src/main/resources/db/migration/V6__add_patient_email.sql
```

### **2. Escrever SQL**

```sql
-- V6__add_patient_email.sql
ALTER TABLE patients ADD COLUMN email VARCHAR(255);
CREATE INDEX idx_patients_email ON patients(email);
```

### **3. Executar**

```bash
# Reiniciar aplicação ou
./mvnw flyway:migrate
```

---

## ⚠️ Boas Práticas

### ✅ **O QUE FAZER:**

1. **Nunca modifique migrations já executadas** em produção
2. **Sempre teste migrations** em ambiente de desenvolvimento primeiro
3. **Use transações** quando possível
4. **Faça backup** antes de migrations críticas
5. **Documente mudanças** complexas

### ❌ **O QUE NÃO FAZER:**

1. ❌ Nunca delete migrations já executadas
2. ❌ Nunca altere o número de versão
3. ❌ Nunca execute `flyway:clean` em produção
4. ❌ Nunca commite migrations com erros

---

## 🎯 Próximos Passos

1. 🔗 [Relacionamentos](04-relationships.md)
2. 🏗️ [Arquitetura em Camadas](../04-architecture/01-layered-architecture.md)
3. 🤖 [Spring AI Setup](../05-ai/01-spring-ai-setup.md)

---

[⬅️ Anterior: Modelo de Dados](02-data-model.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Relacionamentos](04-relationships.md)

