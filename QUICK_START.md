# ⚡ Quick Start - AI Health Agent

## 🚀 Início Rápido (3 passos)

### **1. Configurar Credenciais**

```bash
# Copiar template
cp .env.example .env

# Editar .env e adicionar sua chave OpenAI
# OPENAI_API_KEY=sk-your-real-key-here
```

---

### **2. Subir Ambiente Completo**

**Linux/Mac:**
```bash
chmod +x start-local-env.sh
./start-local-env.sh
```

**Windows (PowerShell):**
```powershell
.\start-local-env.ps1
```

**Ou manualmente:**
```bash
# Subir Docker
docker-compose -f docker-compose.test.yml up -d

# Compilar
./mvnw clean install

# Rodar
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

---

### **3. Popular Dados de Teste**

**Linux/Mac:**
```bash
chmod +x scripts/seed-test-data.sh
./scripts/seed-test-data.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\seed-test-data.ps1
```

---

## 🔗 Acessar Aplicação

| Serviço | URL | Credenciais |
|---------|-----|-------------|
| **Swagger UI** | http://localhost:8080/swagger-ui.html | admin / admin123 |
| **PostgreSQL** | localhost:5432 | postgres / postgres |
| **Evolution API** | http://localhost:8081 | - |
| **PgAdmin** | http://localhost:5050 | admin@aihealth.com / admin123 |

---

## 📚 Documentação Completa

- **[DOCKER_TESTE_LOCAL.md](DOCKER_TESTE_LOCAL.md)** - Guia completo de Docker
- **[SWAGGER_GUIA_TESTE.md](SWAGGER_GUIA_TESTE.md)** - Guia de testes via Swagger
- **[PROATIVIDADE_GUIA_USO.md](PROATIVIDADE_GUIA_USO.md)** - Mensagens proativas
- **[RESUMO_IMPLEMENTACAO_COMPLETA.md](RESUMO_IMPLEMENTACAO_COMPLETA.md)** - Visão geral

---

## 🧪 Testar Rapidamente

```bash
# Health check
curl http://localhost:8080/actuator/health

# Listar agendamentos
curl -u admin:admin123 http://localhost:8080/api/checkin-schedules

# Ver estatísticas
curl -u admin:admin123 http://localhost:8080/api/checkin-schedules/stats/rate-limit
```

---

## 🛑 Parar Ambiente

```bash
# Parar containers
docker-compose -f docker-compose.test.yml down

# Parar e limpar TUDO (dados serão perdidos)
docker-compose -f docker-compose.test.yml down -v
```

---

## 🆘 Problemas?

Veja: **[DOCKER_TESTE_LOCAL.md#troubleshooting](DOCKER_TESTE_LOCAL.md#troubleshooting)**

---

**🎉 Pronto para usar!**

