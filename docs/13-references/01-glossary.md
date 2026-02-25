# 13.1 Glossário

## 📚 Termos Técnicos

### **A**

**Account**  
Tenant no sistema multi-tenant. Representa um cliente B2C (paciente individual) ou B2B (profissional de saúde).

**Alert**  
Notificação automática gerada quando padrões críticos são detectados nos dados de saúde do paciente.

**API (Application Programming Interface)**  
Interface que permite comunicação entre diferentes sistemas de software.

---

### **B**

**B2B (Business-to-Business)**  
Modelo de negócio onde profissionais de saúde monitoram múltiplos pacientes.

**B2C (Business-to-Consumer)**  
Modelo de negócio onde pacientes individuais usam o sistema para auto-monitoramento.

**Baseline**  
Ponto de partida para migrations do Flyway quando o banco já existe.

---

### **C**

**Cascade**  
Comportamento de propagação de operações (delete, update) entre entidades relacionadas.

**ChatMessage**  
Mensagem trocada entre paciente e IA, armazenada para contexto.

**Check-in Proativo**  
Mensagem automática enviada pela IA para coletar dados de saúde do paciente.

**Context Window**  
Janela de contexto das últimas N mensagens usadas pela IA para manter coerência na conversa.

---

### **D**

**DTO (Data Transfer Object)**  
Objeto usado para transferir dados entre camadas da aplicação.

**DDD (Domain-Driven Design)**  
Abordagem de design de software focada no domínio do negócio.

---

### **E**

**Entity**  
Classe JPA que representa uma tabela no banco de dados.

**Evolution API**  
Gateway open-source para integração com WhatsApp Business API.

---

### **F**

**Flyway**  
Ferramenta de versionamento e migração de banco de dados.

**Function Calling**  
Recurso da OpenAI que permite à IA chamar funções estruturadas para extrair dados.

---

### **G**

**GPT-4o-mini**  
Modelo de linguagem da OpenAI usado para conversação com pacientes.

---

### **H**

**HealthLog**  
Registro de dados de saúde extraídos das conversas (dor, humor, sono, etc).

**HikariCP**  
Pool de conexões de banco de dados de alta performance.

---

### **J**

**JPA (Java Persistence API)**  
Especificação Java para mapeamento objeto-relacional (ORM).

**JPQL (Java Persistence Query Language)**  
Linguagem de consulta orientada a objetos para JPA.

---

### **L**

**LLM (Large Language Model)**  
Modelo de IA treinado em grandes volumes de texto para processamento de linguagem natural.

**Lombok**  
Biblioteca Java que reduz boilerplate através de anotações.

---

### **M**

**Migration**  
Script SQL versionado que altera o schema do banco de dados.

**Multi-Tenancy**  
Arquitetura onde múltiplos clientes (tenants) compartilham a mesma aplicação e banco de dados, mas com dados isolados.

---

### **O**

**OpenAI**  
Empresa que desenvolve modelos de IA como GPT-4.

**ORM (Object-Relational Mapping)**  
Técnica de mapear objetos para tabelas de banco de dados.

---

### **P**

**Patient**  
Paciente monitorado pelo sistema.

**Prompt**  
Instrução textual enviada à IA para guiar seu comportamento.

---

### **R**

**Repository**  
Interface JPA para acesso a dados de uma entidade.

**REST (Representational State Transfer)**  
Estilo arquitetural para APIs web.

---

### **S**

**ShedLock**  
Biblioteca para garantir execução única de jobs agendados em ambientes distribuídos.

**Spring AI**  
Framework Spring para integração com modelos de IA.

**Spring Boot**  
Framework Java para criação de aplicações enterprise.

**Swagger**  
Ferramenta para documentação interativa de APIs REST.

---

### **T**

**Tenant**  
Cliente isolado em um sistema multi-tenant (sinônimo de Account).

**ThreadLocal**  
Mecanismo Java para armazenar dados específicos de cada thread.

---

### **W**

**Webhook**  
Endpoint HTTP que recebe notificações de eventos de sistemas externos.

**WhatsApp Business API**  
API oficial do WhatsApp para comunicação empresarial.

---

## 🏥 Termos de Saúde

**Dor Crônica**  
Dor persistente por mais de 3 meses.

**Enxaqueca**  
Tipo de dor de cabeça intensa, geralmente unilateral.

**Gatilho**  
Fator que desencadeia uma crise (ex: estresse, alimentos).

**Humor**  
Estado emocional do paciente (feliz, triste, ansioso, etc).

**Nível de Dor**  
Escala de 0-10 para quantificar intensidade da dor.

**Qualidade do Sono**  
Avaliação subjetiva de quão bem o paciente dormiu.

---

## 💼 Termos de Negócio

**ARR (Annual Recurring Revenue)**  
Receita recorrente anual.

**CAC (Customer Acquisition Cost)**  
Custo para adquirir um novo cliente.

**Churn**  
Taxa de cancelamento de clientes.

**LTV (Lifetime Value)**  
Valor total que um cliente gera durante seu relacionamento com a empresa.

**MRR (Monthly Recurring Revenue)**  
Receita recorrente mensal.

**Payback**  
Tempo necessário para recuperar o CAC.

**Slot**  
Vaga para paciente em um plano B2B.

---

## 🎯 Próximos Passos

1. ❓ [FAQ](02-faq.md)
2. 🐛 [Troubleshooting](03-troubleshooting.md)
3. 📝 [Changelog](04-changelog.md)

---

[⬅️ Anterior: Testes de Integração](../12-testing/03-integration-tests.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: FAQ](02-faq.md)

