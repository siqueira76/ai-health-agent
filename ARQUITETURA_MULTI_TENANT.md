# 🏗️ Arquitetura Multi-Tenant - AI Health Agent

## 📋 Visão Geral

O sistema implementa **Multi-Tenancy baseado em Account (Tenant ID)**, onde cada `Account` representa um tenant isolado que pode gerenciar múltiplos `Patient`.

---

## 🎯 Modelo de Dados

### Hierarquia de Entidades

```
Account (Tenant)
├── id (UUID) - Tenant ID
├── type (B2C | B2B)
├── status (ACTIVE | SUSPENDED | CANCELLED | TRIAL)
├── customPrompt (para personalização da IA)
├── limitSlots (limite de pacientes para B2B)
└── patients[] (List<Patient>)
    └── Patient
        ├── id (UUID)
        ├── account_id (FK - TENANT ID) ⚠️ CAMPO CRÍTICO
        ├── whatsappNumber (chave lógica única)
        ├── name
        ├── diagnosis
        └── ...
```

---

## 🔒 Isolamento Multi-Tenant

### Princípios de Segurança

1. **SEMPRE filtrar por `tenantId` (account_id)**
   - Toda query deve incluir o filtro `WHERE account_id = :tenantId`
   - Nunca expor dados de um tenant para outro

2. **Chave Primária Lógica**
   - `whatsappNumber` é único globalmente
   - Mas queries devem sempre incluir `tenantId` para isolamento

3. **Validação de Acesso**
   - Antes de qualquer operação, validar se o recurso pertence ao tenant

---

## 📊 Estrutura de Tabelas (PostgreSQL)

### Tabela: `accounts`

```sql
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cpf VARCHAR(11) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('B2C', 'B2B')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED', 'TRIAL')),
    custom_prompt TEXT,
    limit_slots INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_account_cpf ON accounts(cpf);
CREATE INDEX idx_account_email ON accounts(email);
CREATE INDEX idx_account_type ON accounts(type);
CREATE INDEX idx_account_status ON accounts(status);
```

### Tabela: `patients`

```sql
CREATE TABLE patients (
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
    updated_at TIMESTAMP,
    
    CONSTRAINT uk_patient_whatsapp UNIQUE (whatsapp_number)
);

CREATE INDEX idx_patient_account ON patients(account_id);
CREATE INDEX idx_patient_whatsapp ON patients(whatsapp_number);
CREATE INDEX idx_patient_active ON patients(is_active);
```

---

## 🔍 Exemplos de Uso do Repository

### ✅ CORRETO: Sempre filtrar por tenantId

```java
// Buscar paciente com isolamento
UUID tenantId = getCurrentTenantId(); // Obtém do contexto de segurança
Optional<Patient> patient = patientRepository
    .findByWhatsappNumberAndTenantId("5511999999999", tenantId);

// Listar pacientes do tenant
List<Patient> patients = patientRepository.findAllByTenantId(tenantId);

// Contar pacientes ativos
Long count = patientRepository.countActivePatientsByTenantId(tenantId);
```

### ❌ INCORRETO: Buscar sem filtro de tenant

```java
// NUNCA FAÇA ISSO EM PRODUÇÃO!
Optional<Patient> patient = patientRepository.findById(patientId);
// Risco: pode retornar paciente de outro tenant!
```

---

## 🛡️ Fluxo de Segurança

### 1. Identificação do Tenant (Ponto de Entrada)

**Fluxo Completo:**

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. WhatsApp Message Received                                   │
│    └─> Extract: whatsappNumber = "5511999999999"               │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. Identificação Inicial (SEM tenantId ainda)                  │
│    └─> findTenantContextByWhatsappNumber(whatsappNumber)       │
│        Retorna: { id, whatsappNumber, tenantId, name }         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. Estabelecer Contexto de Segurança                           │
│    └─> SecurityContext.setTenantId(projection.getTenantId())   │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. Todas as Operações Subsequentes                             │
│    └─> Usar tenantId do contexto para isolamento               │
└─────────────────────────────────────────────────────────────────┘
```

**Código de Exemplo:**

```java
// PASSO 1: Identificação inicial (ponto de entrada legítimo)
public TenantContext identifyTenant(String whatsappNumber) {
    // Projeção leve - não carrega toda a entidade
    PatientTenantProjection projection = patientRepository
        .findTenantContextByWhatsappNumber(whatsappNumber)
        .orElseThrow(() -> new NotFoundException("Paciente não cadastrado"));

    // Estabelece o contexto de segurança
    UUID tenantId = projection.getTenantId();
    SecurityContextHolder.setTenantId(tenantId);

    return new TenantContext(tenantId, projection.getName());
}

