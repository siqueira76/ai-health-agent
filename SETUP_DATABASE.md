# 🗄️ Setup do Banco de Dados

## 📋 Visão Geral

Este projeto usa **PostgreSQL 16** com **Flyway** para migrations automáticas.

---

## 🐳 Opção 1: Docker (Recomendado)

### **Ambiente de Teste**

```bash
# Subir PostgreSQL na porta 5438
docker-compose -f docker-compose.test.yml up -d ai-health-postgres-test

# Verificar se está rodando
docker ps | grep ai-health-postgres-test
```

### **Ambiente de Desenvolvimento**

```bash
# Subir PostgreSQL na porta 5432
docker-compose up -d postgres

# Verificar se está rodando
docker ps | grep postgres
```

---

## 💻 Opção 2: PostgreSQL Local

### **1. Instalar PostgreSQL 16**

- **Windows**: https://www.postgresql.org/download/windows/
- **macOS**: `brew install postgresql@16`
- **Linux**: `sudo apt install postgresql-16`

### **2. Criar Banco de Dados**

```bash
# Conectar ao PostgreSQL
psql -U postgres

# Criar banco
CREATE DATABASE ai_health_agent;

# Sair
\q
```

### **3. Configurar application.properties**

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_health_agent
spring.datasource.username=postgres
spring.datasource.password=sua_senha_aqui
```

---

## 🔄 Migrations (Flyway)

### **Como Funciona**

O Flyway executa automaticamente as migrations na **primeira vez** que a aplicação sobe.

**Arquivos de Migration:**
```
src/main/resources/db/migration/
├── V1__create_base_tables.sql      # Tabelas base (accounts, patients, etc)
└── V5__create_checkin_tables.sql   # Tabelas de check-in proativo
```

### **Configuração**

```properties
# application.properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
```

### **Comandos Úteis**

```bash
# Verificar status das migrations
mvn flyway:info

# Executar migrations manualmente
mvn flyway:migrate

# Limpar banco (CUIDADO: apaga tudo!)
mvn flyway:clean
```

---

## 🚨 Troubleshooting

### **Erro: "Port 5432 already in use"**

**Solução 1:** Parar o container que está usando a porta
```bash
docker ps
docker stop <container_id>
```

**Solução 2:** Usar porta diferente no docker-compose.test.yml (5438)

---

### **Erro: "Flyway migration failed"**

**Solução:** Limpar o banco e rodar novamente
```bash
# Via Docker
docker exec -i ai-health-postgres-test psql -U postgres -d ai_health_agent -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# Via psql local
psql -U postgres -d ai_health_agent -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

---

### **Erro: "Table already exists"**

**Causa:** Hibernate criou as tabelas antes do Flyway

**Solução:** Desabilitar Hibernate DDL e usar apenas Flyway
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

---

## 🌍 Ambientes

### **Development (Local)**
```properties
spring.profiles.active=dev
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_health_agent_dev
```

### **Test (Docker)**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5438/ai_health_agent
```

### **Production (Railway/Supabase)**
```properties
spring.profiles.active=prod
spring.datasource.url=${DATABASE_URL}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

---

## ✅ Checklist de Setup

- [ ] PostgreSQL instalado ou Docker rodando
- [ ] Banco de dados `ai_health_agent` criado
- [ ] `application.properties` configurado com credenciais corretas
- [ ] Flyway habilitado (`spring.flyway.enabled=true`)
- [ ] Migrations em `src/main/resources/db/migration/`
- [ ] Aplicação rodando sem erros

---

## 📊 Estrutura do Banco

```
accounts (tenants)
├── patients
│   ├── health_logs
│   ├── chat_messages
│   ├── alerts
│   ├── checkin_schedules
│   └── checkin_executions
└── shedlock (distributed lock)
```

---

**🚀 Pronto! Agora você pode rodar a aplicação em qualquer ambiente!**

