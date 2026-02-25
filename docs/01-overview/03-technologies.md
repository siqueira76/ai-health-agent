# 1.3 Tecnologias Utilizadas

## 🛠️ Stack Tecnológico Completo

---

## ☕ Backend

### **Spring Boot 3.3.0**
- **Descrição:** Framework Java para aplicações enterprise
- **Por que escolhemos:**
  - ✅ Ecossistema maduro e robusto
  - ✅ Excelente suporte a microserviços
  - ✅ Auto-configuração e convenções
  - ✅ Grande comunidade e documentação
- **Uso no projeto:**
  - REST API
  - Dependency Injection
  - Configuração centralizada
  - Profiles (dev, prod, docker)

### **Spring AI 1.0.0-M5**
- **Descrição:** Framework para integração com LLMs
- **Por que escolhemos:**
  - ✅ Abstração de alto nível para IA
  - ✅ Suporte nativo a Function Calling
  - ✅ Integração perfeita com Spring Boot
  - ✅ Suporte a múltiplos providers (OpenAI, Azure, etc)
- **Uso no projeto:**
  - Conversação com GPT-4o-mini
  - Function Calling para extração de dados
  - Gerenciamento de contexto (memória)
  - Prompts dinâmicos

### **Spring Data JPA**
- **Descrição:** Abstração para acesso a dados
- **Por que escolhemos:**
  - ✅ Reduz boilerplate de SQL
  - ✅ Queries type-safe
  - ✅ Suporte a relacionamentos complexos
  - ✅ Paginação e ordenação automáticas
- **Uso no projeto:**
  - Repositories para todas as entidades
  - Queries customizadas com JPQL
  - Relacionamentos JPA (OneToMany, ManyToOne)

### **Hibernate 6.5.2**
- **Descrição:** ORM (Object-Relational Mapping)
- **Por que escolhemos:**
  - ✅ Padrão de mercado
  - ✅ Performance otimizada
  - ✅ Lazy loading e caching
- **Uso no projeto:**
  - Mapeamento de entidades
  - Geração de DDL (desenvolvimento)
  - Validação de schema

---

## 🗄️ Banco de Dados

### **PostgreSQL 16**
- **Descrição:** Banco de dados relacional open-source
- **Por que escolhemos:**
  - ✅ ACID compliant (transações seguras)
  - ✅ Suporte a JSON (flexibilidade)
  - ✅ Excelente performance
  - ✅ Gratuito e open-source
  - ✅ Suporte nativo em Railway/Render
- **Uso no projeto:**
  - Armazenamento de todos os dados
  - Índices para performance
  - Constraints para integridade
  - Triggers (futuro)

### **Flyway 10.x**
- **Descrição:** Ferramenta de versionamento de banco
- **Por que escolhemos:**
  - ✅ Migrations versionadas
  - ✅ Rollback seguro
  - ✅ Auditoria de mudanças
  - ✅ Integração com Spring Boot
- **Uso no projeto:**
  - Criação de tabelas
  - Alterações de schema
  - Dados iniciais (seeds)

---

## 🤖 Inteligência Artificial

### **OpenAI GPT-4o-mini**
- **Descrição:** Large Language Model (LLM)
- **Por que escolhemos:**
  - ✅ Melhor custo-benefício
  - ✅ Baixa latência (~500ms)
  - ✅ Suporte a Function Calling
  - ✅ Contexto de 128k tokens
- **Uso no projeto:**
  - Conversação natural com pacientes
  - Extração de dados de saúde
  - Geração de insights
  - Personalização de mensagens

**Configuração:**
```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=500
```

---

## 💬 Comunicação

### **Evolution API**
- **Descrição:** Gateway para WhatsApp Business API
- **Por que escolhemos:**
  - ✅ Open-source e gratuito
  - ✅ Fácil deploy (Docker)
  - ✅ Webhooks em tempo real
  - ✅ Suporte a múltiplas instâncias
- **Uso no projeto:**
  - Recebimento de mensagens
  - Envio de mensagens
  - Gerenciamento de sessões
  - Webhooks de eventos

**Endpoints usados:**
```
POST /message/sendText/{instance}
GET /instance/connectionState/{instance}
POST /webhook/set/{instance}
```

---

## 🔧 Ferramentas de Desenvolvimento

### **Lombok**
- **Descrição:** Reduz boilerplate em Java
- **Uso:**
  - `@Data` - Getters/Setters automáticos
  - `@Builder` - Builder pattern
  - `@Slf4j` - Logger automático
  - `@RequiredArgsConstructor` - Injeção de dependências

### **Swagger/OpenAPI 3**
- **Descrição:** Documentação interativa de API
- **Uso:**
  - Documentação automática de endpoints
  - Testes interativos
  - Geração de clientes (futuro)

**Acesso:** `http://localhost:8080/swagger-ui.html`

### **Spring Boot DevTools**
- **Descrição:** Ferramentas de desenvolvimento
- **Uso:**
  - Hot reload de código
  - LiveReload do navegador
  - Configurações de desenvolvimento

---

## ⏰ Agendamento e Jobs

### **Spring Scheduler**
- **Descrição:** Agendamento de tarefas
- **Uso:**
  - Check-ins proativos (a cada minuto)
  - Limpeza de dados antigos (diário)
  - Geração de relatórios (semanal)

### **ShedLock**
- **Descrição:** Lock distribuído para jobs
- **Por que escolhemos:**
  - ✅ Evita execução duplicada em múltiplas instâncias
  - ✅ Usa banco de dados (sem Redis necessário)
  - ✅ Configuração simples
- **Uso:**
  - Lock em jobs agendados
  - Garantia de execução única

---

## 🐳 DevOps e Deploy

### **Docker**
- **Descrição:** Containerização
- **Uso:**
  - PostgreSQL local (desenvolvimento)
  - Evolution API
  - Build da aplicação (futuro)

### **Railway**
- **Descrição:** Plataforma de deploy
- **Por que escolhemos:**
  - ✅ Deploy automático via Git
  - ✅ PostgreSQL gerenciado
  - ✅ Variáveis de ambiente
  - ✅ Logs centralizados
  - ✅ Free tier generoso

### **Maven**
- **Descrição:** Gerenciador de dependências
- **Uso:**
  - Build da aplicação
  - Gerenciamento de dependências
  - Execução de testes
  - Profiles de build

---

## 📊 Monitoramento (Futuro)

### **Spring Boot Actuator**
- Health checks
- Métricas de performance
- Endpoints de monitoramento

### **Prometheus + Grafana**
- Coleta de métricas
- Dashboards de monitoramento
- Alertas de performance

### **Sentry**
- Rastreamento de erros
- Stack traces detalhados
- Notificações de bugs

---

## 🔐 Segurança

### **Spring Security**
- Autenticação e autorização
- Proteção CSRF
- CORS configurável

### **BCrypt**
- Hash de senhas
- Salt automático
- Resistente a rainbow tables

---

## 📚 Dependências Principais

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.3.0</version>
</dependency>

<!-- Spring AI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M5</version>
</dependency>

<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>

<!-- Flyway -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<!-- ShedLock -->
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-spring</artifactId>
    <version>5.10.0</version>
</dependency>
```

---

## 🎯 Próximos Passos

1. 💼 Entenda os [Modelos de Negócio](04-business-models.md)
2. 🚀 Comece com o [Getting Started](../02-getting-started/01-prerequisites.md)
3. 🗄️ Explore a [Estrutura do Banco](../03-database/01-database-structure.md)

---

[⬅️ Anterior: Arquitetura](02-architecture.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Modelos de Negócio](04-business-models.md)

