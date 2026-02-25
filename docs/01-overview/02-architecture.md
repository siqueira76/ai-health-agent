# 1.2 Arquitetura da Solução

## 🏗️ Visão Geral da Arquitetura

O AI Health Agent utiliza uma arquitetura moderna baseada em **microserviços**, **event-driven** e **multi-tenant**, garantindo escalabilidade, segurança e manutenibilidade.

---

## 📐 Diagrama de Arquitetura de Alto Nível

```
┌─────────────────────────────────────────────────────────────────┐
│                         USUÁRIOS                                 │
│  👤 Pacientes (WhatsApp)    👨‍⚕️ Profissionais (Dashboard)        │
└────────────────┬────────────────────────────┬───────────────────┘
                 │                            │
                 ▼                            ▼
┌────────────────────────────┐  ┌──────────────────────────────┐
│    Evolution API           │  │   Web Dashboard (Futuro)     │
│  (WhatsApp Gateway)        │  │   (React/Next.js)            │
└────────────┬───────────────┘  └──────────────┬───────────────┘
             │                                  │
             │ Webhook                          │ REST API
             ▼                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AI HEALTH AGENT (Spring Boot)                │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Webhook     │  │  Dashboard   │  │  Scheduler   │         │
│  │  Controller  │  │  Controller  │  │  (Cron Jobs) │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         │                  │                  │                 │
│         └──────────────────┼──────────────────┘                 │
│                            ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              SERVICE LAYER                              │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │   │
│  │  │   AI     │ │  Health  │ │ Checkin  │ │  Alert   │  │   │
│  │  │ Service  │ │  Service │ │ Service  │ │ Service  │  │   │
│  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘  │   │
│  └───────┼────────────┼────────────┼────────────┼─────────┘   │
│          │            │            │            │              │
│          ▼            ▼            ▼            ▼              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │           REPOSITORY LAYER (Spring Data JPA)            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                    │
└────────────────────────────┼────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EXTERNAL SERVICES                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  PostgreSQL  │  │   OpenAI     │  │    Redis     │          │
│  │  (Database)  │  │  (GPT-4o)    │  │   (Cache)    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Componentes Principais

### **1. Camada de Apresentação**

#### **WhatsApp (via Evolution API)**
- Interface principal para pacientes
- Comunicação assíncrona via webhooks
- Suporte a texto, imagens, áudio (futuro)

#### **Dashboard Web (Futuro)**
- Interface para profissionais de saúde
- Visualização de métricas e alertas
- Gerenciamento de pacientes

---

### **2. Camada de Aplicação (Spring Boot)**

#### **Controllers**
- `WhatsappWebhookController` - Recebe mensagens do WhatsApp
- `DashboardController` - Endpoints para dashboard
- `PatientController` - CRUD de pacientes

#### **Services**
- `AIService` - Integração com OpenAI
- `HealthLogService` - Gerenciamento de logs de saúde
- `CheckinScheduleService` - Agendamento de check-ins
- `AlertService` - Detecção e notificação de alertas
- `WhatsappService` - Envio de mensagens

#### **Repositories**
- Spring Data JPA para acesso ao banco
- Queries otimizadas com JPQL
- Suporte a multi-tenancy

---

### **3. Camada de Dados**

#### **PostgreSQL**
- Banco de dados relacional principal
- Armazena: accounts, patients, health_logs, chat_messages, alerts
- Migrations gerenciadas por Flyway

#### **Redis (Futuro)**
- Cache de sessões
- Rate limiting
- Filas de mensagens

---

### **4. Serviços Externos**

#### **OpenAI API**
- Modelo: GPT-4o-mini
- Function Calling para extração de dados
- Streaming de respostas (futuro)

#### **Evolution API**
- Gateway para WhatsApp Business API
- Gerenciamento de instâncias
- Webhooks de eventos

---

## 🔄 Fluxos Principais

### **Fluxo 1: Recebimento de Mensagem**

```
1. Paciente envia mensagem no WhatsApp
   ↓
