# 13.3 Troubleshooting

## 🐛 Guia de Resolução de Problemas

---

## 🚀 Problemas de Inicialização

### **Erro: "Port 8080 already in use"**

**Sintoma:**
```
Web server failed to start. Port 8080 was already in use.
```

**Solução:**

```bash
# Linux/macOS - Encontrar processo
lsof -i :8080

# Windows - Encontrar processo
netstat -ano | findstr :8080

# Matar processo
kill -9 <PID>  # Linux/macOS
taskkill /PID <PID> /F  # Windows

# Ou mudar porta
java -jar app.jar --server.port=8081
```

---

### **Erro: "Failed to configure a DataSource"**

**Sintoma:**
```
Failed to configure a DataSource: 'url' attribute is not specified
```

**Solução:**

```properties
# Verificar application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_health_agent
spring.datasource.username=postgres
spring.datasource.password=postgres
```

**Ou via variáveis de ambiente:**
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/ai_health_agent
export DB_USER=postgres
export DB_PASSWORD=postgres
```

---

### **Erro: "Could not connect to database"**

**Sintoma:**
```
Connection to localhost:5432 refused
```

**Solução:**

```bash
# Verificar se PostgreSQL está rodando
docker ps | grep postgres

# Se não estiver, subir container
docker-compose -f docker-compose.test.yml up -d ai-health-postgres-test

# Testar conexão manualmente
psql -h localhost -p 5432 -U postgres -d ai_health_agent
```

---

## 🔑 Problemas de Configuração

### **Erro: "OpenAI API key not found"**

**Sintoma:**
```
Could not resolve placeholder 'OPENAI_API_KEY'
```

**Solução:**

```bash
# Verificar se variável está definida
echo $OPENAI_API_KEY

# Definir temporariamente
export OPENAI_API_KEY=sk-proj-xxxxxxxx

# Ou adicionar ao .env.local
echo "OPENAI_API_KEY=sk-proj-xxx" >> .env.local
```

---

### **Erro: "Invalid OpenAI API key"**

**Sintoma:**
```
401 Unauthorized - Incorrect API key provided
```

**Solução:**

```bash
# Testar chave manualmente
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"

# Se falhar, gerar nova chave em:
# https://platform.openai.com/api-keys
```

---

### **Erro: "Profile 'prod' not found"**

**Sintoma:**
```
The following profiles are active: prod
Could not find application-prod.properties
```

**Solução:**

```bash
# Criar arquivo
touch src/main/resources/application-prod.properties

# Ou usar profile existente
java -jar app.jar --spring.profiles.active=dev
```

---

## 🗄️ Problemas de Banco de Dados

### **Erro: "Flyway migration failed"**

**Sintoma:**
```
Migration V1__create_base_tables.sql failed
SQL State: 42P07
Detail: relation "accounts" already exists
```

**Solução:**

```bash
# Opção 1: Baseline (se banco já existe)
./mvnw flyway:baseline

# Opção 2: Limpar e recriar (PERDE DADOS!)
docker-compose -f docker-compose.test.yml down -v
docker-compose -f docker-compose.test.yml up -d
./mvnw spring-boot:run
```

---

### **Erro: "Table 'shedlock' doesn't exist"**

**Sintoma:**
```
Table "shedlock" doesn't exist
```

**Solução:**

```sql
-- Criar tabela manualmente
CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
```

**Ou executar migration V1 novamente.**

---

### **Erro: "JPQL DATE() function not supported"**

**Sintoma:**
```
org.hibernate.query.SemanticException: The DATE function is not supported
```

**Solução:**

```java
// ❌ Errado
@Query("SELECT h FROM HealthLog h WHERE DATE(h.createdAt) = :date")

// ✅ Correto
@Query("SELECT h FROM HealthLog h WHERE CAST(h.createdAt AS date) = :date")
```

---

## 🤖 Problemas com IA

### **Erro: "Rate limit exceeded"**

**Sintoma:**
```
429 Too Many Requests - Rate limit reached for requests
```

**Solução:**

```java
// Adicionar retry logic
@Retryable(
    value = {RateLimitException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 2000)
)
public String chat(String message) {
    return openAiClient.chat(message);
}
```

---

### **Erro: "Context length exceeded"**

**Sintoma:**
```
This model's maximum context length is 128000 tokens
```

**Solução:**

```java
// Limitar janela de contexto
@Query("SELECT c FROM ChatMessage c WHERE c.patient.id = :patientId ORDER BY c.timestamp DESC")
List<ChatMessage> findRecentMessages(@Param("patientId") UUID patientId, Pageable pageable);

// Usar apenas últimas 10 mensagens
Pageable limit = PageRequest.of(0, 10);
List<ChatMessage> context = repository.findRecentMessages(patientId, limit);
```

---

## 💬 Problemas com WhatsApp

### **Erro: "Evolution API connection refused"**

**Sintoma:**
```
Connection refused: http://localhost:8081
```

**Solução:**

```bash
# Verificar se Evolution API está rodando
curl http://localhost:8081/instance/connectionState/ai-health-agent

# Se não estiver, subir container
docker run -d \
  -p 8081:8080 \
  --name evolution-api \
  atendai/evolution-api:latest
```

---

### **Erro: "Webhook not receiving messages"**

**Sintoma:**
Mensagens enviadas no WhatsApp não chegam na aplicação.

**Solução:**

```bash
# 1. Verificar se webhook está configurado
curl http://localhost:8081/webhook/find/ai-health-agent

# 2. Reconfigurar webhook
curl -X POST http://localhost:8081/webhook/set/ai-health-agent \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://sua-app.railway.app/webhook/whatsapp",
    "events": ["messages.upsert"]
  }'

# 3. Testar manualmente
curl -X POST http://localhost:8080/webhook/whatsapp \
  -H "Content-Type: application/json" \
  -d '{
    "key": {"remoteJid": "5511999999999@s.whatsapp.net"},
    "message": {"conversation": "teste"}
  }'
```

---

## 🔐 Problemas de Segurança

### **Erro: "401 Unauthorized"**

**Sintoma:**
```
Full authentication is required to access this resource
```

**Solução:**

```bash
# Usar credenciais corretas
curl -u admin:admin123 http://localhost:8080/api/patients

# Ou desabilitar segurança (APENAS DEV!)
# application.properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

---

## 🚀 Problemas de Deploy

### **Erro: "Application crashed on Railway"**

**Sintoma:**
Aplicação sobe localmente mas falha no Railway.

**Solução:**

```bash
# 1. Verificar logs
railway logs --tail 100

# 2. Verificar variáveis de ambiente
railway variables

# 3. Verificar se DATABASE_URL está correto
# Deve ser: postgresql://user:pass@host:port/db

# 4. Verificar se porta está correta
# Railway usa $PORT automaticamente
server.port=${PORT:8080}
```

---

### **Erro: "Out of memory"**

**Sintoma:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solução:**

```bash
# Aumentar heap size
java -Xmx512m -jar app.jar

# Ou via variável de ambiente (Railway)
JAVA_OPTS=-Xmx512m
```

---

## 🎯 Próximos Passos

1. 📝 [Changelog](04-changelog.md)
2. ❓ [FAQ](02-faq.md)
3. 📚 [Glossário](01-glossary.md)

---

[⬅️ Anterior: FAQ](02-faq.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Changelog](04-changelog.md)

