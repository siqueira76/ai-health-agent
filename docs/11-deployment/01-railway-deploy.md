# 11.1 Deploy no Railway

## 🚀 Deploy Rápido e Fácil

Railway é uma plataforma de deploy moderna que facilita o deploy de aplicações Spring Boot.

---

## 🎯 Por que Railway?

- ✅ Deploy automático via Git
- ✅ PostgreSQL integrado
- ✅ HTTPS automático
- ✅ Logs em tempo real
- ✅ Variáveis de ambiente fáceis
- ✅ $5 grátis/mês (suficiente para testes)

---

## 📋 Pré-requisitos

1. Conta no Railway: https://railway.app
2. Repositório Git (GitHub, GitLab, Bitbucket)
3. Código commitado

---

## 🚀 Passo a Passo

### **1. Criar Projeto no Railway:**

1. Acesse https://railway.app
2. Clique em "New Project"
3. Selecione "Deploy from GitHub repo"
4. Autorize Railway a acessar seu GitHub
5. Selecione o repositório `ai-health-agent`

---

### **2. Adicionar PostgreSQL:**

1. No projeto, clique em "+ New"
2. Selecione "Database" → "PostgreSQL"
3. Railway cria automaticamente o banco

---

### **3. Configurar Variáveis de Ambiente:**

No painel do serviço Spring Boot, vá em "Variables" e adicione:

```bash
# Database (Railway fornece automaticamente)
DATABASE_URL=${{Postgres.DATABASE_URL}}
SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}

# OpenAI
OPENAI_API_KEY=sk-proj-your-key-here

# Evolution API
EVOLUTION_API_URL=https://your-evolution-api.com
EVOLUTION_API_KEY=your-evolution-key
EVOLUTION_INSTANCE_NAME=ai-health-agent
EVOLUTION_WEBHOOK_KEY=your-webhook-secret

# Spring Security
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your-secure-password-here

# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# Port (Railway define automaticamente)
PORT=${{PORT}}
```

---

### **4. Configurar Build:**

Railway detecta automaticamente Maven. Se necessário, customize:

**Settings → Build:**
```bash
Build Command: mvn clean package -DskipTests
Start Command: java -jar target/ai-health-agent-0.0.1-SNAPSHOT.jar
```

---

### **5. Deploy:**

1. Clique em "Deploy"
2. Railway faz build e deploy automaticamente
3. Aguarde ~3-5 minutos

---

## 🔧 Configuração Adicional

### **application-prod.properties:**

```properties
# Server
server.port=${PORT:8080}

# Database
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Logging
logging.level.root=INFO
logging.level.com.healthlink=DEBUG

# OpenAI
spring.ai.openai.api-key=${OPENAI_API_KEY}

# Evolution API
evolution.api.url=${EVOLUTION_API_URL}
evolution.api.key=${EVOLUTION_API_KEY}
evolution.instance.name=${EVOLUTION_INSTANCE_NAME}
```

---

## 🌐 Obter URL Pública

### **1. Gerar Domínio:**

1. No serviço, vá em "Settings"
2. Clique em "Generate Domain"
3. Railway gera URL: `https://ai-health-agent-production.up.railway.app`

### **2. Domínio Customizado (Opcional):**

1. Compre domínio (Namecheap, GoDaddy)
2. Em "Settings" → "Custom Domain"
3. Adicione seu domínio
4. Configure DNS conforme instruções

---

## 🔗 Configurar Webhook

### **Atualizar Evolution API:**

```bash
curl -X POST "https://your-evolution-api.com/webhook/set/ai-health-agent" \
  -H "apikey: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://ai-health-agent-production.up.railway.app/webhook/whatsapp",
    "webhook_by_events": true,
    "events": ["MESSAGES_UPSERT"]
  }'
```

---

## 📊 Monitoramento

### **Logs em Tempo Real:**

1. No serviço, clique em "Deployments"
2. Selecione o deployment ativo
3. Veja logs em tempo real

### **Métricas:**

1. Vá em "Metrics"
2. Veja CPU, memória, rede

---

## 🐛 Troubleshooting

### **Problema: Build falha**

```bash
# Verificar logs de build
# Comum: falta de memória

# Solução: Adicionar variável
MAVEN_OPTS=-Xmx512m
```

### **Problema: Aplicação não inicia**

```bash
# Verificar logs
# Comum: variáveis de ambiente faltando

# Solução: Verificar todas as variáveis necessárias
```

### **Problema: Banco de dados não conecta**

```bash
# Verificar variáveis
echo $SPRING_DATASOURCE_URL

# Solução: Usar variáveis do Railway
DATABASE_URL=${{Postgres.DATABASE_URL}}
```

---

## 💰 Custos

### **Plano Hobby (Grátis):**
- $5 de crédito/mês
- Suficiente para ~500 horas/mês
- Ideal para desenvolvimento/testes

### **Plano Pro ($20/mês):**
- $20 de crédito incluído
- Recursos adicionais
- Ideal para produção

### **Estimativa de Uso:**

| Recurso | Custo/hora | Custo/mês (24/7) |
|---------|------------|------------------|
| App (512MB RAM) | ~$0.007 | ~$5 |
| PostgreSQL | ~$0.01 | ~$7 |
| **Total** | ~$0.017 | ~$12 |

---

## 🔄 CI/CD Automático

### **Deploy Automático:**

Railway faz deploy automático quando você:
1. Faz push para branch principal
2. Merge de Pull Request

### **Desabilitar Auto-Deploy:**

Settings → "Auto Deploy" → Desabilitar

---

## 🎯 Próximos Passos

1. 🐳 [Deploy Docker](02-docker-deploy.md)
2. ☁️ [Deploy Render](03-render-deploy.md)
3. 📚 [Referências](../13-references/01-glossary.md)

---

[⬅️ Anterior: Testes de Integração](../10-testing/02-integration-tests.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Deploy Docker](02-docker-deploy.md)

