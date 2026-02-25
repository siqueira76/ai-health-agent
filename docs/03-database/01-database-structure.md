# 3.1 Estrutura do Banco de Dados

## 🗄️ Visão Geral

O AI Health Agent utiliza **PostgreSQL 16** com uma estrutura relacional otimizada para multi-tenancy e performance.

---

## 📊 Diagrama ER (Entity-Relationship)

```
┌─────────────────┐
│    accounts     │
│  (Tenants)      │
├─────────────────┤
│ id (PK)         │
│ name            │
│ account_type    │◄──────┐
│ status          │       │
│ custom_prompt   │       │
│ created_at      │       │
└─────────────────┘       │
         │                │
         │ 1:N            │
         ▼                │
┌─────────────────┐       │
│    patients     │       │
├─────────────────┤       │
│ id (PK)         │       │
│ account_id (FK) │───────┘
│ name            │
│ whatsapp_number │
│ is_active       │
│ created_at      │
└─────────────────┘
         │
         │ 1:N
         ├──────────────────┬──────────────────┬──────────────────┐
         ▼                  ▼                  ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  health_logs    │ │ chat_messages   │ │     alerts      │ │checkin_schedules│
├─────────────────┤ ├─────────────────┤ ├─────────────────┤ ├─────────────────┤
│ id (PK)         │ │ id (PK)         │ │ id (PK)         │ │ id (PK)         │
│ account_id (FK) │ │ account_id (FK) │ │ account_id (FK) │ │ account_id (FK) │
│ patient_id (FK) │ │ patient_id (FK) │ │ patient_id (FK) │ │ patient_id (FK) │
│ pain_level      │ │ message_text    │ │ alert_type      │ │ frequency       │
│ mood            │ │ sender          │ │ severity        │ │ time_of_day     │
│ sleep_quality   │ │ timestamp       │ │ message         │ │ is_active       │
│ medications     │ │ context_window  │ │ created_at      │ │ next_execution  │
│ energy_level    │ └─────────────────┘ └─────────────────┘ └─────────────────┘
│ stress_level    │                                                  │
│ notes           │                                                  │ 1:N
│ created_at      │                                                  ▼
└─────────────────┘                                         ┌─────────────────┐
                                                            │checkin_executions│
                                                            ├─────────────────┤
                                                            │ id (PK)         │
                                                            │ schedule_id (FK)│
                                                            │ account_id (FK) │
                                                            │ patient_id (FK) │
                                                            │ status          │
                                                            │ executed_at     │
                                                            │ error_message   │
                                                            └─────────────────┘
```

---

## 📋 Tabelas Principais

### **1. accounts (Tenants)**

Armazena informações dos tenants (B2C ou B2B).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | UUID | Chave primária |
| `name` | VARCHAR(255) | Nome do tenant |
| `account_type` | VARCHAR(10) | 'B2C' ou 'B2B' |
| `status` | VARCHAR(20) | 'TRIAL', 'ACTIVE', 'SUSPENDED', 'CANCELLED' |
| `custom_prompt` | TEXT | Prompt personalizado (B2B) |
| `trial_ends_at` | TIMESTAMP | Data de fim do trial |
| `created_at` | TIMESTAMP | Data de criação |
| `updated_at` | TIMESTAMP | Data de atualização |

**Índices:**
```sql
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_type ON accounts(account_type);
```

---

### **2. patients**

Armazena informações dos pacientes.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | UUID | Chave primária |
| `account_id` | UUID | FK para accounts |
| `name` | VARCHAR(255) | Nome do paciente |
| `whatsapp_number` | VARCHAR(20) | Número do WhatsApp (único) |
| `is_active` | BOOLEAN | Paciente ativo? |
| `created_at` | TIMESTAMP | Data de criação |
| `updated_at` | TIMESTAMP | Data de atualização |

**Índices:**
```sql
CREATE UNIQUE INDEX idx_patients_whatsapp ON patients(whatsapp_number);
CREATE INDEX idx_patients_account ON patients(account_id);
CREATE INDEX idx_patients_active ON patients(is_active) WHERE is_active = true;
```

**Constraints:**
```sql
ALTER TABLE patients 
  ADD CONSTRAINT fk_patients_account 
  FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE;
```

---

### **3. health_logs**

Armazena dados de saúde extraídos das conversas.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | UUID | Chave primária |
| `account_id` | UUID | FK para accounts (multi-tenancy) |
| `patient_id` | UUID | FK para patients |
| `pain_level` | INTEGER | Nível de dor (0-10) |
| `mood` | VARCHAR(50) | Humor (feliz, triste, ansioso, etc) |
| `sleep_quality` | VARCHAR(50) | Qualidade do sono |
| `medications_taken` | TEXT | Medicações tomadas (JSON array) |
| `energy_level` | VARCHAR(50) | Nível de energia |
| `stress_level` | VARCHAR(50) | Nível de estresse |
| `notes` | TEXT | Observações adicionais |
| `created_at` | TIMESTAMP | Data de criação |

