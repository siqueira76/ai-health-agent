# 🚀 Guia de Configuração - AI Health Agent

## ✅ Etapas Concluídas

### 1. Spring AI Adicionado ao `pom.xml`
- ✅ Dependência `spring-ai-openai-spring-boot-starter` adicionada
- ✅ Repositório Spring Milestones configurado
- ✅ BOM do Spring AI (versão 1.0.0-M5) configurado

### 2. Arquivos de Configuração Criados
- ✅ `application.properties` - Configuração base
- ✅ `application-dev.properties` - Perfil de desenvolvimento
- ✅ `application-prod.properties` - Perfil de produção
- ✅ `.env.example` - Template de variáveis de ambiente

---

## 📋 Próximos Passos (VOCÊ DEVE EXECUTAR)

### Passo 1: Atualizar Dependências Maven

Abra o terminal no diretório do projeto e execute:

```bash
mvn clean install -DskipTests
```

Ou se estiver usando o Maven Wrapper:

```bash
./mvnw clean install -DskipTests
```

Ou no Windows:

```cmd
mvnw.cmd clean install -DskipTests
```

### Passo 2: Configurar Banco de Dados PostgreSQL

#### Opção A: PostgreSQL Local

1. Instale o PostgreSQL (se ainda não tiver)
2. Crie o banco de dados:

```sql
CREATE DATABASE ai_health_agent_dev;
```

3. Atualize o arquivo `src/main/resources/application-dev.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_health_agent_dev
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
```

#### Opção B: Supabase (Recomendado)

1. Acesse [supabase.com](https://supabase.com)
2. Crie um novo projeto
3. Copie a **Connection String** (formato JDBC)
4. Atualize o `application-dev.properties`:

```properties
spring.datasource.url=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=sua_senha_supabase
```

### Passo 3: Configurar OpenAI API Key

1. Obtenha sua chave em [platform.openai.com](https://platform.openai.com/api-keys)
2. Adicione ao `application-dev.properties`:

```properties
spring.ai.openai.api-key=sk-proj-xxxxxxxxxxxxxxxxxxxxxxxx
```

**OU** configure como variável de ambiente:

```bash
export OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxxxx
```

No Windows (PowerShell):

```powershell
$env:OPENAI_API_KEY="sk-proj-xxxxxxxxxxxxxxxxxxxxxxxx"
```

### Passo 4: Testar a Aplicação

Execute a aplicação com o perfil de desenvolvimento:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Ou:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A aplicação deve iniciar em: **http://localhost:8080**

---

## 🔍 Verificação de Sucesso

Se tudo estiver correto, você verá no console:

```
✓ Tomcat started on port 8080
✓ Started AiHealthAgentApplication in X.XXX seconds
✓ No errors about DataSource or OpenAI
```

---

## ⚠️ Troubleshooting

### Erro: "Failed to configure a DataSource"
- Verifique se o PostgreSQL está rodando
- Confirme as credenciais no `application-dev.properties`
- Teste a conexão com um cliente SQL (DBeaver, pgAdmin)

### Erro: "OpenAI API Key not found"
- Verifique se a chave está correta no arquivo de configuração
- Ou configure a variável de ambiente `OPENAI_API_KEY`

### Erro: "Port 8080 already in use"
- Altere a porta no `application.properties`:
  ```properties
  server.port=8081
  ```

---

## 📦 Estrutura de Configuração

```
src/main/resources/
├── application.properties          # Configuração base
├── application-dev.properties      # Desenvolvimento (use este!)
└── application-prod.properties     # Produção (Railway/Supabase)
```

---

## 🎯 Próxima Etapa

Após a aplicação iniciar com sucesso, estaremos prontos para:

1. ✅ Criar as entidades JPA (Account, Patient, HealthLog)
2. ✅ Implementar o Webhook para Evolution API
3. ✅ Configurar o Function Calling do Spring AI
4. ✅ Implementar a lógica multi-tenant

**Aguardando confirmação de que a aplicação iniciou com sucesso!** 🚀

