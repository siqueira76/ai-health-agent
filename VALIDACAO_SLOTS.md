# 🎯 Validação de Limit Slots - AI Health Agent

## 📋 Visão Geral

O sistema implementa **validação automática de limite de pacientes (slots)** para contas B2B, permitindo controle de planos e monetização.

---

## 🔧 Como Funciona

### Regras de Negócio

| Tipo de Conta | Limite de Slots | Validação |
|---------------|-----------------|-----------|
| **B2C** | Sem limite | ❌ Não valida |
| **B2B** com `limitSlots = null` | Sem limite | ❌ Não valida |
| **B2B** com `limitSlots = 0` | Sem limite | ❌ Não valida |
| **B2B** com `limitSlots > 0` | Limitado | ✅ Valida |

### Fluxo de Validação

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Tentativa de Criar/Reativar Paciente                        │
│    └─> PatientService.createPatient(patient, tenantId)         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. Buscar Account (Tenant)                                      │
│    └─> Account.findById(tenantId)                               │
│    └─> Obter: type, limitSlots                                  │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. Validar Limite de Slots                                      │
│    ├─> Se B2C → PULA validação                                  │
│    ├─> Se limitSlots = null/0 → PULA validação                  │
│    └─> Se B2B com limite → VALIDA                               │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. Contar Pacientes Ativos                                      │
│    └─> countActivePatientsByTenantId(tenantId)                  │
│    └─> Exemplo: 4 pacientes ativos                              │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. Comparar com Limite                                          │
│    ├─> Se activePatients >= limitSlots                          │
│    │   └─> LANÇA SlotLimitExceededException                     │
│    └─> Senão                                                    │
│        └─> PERMITE criação                                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 💻 Implementação

### PatientService - Validação Principal

<augment_code_snippet path="src/main/java/com/healthlink/ai_health_agent/service/PatientService.java" mode="EXCERPT">
````java
private void validateSlotLimit(Account account) {
    // Contas B2C não têm limite
    if (account.isB2C()) {
        return;
    }

    // Se limitSlots é null ou 0, não há limite
    if (account.getLimitSlots() == null || account.getLimitSlots() == 0) {
        return;
    }

    // Contar pacientes ativos do tenant
    Long currentPatientCount = patientRepository.countActivePatientsByTenantId(account.getId());

    // Validar se atingiu o limite
    if (currentPatientCount >= account.getLimitSlots()) {
        throw new SlotLimitExceededException(
            String.format("Limite de pacientes atingido: %d/%d", 
                          currentPatientCount, account.getLimitSlots())
        );
    }
}
````
</augment_code_snippet>

---

## 📊 Exemplos de Uso

### Exemplo 1: Criar Paciente com Slots Disponíveis

**Cenário:**
- Conta B2B com `limitSlots = 5`
- Pacientes ativos: 3

**Request:**
```bash
POST /api/patients?tenantId=123e4567-e89b-12d3-a456-426614174000
Content-Type: application/json

{
  "whatsappNumber": "5511999999999",
  "name": "João Silva",
  "email": "joao@example.com",
  "diagnosis": "Ansiedade"
}
```

**Response (201 Created):**
```json
{
  "id": "987e6543-e21b-43d2-b654-426614174111",
  "whatsappNumber": "5511999999999",
  "name": "João Silva",
  "account": {
    "id": "123e4567-e89b-12d3-a456-426614174000"
  },
  "isActive": true
}
```

**Log:**
```
Tenant 123e4567 - Pacientes ativos: 3 / Limite: 5
Paciente criado com sucesso: 987e6543 (Tenant: 123e4567)
```

---

### Exemplo 2: Limite de Slots Atingido

**Cenário:**
- Conta B2B com `limitSlots = 5`
- Pacientes ativos: 5 (limite atingido)

**Request:**
```bash
POST /api/patients?tenantId=123e4567-e89b-12d3-a456-426614174000
Content-Type: application/json

{
  "whatsappNumber": "5511888888888",
  "name": "Maria Santos"
}
```

**Response (403 Forbidden):**
```json
{
  "error": "SLOT_LIMIT_EXCEEDED",
  "message": "Limite de pacientes atingido: 5/5. Faça upgrade do seu plano para adicionar mais pacientes."
}
```