**Índices:**
```sql
CREATE INDEX idx_health_logs_patient ON health_logs(patient_id, created_at DESC);
CREATE INDEX idx_health_logs_account ON health_logs(account_id);
CREATE INDEX idx_health_logs_pain ON health_logs(pain_level) WHERE pain_level >= 8;
CREATE INDEX idx_health_logs_date ON health_logs(created_at);
```

---

### **4. chat_messages**

Armazena histórico de mensagens (contexto para IA).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | UUID | Chave primária |
| `account_id` | UUID | FK para accounts |
| `patient_id` | UUID | FK para patients |
| `message_text` | TEXT | Conteúdo da mensagem |
| `sender` | VARCHAR(20) | 'PATIENT' ou 'AI' |
| `timestamp` | TIMESTAMP | Data/hora da mensagem |
| `context_window` | INTEGER | Janela de contexto (1-10) |

**Índices:**
```sql
CREATE INDEX idx_chat_messages_patient ON chat_messages(patient_id, timestamp DESC);
CREATE INDEX idx_chat_messages_context ON chat_messages(patient_id, context_window);
```

---

### **5. alerts**

Armazena alertas gerados automaticamente.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | UUID | Chave primária |
| `account_id` | UUID | FK para accounts |
| `patient_id` | UUID | FK para patients |
| `alert_type` | VARCHAR(50) | 'CRISIS', 'TREND', 'MEDICATION', etc |
| `severity` | VARCHAR(20) | 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL' |
| `message` | TEXT | Mensagem do alerta |
| `is_read` | BOOLEAN | Alerta foi lido? |
| `created_at` | TIMESTAMP | Data de criação |

**Índices:**
```sql
CREATE INDEX idx_alerts_patient ON alerts(patient_id, created_at DESC);
CREATE INDEX idx_alerts_unread ON alerts(account_id, is_read) WHERE is_read = false;
CREATE INDEX idx_alerts_severity ON alerts(severity, created_at DESC);
```

---

### **6. checkin_schedules**

Armazena agendamentos de check-ins proativos.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | UUID | Chave primária |
| `account_id` | UUID | FK para accounts |
| `patient_id` | UUID | FK para patients |
| `frequency` | VARCHAR(20) | 'DAILY', 'WEEKLY', 'CUSTOM' |
| `time_of_day` | TIME | Horário do check-in |
| `days_of_week` | VARCHAR(50) | Dias da semana (JSON array) |
| `is_active` | BOOLEAN | Agendamento ativo? |
| `next_execution_at` | TIMESTAMP | Próxima execução |
| `last_reset_date` | DATE | Última data de reset do contador |
| `messages_sent_today` | INTEGER | Mensagens enviadas hoje |
| `created_at` | TIMESTAMP | Data de criação |

**Índices:**
```sql
CREATE INDEX idx_checkin_schedules_next_exec ON checkin_schedules(next_execution_at) 
  WHERE is_active = true;
CREATE INDEX idx_checkin_schedules_patient ON checkin_schedules(patient_id);
```

---

### **7. checkin_executions**

Armazena histórico de execuções de check-ins.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | UUID | Chave primária |
| `schedule_id` | UUID | FK para checkin_schedules |
| `account_id` | UUID | FK para accounts |
| `patient_id` | UUID | FK para patients |
| `status` | VARCHAR(20) | 'SUCCESS', 'FAILED', 'SKIPPED' |
| `executed_at` | TIMESTAMP | Data/hora da execução |
| `error_message` | TEXT | Mensagem de erro (se falhou) |

**Índices:**
```sql
CREATE INDEX idx_checkin_executions_schedule ON checkin_executions(schedule_id, executed_at DESC);
CREATE INDEX idx_checkin_executions_status ON checkin_executions(status, executed_at DESC);
```

---

### **8. shedlock**

Tabela de controle para jobs distribuídos (ShedLock).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `name` | VARCHAR(64) | Nome do lock (PK) |
| `lock_until` | TIMESTAMP | Até quando o lock é válido |
| `locked_at` | TIMESTAMP | Quando foi adquirido |
| `locked_by` | VARCHAR(255) | Qual instância adquiriu |

---

## 🎯 Próximos Passos

1. 📊 [Modelo de Dados Detalhado](02-data-model.md)
2. 🔄 [Migrations](03-migrations.md)
3. 🔗 [Relacionamentos](04-relationships.md)

---

[⬅️ Anterior: Configuração](../02-getting-started/03-configuration.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Modelo de Dados](02-data-model.md)

