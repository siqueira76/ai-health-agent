# 2.4 Primeiro Deploy

## 🚀 Deploy Local (Desenvolvimento)

### **1. Verificar Pré-requisitos**

```bash
# Java
java -version

# PostgreSQL (Docker)
docker ps | grep postgres

# Variáveis de ambiente
echo $OPENAI_API_KEY
```

### **2. Executar Aplicação**

```bash
# Via Maven Wrapper
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Ou via JAR
./mvnw clean package -DskipTests
java -jar target/ai-health-agent-1.0.0.jar --spring.profiles.active=dev
```

### **3. Verificar Saúde**

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger
open http://localhost:8080/swagger-ui.html
```

---

## 🐳 Deploy com Docker

### **1. Build da Imagem**

```bash
# Build
docker build -t ai-health-agent:latest .

# Verificar
docker images | grep ai-health-agent
```

### **2. Executar Container**

```bash
docker run -d \
  --name ai-health-agent \
  -p 8080:8080 \
  --env-file .env \
  ai-health-agent:latest
```

### **3. Ver Logs**

```bash
docker logs -f ai-health-agent
```

---

## ☁️ Deploy no Railway

### **1. Criar Conta**

1. Acesse: https://railway.app/
2. Faça login com GitHub
3. Crie novo projeto

### **2. Adicionar PostgreSQL**

1. New → Database → PostgreSQL
2. Copie as credenciais geradas

### **3. Adicionar Aplicação**

1. New → GitHub Repo
2. Selecione `ai-health-agent`
3. Configure variáveis de ambiente:

```bash
OPENAI_API_KEY=sk-proj-xxx
EVOLUTION_API_URL=https://sua-evolution-api.com
EVOLUTION_API_KEY=xxx
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=${{Postgres.DATABASE_URL}}
ADMIN_USERNAME=admin
ADMIN_PASSWORD=senha_forte_aqui
```

### **4. Deploy Automático**

Railway detecta automaticamente o `pom.xml` e faz deploy.

**Logs:**
```
✅ Building...
✅ Running Flyway migrations...
✅ Started AiHealthAgentApplication
✅ Deployed to: https://ai-health-agent-production.up.railway.app
```

---

## 🌐 Deploy no Render

### **1. Criar Conta**

1. Acesse: https://render.com/
2. Faça login com GitHub

### **2. Criar PostgreSQL**

1. New → PostgreSQL
2. Nome: `ai-health-agent-db`
3. Copie a URL de conexão

### **3. Criar Web Service**

1. New → Web Service
2. Conecte ao repositório GitHub
3. Configure:

```yaml
Name: ai-health-agent
Environment: Docker
Build Command: ./mvnw clean package -DskipTests
Start Command: java -jar target/ai-health-agent-1.0.0.jar
```

4. Adicione variáveis de ambiente (igual Railway)

### **4. Deploy**

Render faz deploy automático a cada push no GitHub.

---

## 🔍 Verificação Pós-Deploy

### **1. Health Check**

```bash
curl https://sua-app.railway.app/actuator/health
```

**Resposta esperada:**
```json
{"status":"UP"}
```

### **2. Verificar Logs**

```bash
# Railway
railway logs

# Render
# Via dashboard: Logs tab
```

### **3. Testar Webhook**

```bash
curl -X POST https://sua-app.railway.app/webhook/whatsapp \
  -H "Content-Type: application/json" \
  -d '{
    "key": {
      "remoteJid": "5511999999999@s.whatsapp.net",
      "fromMe": false
    },
    "message": {
      "conversation": "Olá, estou com dor de cabeça"
    }
  }'
```

---

## 🐛 Troubleshooting

### **Erro: "Application failed to start"**

**Verificar:**
1. Variáveis de ambiente configuradas?
2. Database URL correto?
3. Flyway migrations executaram?

**Logs:**
```bash
railway logs --tail 100
```

### **Erro: "Connection refused to database"**

**Solução:**
```bash
# Verificar DATABASE_URL
echo $DATABASE_URL

# Deve ser algo como:
# postgresql://user:pass@host:5432/database
```

### **Erro: "OpenAI API key invalid"**

**Solução:**
```bash
# Verificar chave
echo $OPENAI_API_KEY

# Testar manualmente
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

---

## 🎯 Próximos Passos

1. 🗄️ [Estrutura do Banco](../03-database/01-database-structure.md)
2. 📡 [Configurar Webhook WhatsApp](../07-whatsapp/03-webhooks.md)
3. 🤖 [Configurar Spring AI](../05-ai/01-spring-ai-setup.md)

---

[⬅️ Anterior: Configuração](03-configuration.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Estrutura do Banco](../03-database/01-database-structure.md)