**Log:**
```
Limite de slots atingido para tenant 123e4567: 5 / 5
```

---

### Exemplo 3: Conta B2C (Sem Limite)

**Cenário:**
- Conta B2C (paciente direto)
- Sem limite de slots

**Request:**
```bash
POST /api/patients?tenantId=456e7890-e12b-34d5-c678-426614174222
Content-Type: application/json

{
  "whatsappNumber": "5511777777777",
  "name": "Ana Costa"
}
```

**Response (201 Created):**
```json
{
  "id": "111e2222-e33b-44d5-e666-426614174333",
  "whatsappNumber": "5511777777777",
  "name": "Ana Costa"
}
```

**Log:**
```
Conta B2C não tem limite de slots
Paciente criado com sucesso
```

---

## 📈 Estatísticas de Uso de Slots

### Endpoint de Estatísticas

```bash
GET /api/patients/slots/stats?tenantId=123e4567-e89b-12d3-a456-426614174000
```

**Response:**
```json
{
  "activePatients": 4,
  "totalPatients": 5,
  "limit": 5,
  "available": 1,
  "isAtLimit": false,
  "usagePercentage": 80.0
}
```

### Interpretação

| Campo | Descrição | Exemplo |
|-------|-----------|---------|
| `activePatients` | Pacientes ativos (consomem slots) | 4 |
| `totalPatients` | Total de pacientes (ativos + inativos) | 5 |
| `limit` | Limite configurado | 5 |
| `available` | Slots disponíveis | 1 |
| `isAtLimit` | Se atingiu o limite | false |
| `usagePercentage` | Percentual de uso | 80% |

---

## 🔄 Desativar/Reativar Pacientes

### Desativar Paciente (Libera Slot)

```bash
PUT /api/patients/{patientId}/deactivate?tenantId={tenantId}
```

**Efeito:**
- `isActive = false`
- Libera 1 slot
- `activePatients` diminui

### Reativar Paciente (Consome Slot)

```bash
PUT /api/patients/{patientId}/reactivate?tenantId={tenantId}
```

**Validação:**
- ✅ Verifica se há slots disponíveis
- ❌ Se limite atingido, retorna erro 403

---

## 🎨 Casos de Uso

### Caso 1: Plano Básico (5 pacientes)

```
Account B2B
├── limitSlots: 5
└── Pacientes
    ├── Paciente 1 (ativo) ✅
    ├── Paciente 2 (ativo) ✅
    ├── Paciente 3 (ativo) ✅
    ├── Paciente 4 (ativo) ✅
    ├── Paciente 5 (ativo) ✅
    └── Paciente 6 (tentativa) ❌ BLOQUEADO
```

### Caso 2: Plano Premium (Ilimitado)

```
Account B2B
├── limitSlots: null (ou 0)
└── Pacientes
    ├── Paciente 1 (ativo) ✅
    ├── Paciente 2 (ativo) ✅
    ├── ... (quantos quiser) ✅
```

### Caso 3: Upgrade de Plano

**Antes:**
```
limitSlots: 5
activePatients: 5
→ Não pode adicionar mais
```

**Depois do Upgrade:**
```sql
UPDATE accounts 
SET limit_slots = 20 
WHERE id = '123e4567-e89b-12d3-a456-426614174000';
```

```
limitSlots: 20
activePatients: 5
→ Pode adicionar mais 15 pacientes
```

---

## 🧪 Testes Unitários

Foram criados testes para validar todos os cenários:

- ✅ Criar paciente com slots disponíveis
- ✅ Bloquear criação quando limite atingido
- ✅ Permitir criação em conta B2C sem validação
- ✅ Permitir criação em conta B2B sem limite configurado
- ✅ Bloquear reativação quando limite atingido
- ✅ Calcular estatísticas corretamente

**Executar testes:**
```bash
mvn test -Dtest=PatientServiceTest
```

---

## 💡 Benefícios

| Benefício | Descrição |
|-----------|-----------|
| **Monetização** | Controle de planos (Básico, Premium, Enterprise) |
| **Escalabilidade** | Limita recursos por tenant |
| **Flexibilidade** | B2C sem limite, B2B configurável |
| **Transparência** | API de estatísticas para dashboard |
| **Segurança** | Validação automática em todas as operações |

---

**Documentação criada em:** 2026-02-19

