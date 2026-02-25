# 2.3 Configuração

## ⚙️ Arquivos de Configuração

O projeto utiliza **Spring Profiles** para gerenciar configurações de diferentes ambientes.

---

## 📁 Estrutura de Configuração

```
src/main/resources/
├── application.properties          # Configuração base
├── application-dev.properties      # Desenvolvimento local
├── application-docker.properties   # Docker/Testes
└── application-prod.properties     # Produção
```

---

## 🔧 application.properties (Base)

Configurações compartilhadas por todos os ambientes:

```properties
# ============================================
# APPLICATION
# ============================================
spring.application.name=ai-health-agent
server.port=8080

# ============================================
# DATABASE
# ============================================
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=validate

# ============================================
# FLYWAY
# ============================================
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

# ============================================
# SPRING AI - OPENAI
# ============================================
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=500

# ============================================
# EVOLUTION API
# ============================================
evolution.api.url=${EVOLUTION_API_URL:http://localhost:8081}
evolution.api.key=${EVOLUTION_API_KEY}
evolution.instance.name=${EVOLUTION_INSTANCE_NAME:ai-health-agent}

# ============================================
# LOGGING
# ============================================
logging.level.root=INFO
logging.level.com.healthlink.ai_health_agent=DEBUG
logging.level.org.springframework.ai=DEBUG

# ============================================
# SWAGGER
# ============================================
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.path=/swagger-ui.html
```

---

## 🏠 application-dev.properties (Desenvolvimento)

Ative com: `-Dspring.profiles.active=dev`

```properties
# DATABASE - Local
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_health_agent_dev
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA - Modo desenvolvimento
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# SECURITY - Desabilitado para testes
spring.security.user.name=dev
spring.security.user.password=dev123

# LOGGING - Mais verboso
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

---

## 🐳 application-docker.properties (Docker)

Ative com: `-Dspring.profiles.active=docker`

```properties
# DATABASE - Docker Compose
spring.datasource.url=jdbc:postgresql://ai-health-postgres-test:5432/ai_health_agent
spring.datasource.username=postgres
spring.datasource.password=postgres

# FLYWAY
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# SECURITY
spring.security.user.name=${SPRING_SECURITY_USER_NAME:admin}
spring.security.user.password=${SPRING_SECURITY_USER_PASSWORD:admin123}
```

---

## 🚀 application-prod.properties (Produção)

Ative com: `-Dspring.profiles.active=prod`

```properties
# DATABASE - Railway/Render (via variáveis de ambiente)
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# Connection Pool - Otimizado
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=20000

# JPA - Produção
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# SECURITY - Obrigatório via env vars
spring.security.user.name=${ADMIN_USERNAME}
spring.security.user.password=${ADMIN_PASSWORD}

# LOGGING - Menos verboso
logging.level.root=WARN
logging.level.com.healthlink.ai_health_agent=INFO

# SWAGGER - Desabilitado em produção
springdoc.swagger-ui.enabled=false
```

---

## 🔑 Variáveis de Ambiente

### **Obrigatórias**

```bash
# OpenAI
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Evolution API
EVOLUTION_API_URL=http://localhost:8081
EVOLUTION_API_KEY=sua_api_key_aqui
EVOLUTION_INSTANCE_NAME=ai-health-agent

# Database (Produção)
DATABASE_URL=jdbc:postgresql://host:port/database
DB_USER=postgres
DB_PASSWORD=senha_forte_aqui

# Security (Produção)
ADMIN_USERNAME=admin
ADMIN_PASSWORD=senha_muito_forte_aqui
```

### **Opcionais**

```bash
# Server
SERVER_PORT=8080

# Logging
LOGGING_LEVEL_ROOT=INFO

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

---

## 🔐 Gerenciamento de Secrets

### **Desenvolvimento Local**

Use arquivo `.env.local` (NÃO commitar):

```bash
# .env.local
OPENAI_API_KEY=sk-proj-xxx
EVOLUTION_API_KEY=xxx
```

Carregue no IntelliJ:
1. Run → Edit Configurations
2. Environment Variables → Load from file
3. Selecione `.env.local`

### **Produção (Railway)**

Configure via dashboard:
1. Acesse seu projeto no Railway
2. Variables → New Variable
3. Adicione cada variável
4. Deploy automático após salvar

### **Produção (Docker)**

Use arquivo `.env` (NÃO commitar):

```bash
docker run --env-file .env ai-health-agent
```

---

## 🎛️ Configurações Avançadas

### **Connection Pool (HikariCP)**

```properties
# Máximo de conexões
spring.datasource.hikari.maximum-pool-size=20

# Mínimo de conexões idle
spring.datasource.hikari.minimum-idle=10

# Timeout de conexão (ms)
spring.datasource.hikari.connection-timeout=20000

# Tempo máximo de vida de uma conexão (ms)
spring.datasource.hikari.max-lifetime=1200000
```

### **OpenAI Customização**

```properties
# Modelo
spring.ai.openai.chat.options.model=gpt-4o-mini

# Temperatura (0.0 = determinístico, 1.0 = criativo)
spring.ai.openai.chat.options.temperature=0.7

# Máximo de tokens na resposta
spring.ai.openai.chat.options.max-tokens=500

# Timeout (ms)
spring.ai.openai.chat.options.timeout=30000
```

### **Flyway**

```properties
# Habilitar migrations
spring.flyway.enabled=true

# Criar baseline se banco já existe
spring.flyway.baseline-on-migrate=true

# Localização das migrations
spring.flyway.locations=classpath:db/migration

# Validar migrations ao iniciar
spring.flyway.validate-on-migrate=true
```

---

## 🔍 Verificando Configuração

### **Ver configuração ativa**

```bash
curl http://localhost:8080/actuator/env
```

### **Ver profile ativo**

```bash
curl http://localhost:8080/actuator/info
```

### **Logs de inicialização**

Procure por:
```
The following profiles are active: dev
```

---

## 🐛 Troubleshooting

### **Erro: "Could not resolve placeholder 'OPENAI_API_KEY'"**

**Solução:**
```bash
# Verificar se variável está definida
echo $OPENAI_API_KEY

# Definir temporariamente
export OPENAI_API_KEY=sk-proj-xxx

# Ou adicionar ao .env.local
```

### **Erro: "Failed to configure a DataSource"**

**Solução:**
```properties
# Verificar application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_health_agent
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### **Profile não está ativo**

**Solução:**
```bash
# Via linha de comando
java -jar app.jar --spring.profiles.active=dev

# Via variável de ambiente
export SPRING_PROFILES_ACTIVE=dev

# Via IntelliJ
Run → Edit Configurations → Active profiles: dev
```

---

## 🎯 Próximos Passos

1. 🚀 [Primeiro Deploy](04-first-deploy.md)
2. 🗄️ [Estrutura do Banco](../03-database/01-database-structure.md)
3. 📡 [Configurar Webhook](../07-whatsapp/03-webhooks.md)

---

[⬅️ Anterior: Instalação](02-installation.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Primeiro Deploy](04-first-deploy.md)