2. Evolution API recebe e envia webhook
   ↓
3. WhatsappWebhookController processa evento
   ↓
4. TenantIdentificationService identifica tenant/paciente
   ↓
5. AIService processa mensagem com contexto
   ↓
6. OpenAI retorna resposta + function calls
   ↓
7. HealthLogService salva dados extraídos
   ↓
8. AlertService verifica condições de alerta
   ↓
9. WhatsappService envia resposta ao paciente
```

---

### **Fluxo 2: Check-in Proativo**

```
1. Scheduler executa job a cada minuto
   ↓
2. CheckinScheduleService busca agendamentos prontos
   ↓
3. Para cada agendamento:
   ├─ Verifica rate limiting (máx 3/dia)
   ├─ Monta mensagem personalizada
   ├─ Envia via WhatsappService
   ├─ Registra execução
   └─ Atualiza próxima execução
```

---

### **Fluxo 3: Detecção de Alertas**

```
1. HealthLogService salva novo log
   ↓
2. AlertService.detectCrisis() é chamado
   ↓
3. Verifica condições:
   ├─ Dor > 8 → CRITICAL
   ├─ Humor muito baixo → HIGH
   ├─ Medicação não tomada → MEDIUM
   └─ Tendência negativa → LOW
   ↓
4. Se alerta detectado:
   ├─ Salva no banco
   ├─ Notifica profissional (futuro)
   └─ Registra no log
```

---

## 🏢 Multi-Tenancy

### **Estratégia: Shared Database, Shared Schema**

Todos os tenants compartilham o mesmo banco e schema, mas os dados são isolados por `account_id`.

**Vantagens:**
- ✅ Custo reduzido (um único banco)
- ✅ Manutenção simplificada
- ✅ Escalabilidade horizontal

**Implementação:**
```java
// Todas as queries incluem account_id
@Query("SELECT h FROM HealthLog h WHERE h.account.id = :tenantId")
List<HealthLog> findByTenant(@Param("tenantId") UUID tenantId);
```

**Segurança:**
- `TenantContext` armazenado em ThreadLocal
- Validação em todas as operações
- Impossível acessar dados de outro tenant

---

## 🔐 Segurança

### **Camadas de Segurança**

1. **Autenticação**
   - Spring Security (Basic Auth em dev)
   - JWT tokens (produção - futuro)

2. **Autorização**
   - Role-based access control (RBAC)
   - Tenant isolation em todas as queries

3. **Dados Sensíveis**
   - Senhas hasheadas (BCrypt)
   - API keys em variáveis de ambiente
   - HTTPS obrigatório em produção

4. **Rate Limiting**
   - Máximo 3 mensagens proativas/dia por paciente
   - Proteção contra spam

---

## 📈 Escalabilidade

### **Horizontal Scaling**

```
┌─────────────┐
│ Load        │
│ Balancer    │
└──────┬──────┘
       │
   ┌───┴───┬───────┬───────┐
   ▼       ▼       ▼       ▼
┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐
│ App │ │ App │ │ App │ │ App │
│  1  │ │  2  │ │  3  │ │  4  │
└──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘
   └───────┴───────┴───────┘
              │
         ┌────┴────┐
         ▼         ▼
    ┌────────┐ ┌────────┐
    │  DB    │ │ Redis  │
    │ Master │ │ Cluster│
    └────────┘ └────────┘
```

**Suporte:**
- ✅ Stateless application (sem sessões em memória)
- ✅ ShedLock para jobs distribuídos
- ✅ Database connection pooling (HikariCP)

---

## 🎯 Próximos Passos

1. 📖 Veja as [Tecnologias Utilizadas](03-technologies.md)
2. 💼 Entenda os [Modelos de Negócio](04-business-models.md)
3. 🗄️ Explore a [Estrutura do Banco](../03-database/01-database-structure.md)

---

[⬅️ Anterior: Introdução](01-introduction.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Tecnologias](03-technologies.md)