// PASSO 2: Operações subsequentes (sempre com tenantId)
public Patient getPatientDetails(String whatsappNumber) {
    UUID tenantId = SecurityContextHolder.getTenantId();

    // Agora sim, busca completa com isolamento
    return patientRepository
        .findByWhatsappNumberAndTenantId(whatsappNumber, tenantId)
        .orElseThrow(() -> new NotFoundException("Paciente não encontrado"));
}
```

### 2. Validação de Acesso em Operações Diretas

```java
public Patient getPatient(UUID patientId, UUID tenantId) {
    Patient patient = patientRepository.findById(patientId)
        .orElseThrow(() -> new NotFoundException("Paciente não encontrado"));

    // VALIDAÇÃO CRÍTICA
    if (!patient.getTenantId().equals(tenantId)) {
        throw new UnauthorizedException("Acesso negado");
    }

    return patient;
}
```

### 3. Por Que a Projeção é Importante?

**Sem Projeção (Ineficiente):**
```java
// Carrega TODA a entidade Patient + relacionamento Account (LAZY)
Optional<Patient> patient = patientRepository.findByWhatsappNumber(whatsapp);
UUID tenantId = patient.get().getAccount().getId(); // Pode causar N+1
```

**Com Projeção (Eficiente):**
```java
// Carrega apenas 4 campos essenciais em uma única query
Optional<PatientTenantProjection> projection =
    patientRepository.findTenantContextByWhatsappNumber(whatsapp);
UUID tenantId = projection.get().getTenantId(); // Direto, sem JOIN extra
```

---

## 📈 Cenários de Uso

### Cenário 1: B2C (Fibromialgia)

```
Account (B2C)
├── CPF: 12345678900
├── Type: B2C
└── Patient (self)
    └── WhatsApp: 5511999999999
```

### Cenário 2: B2B (Psicólogo)

```
Account (B2B)
├── CPF: 98765432100 (Psicólogo)
├── Type: B2B
├── limitSlots: 50
└── Patients
    ├── Patient 1 (WhatsApp: 5511111111111)
    ├── Patient 2 (WhatsApp: 5511222222222)
    └── Patient 3 (WhatsApp: 5511333333333)
```

---

## ⚙️ Métodos Principais do Repository

| Método | Descrição | Uso | Isolamento |
|--------|-----------|-----|------------|
| `findTenantContextByWhatsappNumber()` | **Projeção leve** para identificar tenant | 🔑 Ponto de entrada | ✅ Seguro |
| `findByWhatsappNumber()` | Busca completa apenas por WhatsApp | 🔑 Identificação inicial | ⚠️ Legítimo mas limitado |
| `findByWhatsappNumberAndTenantId()` | Busca paciente por WhatsApp + Tenant | ⭐ Operações normais | ✅ Seguro |
| `findAllByTenantId()` | Lista todos os pacientes do tenant | 📊 Dashboard | ✅ Seguro |
| `findActivePatientsByTenantId()` | Lista pacientes ativos | 📊 Monitoramento | ✅ Seguro |
| `countByTenantId()` | Conta pacientes do tenant | 📈 Estatísticas | ✅ Seguro |
| `searchByNameAndTenantId()` | Busca por nome | 🔍 Pesquisa | ✅ Seguro |
| `findInactiveSince()` | Pacientes sem interação | 🔔 Reengajamento | ✅ Seguro |

---

## 🚀 Próximos Passos

1. ✅ Entidades criadas (Account, Patient)
2. ✅ Repositories com isolamento multi-tenant
3. ⏳ Criar Service Layer com lógica de negócio
4. ⏳ Implementar Webhook para Evolution API
5. ⏳ Configurar Spring AI Function Calling
6. ⏳ Implementar autenticação e autorização

---

**Documentação criada em:** 2026-02-19

