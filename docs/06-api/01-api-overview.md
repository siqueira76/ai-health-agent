# 6.1 Visão Geral da API

## 🌐 API REST

O AI Health Agent expõe uma API REST completa para integração com clientes externos e dashboard.

---

## 📡 Base URL

### **Desenvolvimento**
```
http://localhost:8080
```

### **Produção**
```
https://api.healthlink.com
```

---

## 🔐 Autenticação

### **Desenvolvimento (Basic Auth)**

```bash
curl -u admin:admin123 http://localhost:8080/api/patients
```

### **Produção (JWT - Futuro)**

```bash
curl -H "Authorization: Bearer <token>" https://api.healthlink.com/api/patients
```

---

## 📚 Documentação Interativa

### **Swagger UI**

Acesse a documentação interativa em:

```
http://localhost:8080/swagger-ui.html
```

**Funcionalidades:**
- ✅ Testar endpoints diretamente no navegador
- ✅ Ver schemas de request/response
- ✅ Copiar exemplos de código
- ✅ Exportar OpenAPI spec

### **OpenAPI JSON**

Baixe a especificação OpenAPI em:

```
http://localhost:8080/v3/api-docs
```

---

## 🎯 Endpoints Principais

### **1. Webhook (WhatsApp)**

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/webhook/whatsapp` | Recebe mensagens do WhatsApp |

**Uso:** Configurado na Evolution API para receber eventos.

---

### **2. Dashboard**

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/dashboard/stats` | Estatísticas gerais |
| GET | `/api/dashboard/alerts` | Alertas não lidos |
| GET | `/api/dashboard/trends` | Tendências de saúde |

**Uso:** Alimenta o dashboard web (futuro).

---

### **3. Patients**

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/patients` | Lista todos os pacientes |
| GET | `/api/patients/{id}` | Busca paciente por ID |
| POST | `/api/patients` | Cria novo paciente |
| PUT | `/api/patients/{id}` | Atualiza paciente |
| DELETE | `/api/patients/{id}` | Remove paciente |

---

### **4. Health Logs**

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/health-logs` | Lista logs de saúde |
| GET | `/api/health-logs/patient/{patientId}` | Logs de um paciente |
| POST | `/api/health-logs` | Cria novo log |

---

### **5. Alerts**

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/alerts` | Lista todos os alertas |
| GET | `/api/alerts/unread` | Alertas não lidos |
| PUT | `/api/alerts/{id}/read` | Marca alerta como lido |

---

### **6. Check-ins**

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/checkins/schedules` | Lista agendamentos |
| POST | `/api/checkins/schedules` | Cria agendamento |
| PUT | `/api/checkins/schedules/{id}` | Atualiza agendamento |
| DELETE | `/api/checkins/schedules/{id}` | Remove agendamento |

---

## 📋 Formatos de Request/Response

### **Content-Type**

Todos os endpoints aceitam e retornam JSON:

```
Content-Type: application/json
Accept: application/json
```

### **Estrutura de Resposta Padrão**

#### **Sucesso (200 OK)**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Maria Silva",
  "whatsappNumber": "5511999999999",
  "isActive": true,
  "createdAt": "2026-02-25T10:30:00Z"
}
```

#### **Lista (200 OK)**

```json
{
  "content": [
    { "id": "...", "name": "..." },
    { "id": "...", "name": "..." }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "size": 10,
  "number": 0
}
```

#### **Erro (4xx/5xx)**

```json
{
  "timestamp": "2026-02-25T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/patients",
  "errors": [
    {
      "field": "whatsappNumber",
      "message": "must not be blank"
    }
  ]
}
```

---

## 🔍 Paginação

Endpoints de listagem suportam paginação:

```bash
GET /api/patients?page=0&size=20&sort=name,asc
```

**Parâmetros:**
- `page` - Número da página (0-indexed)
- `size` - Itens por página (padrão: 10, máx: 100)
- `sort` - Campo e direção (ex: `name,asc`)

---

## 🔎 Filtros

Alguns endpoints suportam filtros:

```bash
GET /api/health-logs?patientId=123&startDate=2026-02-01&endDate=2026-02-28
```

---

## 📊 Rate Limiting

### **Limites Atuais**

| Endpoint | Limite |
|----------|--------|
| `/webhook/whatsapp` | 100 req/min |
| `/api/*` | 1000 req/min |

### **Headers de Resposta**

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1614556800
```

---

## 🚨 Códigos de Status HTTP

| Código | Significado | Quando usar |
|--------|-------------|-------------|
| 200 | OK | Sucesso (GET, PUT) |
| 201 | Created | Recurso criado (POST) |
| 204 | No Content | Sucesso sem corpo (DELETE) |
| 400 | Bad Request | Validação falhou |
| 401 | Unauthorized | Não autenticado |
| 403 | Forbidden | Sem permissão |
| 404 | Not Found | Recurso não encontrado |
| 409 | Conflict | Conflito (ex: duplicado) |
| 429 | Too Many Requests | Rate limit excedido |
| 500 | Internal Server Error | Erro no servidor |

---

## 🔐 Multi-Tenancy

Todos os endpoints respeitam isolamento de tenant:

```bash
# Cada request é automaticamente filtrado pelo tenant do usuário autenticado
GET /api/patients
# Retorna apenas pacientes do tenant atual
```

**Implementação:**
- `TenantContext` extraído do token JWT
- Queries automáticas com `account_id`
- Impossível acessar dados de outro tenant

---

## 📝 Exemplos de Uso

### **Criar Paciente**

```bash
curl -X POST http://localhost:8080/api/patients \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maria Silva",
    "whatsappNumber": "5511999999999"
  }'
```

### **Buscar Estatísticas**

```bash
curl -X GET http://localhost:8080/api/dashboard/stats \
  -u admin:admin123
```

### **Listar Alertas Não Lidos**

```bash
curl -X GET http://localhost:8080/api/alerts/unread \
  -u admin:admin123
```

---

## 🎯 Próximos Passos

1. 📡 [Webhook WhatsApp](02-webhook-whatsapp.md)
2. 📊 [Dashboard Endpoints](03-dashboard-endpoints.md)
3. 🔐 [Autenticação](04-authentication.md)

---

[⬅️ Anterior: Relacionamentos](../03-database/04-relationships.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Webhook WhatsApp](02-webhook-whatsapp.md)

