# 🐳 Docker Compose - Guia de Testes Locais

## 📋 Índice
1. [Visão Geral](#visão-geral)
2. [Pré-requisitos](#pré-requisitos)
3. [Configuração Inicial](#configuração-inicial)
4. [Subindo o Ambiente](#subindo-o-ambiente)
5. [Testando a Aplicação](#testando-a-aplicação)
6. [Populando Dados de Teste](#populando-dados-de-teste)
7. [Troubleshooting](#troubleshooting)

---

## 🎯 Visão Geral

Este guia mostra como rodar **AI Health Agent** localmente usando Docker Compose para:
- ✅ PostgreSQL (banco de dados)
- ✅ Evolution API (gateway WhatsApp)
- ✅ PgAdmin (interface web para PostgreSQL - opcional)

**Benefícios:**
- 🚀 Ambiente completo em 1 comando
- 🔄 Fácil reset e recriação
- 📦 Isolado do sistema
- 🧪 Perfeito para testes

---

## 📦 Pré-requisitos

### **1. Instalar Docker**
- **Windows/Mac:** [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- **Linux:** [Docker Engine](https://docs.docker.com/engine/install/)

### **2. Verificar Instalação**
```bash
docker --version
docker-compose --version
```

### **3. Ferramentas Necessárias**
- ✅ Java 21+ (para rodar a aplicação Spring Boot)
- ✅ Maven (incluído no projeto via `mvnw`)
- ✅ Git
- ✅ curl ou Postman (para testes de API)

---

## ⚙️ Configuração Inicial

### **1. Clonar o Repositório**
```bash
git clone <seu-repositorio>
cd ai-health-agent
```

### **2. Criar Arquivo `.env`**
```bash
# Copiar template
cp .env.example .env

# Editar com suas credenciais
# Windows: notepad .env
# Linux/Mac: nano .env
```

**Variáveis obrigatórias:**
```env
# OpenAI API Key (obtenha em: https://platform.openai.com/api-keys)
OPENAI_API_KEY=sk-your-real-openai-key-here

# Evolution API Key (pode ser qualquer string para testes locais)
EVOLUTION_API_KEY=test-api-key-123

# Senha do PostgreSQL
DATABASE_PASSWORD=postgres
```

---

## 🚀 Subindo o Ambiente

### **Opção 1: Ambiente Completo (Recomendado)**

```bash
# Subir PostgreSQL + Evolution API
docker-compose -f docker-compose.test.yml up -d

# Verificar status
docker-compose -f docker-compose.test.yml ps
```

**Serviços disponíveis:**
- 🐘 **PostgreSQL:** `localhost:5432`
- 📱 **Evolution API:** `localhost:8081`

---

### **Opção 2: Com PgAdmin (Interface Web)**

```bash
# Subir com PgAdmin
docker-compose -f docker-compose.test.yml --profile tools up -d
```

**Serviços adicionais:**
- 🖥️ **PgAdmin:** `http://localhost:5050`
  - Email: `admin@aihealth.com`
  - Senha: `admin123`

---

### **Verificar Logs**

```bash
# Ver logs de todos os serviços
docker-compose -f docker-compose.test.yml logs -f

# Ver logs apenas do PostgreSQL
docker-compose -f docker-compose.test.yml logs -f postgres

# Ver logs apenas da Evolution API
docker-compose -f docker-compose.test.yml logs -f evolution-api
```

---

## 🏃 Testando a Aplicação

### **1. Rodar Aplicação Spring Boot**

```bash
# Compilar
./mvnw clean install

# Rodar com profile Docker
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

**Ou com variáveis de ambiente:**
```bash
# Windows (PowerShell)
$env:SPRING_PROFILES_ACTIVE="docker"
./mvnw spring-boot:run

# Linux/Mac
export SPRING_PROFILES_ACTIVE=docker
./mvnw spring-boot:run
```

---

### **2. Verificar Saúde da Aplicação**

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
# Abra no navegador: http://localhost:8080/swagger-ui.html
```

---

### **3. Testar Conexão com Banco**

```bash
# Conectar via psql (se tiver instalado)
psql -h localhost -p 5432 -U postgres -d ai_health_agent

# Ou via Docker
docker exec -it ai-health-postgres-test psql -U postgres -d ai_health_agent
```

**Comandos úteis no psql:**
```sql
-- Listar tabelas
\dt

-- Ver estrutura de uma tabela
\d accounts

-- Contar registros
SELECT COUNT(*) FROM accounts;

-- Sair
\q
```

---

## 🌱 Populando Dados de Teste

### **Opção 1: Script Automatizado (Recomendado)**

```bash
# Dar permissão de execução (Linux/Mac)
chmod +x scripts/seed-test-data.sh

# Executar
./scripts/seed-test-data.sh
```

**O script cria:**
- ✅ 2 Accounts (1 B2B + 1 B2C)
- ✅ 3 Pacientes
- ✅ 3 Agendamentos de check-in proativo

---

### **Opção 2: Via Swagger UI**

1. Acesse: `http://localhost:8080/swagger-ui.html`
2. Autentique: `admin` / `admin123`
3. Crie manualmente via endpoints

---

## 🧪 Cenários de Teste

### **1. Testar Webhook do WhatsApp**

```bash
# Simular mensagem recebida
curl -X POST http://localhost:8080/webhook/whatsapp \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Key: default-secret" \
  -d '{
    "event": "messages.upsert",
    "data": {
      "key": {
        "remoteJid": "5511999990001@s.whatsapp.net",
        "fromMe": false
      },
      "message": {
        "conversation": "Estou com dor nível 8 hoje"
      }
    }
  }'
```

---

### **2. Testar Dashboard**

```bash
# Listar pacientes
curl -u admin:admin123 \
  "http://localhost:8080/api/dashboard/patients?tenantId=<ACCOUNT_ID>"

# Ver estatísticas
curl -u admin:admin123 \
  "http://localhost:8080/api/dashboard/patients/<PATIENT_ID>/stats?tenantId=<ACCOUNT_ID>"
```

---

### **3. Testar Check-ins Proativos**

```bash
# Listar agendamentos
curl -u admin:admin123 \
  http://localhost:8080/api/checkin-schedules

# Ver histórico de execuções
curl -u admin:admin123 \
  http://localhost:8080/api/checkin-schedules/<SCHEDULE_ID>/executions

# Ver estatísticas de rate limiting
curl -u admin:admin123 \
  http://localhost:8080/api/checkin-schedules/stats/rate-limit
```

---

## 🔧 Troubleshooting

### **Problema: Porta 5432 já está em uso**

```bash
# Verificar o que está usando a porta
# Windows
netstat -ano | findstr :5432

# Linux/Mac
lsof -i :5432

# Solução 1: Parar o PostgreSQL local
# Windows: Services > PostgreSQL > Stop
# Linux: sudo systemctl stop postgresql

# Solução 2: Mudar porta no docker-compose.test.yml
# Alterar "5432:5432" para "5433:5432"
# E no .env: DATABASE_URL=jdbc:postgresql://localhost:5433/ai_health_agent
```

---

### **Problema: Evolution API não conecta**

```bash
# Verificar logs
docker-compose -f docker-compose.test.yml logs evolution-api

# Reiniciar serviço
docker-compose -f docker-compose.test.yml restart evolution-api
```

---

### **Problema: Migrations não rodam**

```bash
# Verificar se Flyway está habilitado
# application-docker.properties deve ter:
# spring.flyway.enabled=true

# Forçar migrations manualmente
./mvnw flyway:migrate -Dspring.profiles.active=docker
```

---

### **Resetar Ambiente Completamente**

```bash
# Parar e remover TUDO (containers + volumes + networks)
docker-compose -f docker-compose.test.yml down -v

# Remover imagens (opcional)
docker-compose -f docker-compose.test.yml down -v --rmi all

# Subir novamente
docker-compose -f docker-compose.test.yml up -d
```

---

## 📊 Monitoramento

### **Ver Recursos Usados**

```bash
# Uso de CPU/Memória
docker stats

# Espaço em disco
docker system df
```

---

### **Acessar PgAdmin**

1. Abra: `http://localhost:5050`
2. Login: `admin@aihealth.com` / `admin123`
3. Adicionar servidor:
   - **Name:** AI Health Local
   - **Host:** `postgres` (nome do container)
   - **Port:** `5432`
   - **Username:** `postgres`
   - **Password:** `postgres`

---

## 🎓 Boas Práticas

1. ✅ **Sempre use `.env`** - Nunca commite credenciais
2. ✅ **Reset frequente** - `docker-compose down -v` limpa tudo
3. ✅ **Monitore logs** - `docker-compose logs -f`
4. ✅ **Use profiles** - Separe dev/test/prod
5. ✅ **Backup dados** - Antes de `down -v`

---

## 🚀 Próximos Passos

1. ✅ Subir ambiente Docker
2. ✅ Rodar aplicação Spring Boot
3. ✅ Popular dados de teste
4. ✅ Testar via Swagger UI
5. ✅ Simular webhooks
6. ✅ Monitorar check-ins proativos

---

**🎉 Ambiente de Testes Pronto!**

