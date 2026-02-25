# 2.2 Instalação

## 📥 Clonando o Repositório

### **1. Clone o projeto**

```bash
git clone https://github.com/seu-usuario/ai-health-agent.git
cd ai-health-agent
```

### **2. Verifique a estrutura**

```bash
tree -L 2
```

**Estrutura esperada:**
```
ai-health-agent/
├── src/
│   ├── main/
│   └── test/
├── docs/
├── docker-compose.test.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## 🗄️ Configurando o Banco de Dados

### **Opção 1: Docker (Recomendado)**

#### **1. Subir PostgreSQL**

```bash
docker-compose -f docker-compose.test.yml up -d ai-health-postgres-test
```

#### **2. Verificar se está rodando**

```bash
docker ps | grep postgres
```

**Saída esperada:**
```
CONTAINER ID   IMAGE         PORTS                    NAMES
abc123def456   postgres:16   0.0.0.0:5433->5432/tcp   ai-health-postgres-test
```

#### **3. Testar conexão**

```bash
docker exec -it ai-health-postgres-test psql -U postgres -d ai_health_agent
```

**Comandos úteis no psql:**
```sql
-- Listar tabelas
\dt

-- Ver estrutura de uma tabela
\d accounts

-- Sair
\q
```

---

### **Opção 2: PostgreSQL Local**

#### **1. Criar banco de dados**

```bash
# Conectar ao PostgreSQL
psql -U postgres

# Criar banco
CREATE DATABASE ai_health_agent;

# Criar usuário (opcional)
CREATE USER ai_health_user WITH PASSWORD 'sua_senha_forte';
GRANT ALL PRIVILEGES ON DATABASE ai_health_agent TO ai_health_user;

# Sair
\q
```

#### **2. Atualizar configuração**

Edite `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_health_agent
spring.datasource.username=postgres
spring.datasource.password=sua_senha
```

---

## 🔑 Configurando Variáveis de Ambiente

### **1. Criar arquivo `.env.local`**

```bash
# Na raiz do projeto
touch .env.local
```

### **2. Adicionar variáveis**

Edite `.env.local`:

```bash
# OpenAI
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Evolution API
EVOLUTION_API_URL=http://localhost:8081
EVOLUTION_API_KEY=sua_api_key_aqui
EVOLUTION_INSTANCE_NAME=ai-health-agent

# Database (se não usar Docker)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ai_health_agent
DB_USER=postgres
DB_PASSWORD=postgres

# Security (desenvolvimento)
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=admin123
```

### **3. Adicionar ao .gitignore**

```bash
echo ".env.local" >> .gitignore
```

**⚠️ IMPORTANTE:** Nunca commite o arquivo `.env.local`!

---

## 📦 Instalando Dependências

### **Usando Maven Wrapper (Recomendado)**

```bash
# Linux/macOS
./mvnw clean install

# Windows
mvnw.cmd clean install
```

### **Usando Maven Instalado**

```bash
mvn clean install
```

**O que acontece:**
1. ✅ Baixa todas as dependências do `pom.xml`
2. ✅ Compila o código
3. ✅ Executa testes unitários
4. ✅ Gera o arquivo `.jar` em `target/`

**Tempo estimado:** 2-5 minutos (primeira vez)

---

## 🚀 Executando a Aplicação

### **Opção 1: Via Maven**

```bash
# Linux/macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

### **Opção 2: Via JAR**

```bash
# Compilar
./mvnw clean package -DskipTests

# Executar
java -jar target/ai-health-agent-1.0.0.jar
```

### **Opção 3: Via IDE (IntelliJ)**

1. Abra o projeto no IntelliJ
2. Localize `AiHealthAgentApplication.java`
3. Clique com botão direito → **Run 'AiHealthAgentApplication'**

---

## ✅ Verificando a Instalação

### **1. Verificar logs de inicialização**

Procure por estas mensagens no console:

```
✅ Started AiHealthAgentApplication in X.XXX seconds
✅ Tomcat started on port(s): 8080 (http)
✅ Flyway migration completed successfully
```

### **2. Testar Health Check**

```bash
curl http://localhost:8080/actuator/health
```

**Resposta esperada:**
```json
{
  "status": "UP"
}
```

### **3. Acessar Swagger UI**

Abra no navegador:
```
http://localhost:8080/swagger-ui.html
```

Você deve ver a documentação interativa da API.

### **4. Verificar tabelas no banco**

```bash
docker exec -it ai-health-postgres-test psql -U postgres -d ai_health_agent -c "\dt"
```

**Tabelas esperadas:**
```
 Schema |         Name          | Type  |  Owner
--------+-----------------------+-------+----------
 public | accounts              | table | postgres
 public | alerts                | table | postgres
 public | chat_messages         | table | postgres
 public | checkin_executions    | table | postgres
 public | checkin_schedules     | table | postgres
 public | flyway_schema_history | table | postgres
 public | health_logs           | table | postgres
 public | patients              | table | postgres
 public | shedlock              | table | postgres
```

---

## 🐛 Troubleshooting

### **Erro: "Port 8080 already in use"**

```bash
# Encontrar processo usando a porta
# Linux/macOS
lsof -i :8080

# Windows
netstat -ano | findstr :8080

# Matar o processo
kill -9 <PID>
```

### **Erro: "Could not connect to database"**

```bash
# Verificar se PostgreSQL está rodando
docker ps | grep postgres

# Ver logs do container
docker logs ai-health-postgres-test

# Reiniciar container
docker restart ai-health-postgres-test
```

### **Erro: "OpenAI API key not found"**

Verifique se a variável de ambiente está configurada:

```bash
# Linux/macOS
echo $OPENAI_API_KEY

# Windows
echo %OPENAI_API_KEY%
```

Se vazio, configure no `.env.local` ou exporte:

```bash
export OPENAI_API_KEY=sk-proj-xxxxxxxx
```

### **Erro: "Flyway migration failed"**

```bash
# Resetar banco de dados
docker-compose -f docker-compose.test.yml down -v
docker-compose -f docker-compose.test.yml up -d

# Executar migrations manualmente
./mvnw flyway:migrate
```

---

## 🎯 Próximos Passos

Agora que a aplicação está instalada e rodando:

1. ⚙️ [Configuração Avançada](03-configuration.md)
2. 🚀 [Primeiro Deploy](04-first-deploy.md)
3. 📡 [Configurar Webhook WhatsApp](../07-whatsapp/03-webhooks.md)

---

[⬅️ Anterior: Pré-requisitos](01-prerequisites.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Configuração](03-configuration.md)

