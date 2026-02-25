# 📚 Swagger UI - Guia Completo de Teste

## 🎯 O que é Swagger?

O **Swagger UI** é uma interface web interativa que permite:
- ✅ Visualizar todas as APIs disponíveis
- ✅ Testar endpoints diretamente no navegador
- ✅ Ver exemplos de requisições e respostas
- ✅ Entender parâmetros e schemas
- ✅ Exportar documentação OpenAPI

---

## 🚀 Como Acessar

### **1. Iniciar a Aplicação**

```bash
# Via Maven
./mvnw spring-boot:run

# Ou via IDE (IntelliJ/Eclipse)
# Run AiHealthAgentApplication.java
```

### **2. Abrir o Swagger UI**

Acesse no navegador:

```
http://localhost:8080/swagger-ui.html
```

**Credenciais (Spring Security):**
- **Username:** `admin`
- **Password:** `admin123`

---

## 📊 Estrutura da Documentação

O Swagger UI organiza os endpoints em **3 tags principais**:

### **1. Dashboard** 🎯
APIs para visualização de estatísticas e alertas
- `GET /api/dashboard/patients` - Listar todos os pacientes
- `GET /api/dashboard/patients/{id}` - Estatísticas de um paciente
- `GET /api/dashboard/patients/{id}/conversations` - Resumo de conversas
- `GET /api/dashboard/alerts` - Todos os alertas ativos
- `GET /api/dashboard/alerts/critical` - Alertas críticos
- `GET /api/dashboard/patients/{id}/alerts` - Alertas de um paciente
- `POST /api/dashboard/alerts/{id}/acknowledge` - Reconhecer alerta

### **2. Webhook** 📨
Endpoint para receber mensagens da Evolution API
- `POST /webhook/whatsapp` - Receber mensagem do WhatsApp

### **3. Patients** 👥
Gerenciamento de pacientes
- `POST /api/patients` - Cadastrar novo paciente

---

## 🧪 Testando Endpoints no Swagger

### **Exemplo 1: Listar Todos os Pacientes**

**1. Expandir o endpoint:**
- Clique em `GET /api/dashboard/patients`

**2. Clicar em "Try it out"**

**3. Preencher parâmetros:**
```
tenantId: 123e4567-e89b-12d3-a456-426614174000
```

**4. Clicar em "Execute"**

**5. Ver resposta:**
```json
[
  {
    "patientId": "abc-123",
    "name": "João Silva",
    "diagnosis": "Fibromialgia",
    "healthStats": {
      "averagePainLevel": 6.5,
      "maxPainLevel": 9.0,
      "medicationAdherence": 85.0
    },
    "activeAlerts": [
      {
        "type": "HIGH_PAIN_LEVEL",
        "severity": "CRITICAL",
        "message": "Paciente João Silva reportou dor nível 9"
      }
    ]
  }
]
```

---

### **Exemplo 2: Reconhecer um Alerta**

**1. Expandir o endpoint:**
- Clique em `POST /api/dashboard/alerts/{alertId}/acknowledge`

**2. Clicar em "Try it out"**

**3. Preencher parâmetros:**
```
alertId: alert-uuid-here
tenantId: tenant-uuid-here
```

**4. Preencher Request Body:**
```json
{
  "acknowledgedBy": "Dr. Maria Santos"
}
```

**5. Clicar em "Execute"**

**6. Ver resposta:**
```json
{
  "status": "success",
  "message": "Alerta reconhecido com sucesso",
  "alertId": "alert-uuid-here"
}
```

---

### **Exemplo 3: Testar Webhook (Simular Evolution API)**

**1. Expandir o endpoint:**
- Clique em `POST /webhook/whatsapp`

**2. Clicar em "Try it out"**

**3. Adicionar Header:**
```
X-Webhook-Key: default-secret
```

**4. Preencher Request Body:**
```json
{
  "event": "messages.upsert",
  "instance": "instance-name",
  "data": {
    "key": {
      "remoteJid": "5511999999999@s.whatsapp.net",
      "fromMe": false,
      "id": "msg-123"
    },
    "message": {
      "conversation": "Estou com dor 8 hoje"
    },
    "messageTimestamp": 1708531200,
    "pushName": "João Silva"
  }
}
```

**5. Clicar em "Execute"**

**6. Ver resposta:**
```json
{
  "status": "success",
  "message": "Mensagem processada com sucesso"
}
```

---

## 🔍 Recursos Avançados do Swagger

### **1. Schemas**
Clique em "Schemas" no final da página para ver todos os DTOs:
- `PatientStatsDTO`
- `ConversationSummaryDTO`
- `Alert`
- `EvolutionApiWebhookDTO`

### **2. Filtrar Endpoints**
Use a caixa de busca no topo para filtrar endpoints por nome.

### **3. Exportar Documentação**
Acesse a documentação OpenAPI em JSON:
```
http://localhost:8080/v3/api-docs
```

Ou em YAML:
```
http://localhost:8080/v3/api-docs.yaml
```

### **4. Copiar cURL**
Após executar um endpoint, clique em "Copy as cURL" para copiar o comando.

---

## 📝 Exemplos de Testes Completos

### **Cenário 1: Monitorar Paciente com Dor Alta**

**1. Simular mensagem do paciente:**
```bash
POST /webhook/whatsapp
Header: X-Webhook-Key: default-secret
Body: {
  "event": "messages.upsert",
  "data": {
    "key": {"remoteJid": "5511999999999@s.whatsapp.net", "fromMe": false},
    "message": {"conversation": "Estou com dor 9 hoje"}
  }
}
```

**2. Verificar alertas criados:**
```bash
GET /api/dashboard/alerts?tenantId=xxx
```

**3. Ver estatísticas do paciente:**
```bash
GET /api/dashboard/patients/{patientId}?tenantId=xxx
```

**4. Reconhecer alerta:**
```bash
POST /api/dashboard/alerts/{alertId}/acknowledge?tenantId=xxx
Body: {"acknowledgedBy": "Dr. Maria"}
```

---

### **Cenário 2: Analisar Conversas**

**1. Buscar resumo de conversas (últimos 30 dias):**
```bash
GET /api/dashboard/patients/{patientId}/conversations?tenantId=xxx
```

**2. Ver mensagens recentes:**
```json
{
  "totalMessages": 45,
  "recentMessages": [
    {
      "role": "USER",
      "content": "Estou com dor 8 hoje",
      "timestamp": "2026-02-19T15:30:00"
    },
    {
      "role": "ASSISTANT",
      "content": "Entendi, registrei sua dor nível 8...",
      "timestamp": "2026-02-19T15:30:05"
    }
  ]
}
```

---

## 🛠️ Troubleshooting

### **Problema 1: "401 Unauthorized"**
**Solução:** Fazer login com credenciais:
- Username: `admin`
- Password: `admin123`

### **Problema 2: "404 Not Found" no Swagger UI**
**Solução:** Verificar se a aplicação está rodando:
```bash
curl http://localhost:8080/actuator/health
```

### **Problema 3: Webhook retorna "Invalid API Key"**
**Solução:** Adicionar header correto:
```
X-Webhook-Key: default-secret
```

### **Problema 4: "Paciente não encontrado"**
**Solução:** Cadastrar paciente primeiro via banco de dados ou API.

---

## 📚 Documentação Adicional

- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Documentação Completa:** [FASE4_DASHBOARD_ANALYTICS.md](FASE4_DASHBOARD_ANALYTICS.md)

---

**🎉 Swagger configurado e pronto para uso!**

Agora você pode testar todas as APIs diretamente no navegador! 🚀

