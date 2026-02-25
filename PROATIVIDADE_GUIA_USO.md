# 🤖 Guia de Uso: Mensagens Proativas

## 📋 Índice
1. [Visão Geral](#visão-geral)
2. [Como Funciona](#como-funciona)
3. [Configuração Inicial](#configuração-inicial)
4. [Exemplos de Uso](#exemplos-de-uso)
5. [APIs Disponíveis](#apis-disponíveis)
6. [Rate Limiting](#rate-limiting)
7. [Monitoramento](#monitoramento)

---

## 🎯 Visão Geral

O sistema de **Mensagens Proativas** permite que a IA inicie conversas automaticamente com os pacientes em horários agendados, sem necessidade de intervenção manual.

### **Casos de Uso:**
- ✅ Check-in diário de sintomas
- ✅ Lembretes de medicação
- ✅ Acompanhamento pós-consulta
- ✅ Monitoramento de crises
- ✅ Engajamento preventivo

---

## ⚙️ Como Funciona

### **Fluxo de Execução:**

```
1. Job Agendado (a cada minuto)
   ↓
2. ShedLock (garante execução única)
   ↓
3. Busca schedules prontos (next_execution_at <= now)
   ↓
4. Para cada schedule:
   - Verifica rate limiting
   - Estabelece contexto do tenant
   - Gera mensagem (IA ou customizada)
   - Envia via Evolution API
   - Registra execução
   - Calcula próxima execução
   ↓
5. Libera lock
```

### **Tipos de Agendamento:**

| Tipo | Descrição | Exemplo |
|------|-----------|---------|
| **DAILY** | Todos os dias no mesmo horário | 09:00 todos os dias |
| **WEEKLY** | Dias específicos da semana | Segunda, Quarta, Sexta às 14:00 |
| **CUSTOM** | Futuro: Intervalos personalizados | A cada 3 dias |

---

## 🚀 Configuração Inicial

### **1. Criar Agendamento**

**Endpoint:** `POST /api/checkin-schedules`

**Request:**
```json
{
  "patientId": "123e4567-e89b-12d3-a456-426614174000",
  "scheduleType": "DAILY",
  "timeOfDay": "09:00:00",
  "timezone": "America/Sao_Paulo",
  "useAiGeneration": true,
  "maxMessagesPerDay": 3,
  "isActive": true
}
```

**Response:**
```json
{
  "id": "abc12345-...",
  "patientId": "123e4567-...",
  "patientName": "Maria Silva",
  "scheduleType": "DAILY",
  "timeOfDay": "09:00:00",
  "useAiGeneration": true,
  "maxMessagesPerDay": 3,
  "messagesSentToday": 0,
  "isActive": true,
  "nextExecutionAt": "2026-02-20T09:00:00"
}
```

---

## 📚 Exemplos de Uso

### **Exemplo 1: Check-in Diário com IA**

```json
{
  "patientId": "...",
  "scheduleType": "DAILY",
  "timeOfDay": "09:00:00",
  "useAiGeneration": true,
  "maxMessagesPerDay": 3
}
```

**Mensagem gerada pela IA:**
> "Bom dia, Maria! Como você está se sentindo hoje? Vi que ontem você mencionou dor nível 7. Melhorou?"

---

### **Exemplo 2: Lembrete de Medicação (Mensagem Fixa)**

```json
{
  "patientId": "...",
  "scheduleType": "DAILY",
  "timeOfDay": "08:00:00",
  "useAiGeneration": false,
  "customMessage": "🔔 Lembrete: Hora de tomar sua medicação!",
  "maxMessagesPerDay": 1
}
```

---

### **Exemplo 3: Check-in Semanal (Dias Úteis)**

```json
{
  "patientId": "...",
  "scheduleType": "WEEKLY",
  "timeOfDay": "14:00:00",
  "daysOfWeek": [1, 2, 3, 4, 5],
  "useAiGeneration": true,
  "maxMessagesPerDay": 2
}
```

**Dias da semana:**
- 1 = Segunda
- 2 = Terça
- 3 = Quarta
- 4 = Quinta
- 5 = Sexta
- 6 = Sábado
- 7 = Domingo

---

## 🔌 APIs Disponíveis

### **1. Criar Agendamento**
```http
POST /api/checkin-schedules
```

### **2. Listar Agendamentos**
```http
GET /api/checkin-schedules
```

### **3. Buscar por ID**
```http
GET /api/checkin-schedules/{scheduleId}
```

### **4. Atualizar Agendamento**
```http
PUT /api/checkin-schedules/{scheduleId}
```

### **5. Ativar/Desativar**
```http
PUT /api/checkin-schedules/{scheduleId}/toggle
```

### **6. Deletar Agendamento**
```http
DELETE /api/checkin-schedules/{scheduleId}
```

### **7. Histórico de Execuções**
```http
GET /api/checkin-schedules/{scheduleId}/executions
```

### **8. Estatísticas de Rate Limiting**
```http
GET /api/checkin-schedules/stats/rate-limit
```

---

## 🚦 Rate Limiting

### **3 Níveis de Controle:**

#### **1. Nível Paciente**
- Configurável por schedule (`maxMessagesPerDay`)
- Padrão: **3 mensagens/dia**
- Resetado à meia-noite

#### **2. Nível Tenant**
- **B2B (Psicólogos):** 100 mensagens/dia
- **B2C (Fibromialgia):** 50 mensagens/dia

#### **3. Nível Global** (Futuro)
- Controle de custos em tempo real
- Implementação com Redis

---

## 📊 Monitoramento

### **Verificar Estatísticas:**

```http
GET /api/checkin-schedules/stats/rate-limit
```

**Response:**
```json
{
  "messagesUsed": 45,
  "dailyLimit": 100,
  "remaining": 55,
  "usagePercentage": 45.0
}
```

### **Histórico de Execuções:**

```http
GET /api/checkin-schedules/{scheduleId}/executions
```

**Response:**
```json
[
  {
    "id": "...",
    "executedAt": "2026-02-19T09:00:00",
    "status": "SUCCESS",
    "messageSent": "Bom dia! Como você está?",
    "messageId": "whatsapp-msg-123",
    "patientResponded": true,
    "responseReceivedAt": "2026-02-19T09:05:00",
    "executionDurationMs": 1250
  }
]
```

### **Status Possíveis:**
- ✅ **SUCCESS** - Mensagem enviada com sucesso
- ❌ **FAILED** - Erro no envio
- ⏭️ **SKIPPED** - Pulado por rate limit

---

## 🔧 Troubleshooting

### **Mensagem não foi enviada?**

1. Verificar se schedule está ativo:
   ```http
   GET /api/checkin-schedules/{scheduleId}
   ```

2. Verificar rate limiting:
   ```http
   GET /api/checkin-schedules/stats/rate-limit
   ```

3. Verificar histórico de execuções:
   ```http
   GET /api/checkin-schedules/{scheduleId}/executions
   ```

### **Como desativar temporariamente?**

```http
PUT /api/checkin-schedules/{scheduleId}/toggle
```

---

## 🎓 Boas Práticas

1. ✅ **Use IA para personalização** - Mensagens mais naturais e contextualizadas
2. ✅ **Configure rate limiting adequado** - Evite spam
3. ✅ **Monitore execuções** - Acompanhe taxa de resposta
4. ✅ **Ajuste horários** - Respeite rotina do paciente
5. ✅ **Teste antes de ativar** - Valide configuração

---

**🎉 Sistema de Mensagens Proativas Pronto para Uso!**

