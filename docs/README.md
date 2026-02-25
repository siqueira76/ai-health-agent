# 📚 AI Health Agent - Documentação Completa

> Sistema multi-tenant de monitoramento de saúde via WhatsApp com Inteligência Artificial

---

## 📖 Índice da Documentação

### 🎯 **1. Visão Geral**
- [1.1 Introdução](01-overview/01-introduction.md)
- [1.2 Arquitetura da Solução](01-overview/02-architecture.md)
- [1.3 Tecnologias Utilizadas](01-overview/03-technologies.md)
- [1.4 Modelos de Negócio](01-overview/04-business-models.md)

### 🚀 **2. Getting Started**
- [2.1 Pré-requisitos](02-getting-started/01-prerequisites.md)
- [2.2 Instalação](02-getting-started/02-installation.md)
- [2.3 Configuração](02-getting-started/03-configuration.md)
- [2.4 Primeiro Deploy](02-getting-started/04-first-deploy.md)

### 🗄️ **3. Banco de Dados**
- [3.1 Estrutura do Banco](03-database/01-database-structure.md)
- [3.2 Modelo de Dados](03-database/02-data-model.md)
- [3.3 Migrations](03-database/03-migrations.md)
- [3.4 Relacionamentos](03-database/04-relationships.md)

### 🏗️ **4. Arquitetura**
- [4.1 Arquitetura em Camadas](04-architecture/01-layered-architecture.md)
- [4.2 Multi-Tenancy](04-architecture/02-multi-tenancy.md)
- [4.3 Padrões de Projeto](04-architecture/03-design-patterns.md)
- [4.4 Segurança](04-architecture/04-security.md)

### 🤖 **5. Inteligência Artificial**
- [5.1 Spring AI Overview](05-ai/01-spring-ai-overview.md)
- [5.2 Function Calling](05-ai/02-function-calling.md)
- [5.3 Prompts e Contexto](05-ai/03-prompts-context.md)
- [5.4 Extração de Dados](05-ai/04-data-extraction.md)

### 📡 **6. API e Endpoints**
- [6.1 Visão Geral da API](06-api/01-api-overview.md)
- [6.2 Webhook WhatsApp](06-api/02-webhook-whatsapp.md)
- [6.3 Dashboard Endpoints](06-api/03-dashboard-endpoints.md)
- [6.4 Autenticação](06-api/04-authentication.md)

### 💬 **7. Integração WhatsApp**
- [7.1 Evolution API](07-whatsapp/01-evolution-api.md)
- [7.2 Fluxo de Mensagens](07-whatsapp/02-message-flow.md)
- [7.3 Webhooks](07-whatsapp/03-webhooks.md)
- [7.4 Tratamento de Erros](07-whatsapp/04-error-handling.md)

### ⏰ **8. Check-ins Proativos**
- [8.1 Conceito](08-proactive-checkins/01-concept.md)
- [8.2 Agendamento](08-proactive-checkins/02-scheduling.md)
- [8.3 Execução](08-proactive-checkins/03-execution.md)
- [8.4 Rate Limiting](08-proactive-checkins/04-rate-limiting.md)

### 🚨 **9. Sistema de Alertas**
- [9.1 Tipos de Alertas](09-alerts/01-alert-types.md)
- [9.2 Detecção Automática](09-alerts/02-automatic-detection.md)
- [9.3 Notificações](09-alerts/03-notifications.md)

### 📊 **10. Analytics e Dashboard**
- [10.1 Métricas](10-analytics/01-metrics.md)
- [10.2 Tendências](10-analytics/02-trends.md)
- [10.3 Relatórios](10-analytics/03-reports.md)

### 🔧 **11. Configuração e Deploy**
- [11.1 Variáveis de Ambiente](11-deployment/01-environment-variables.md)
- [11.2 Docker](11-deployment/02-docker.md)
- [11.3 Railway Deploy](11-deployment/03-railway-deploy.md)
- [11.4 Monitoramento](11-deployment/04-monitoring.md)

### 🧪 **12. Testes**
- [12.1 Testes Unitários](12-testing/01-unit-tests.md)
- [12.2 Testes de Integração](12-testing/02-integration-tests.md)
- [12.3 Testes E2E](12-testing/03-e2e-tests.md)

### 📚 **13. Referências**
- [13.1 Glossário](13-reference/01-glossary.md)
- [13.2 FAQ](13-reference/02-faq.md)
- [13.3 Troubleshooting](13-reference/03-troubleshooting.md)
- [13.4 Changelog](13-reference/04-changelog.md)

---

## 🎯 Como Usar Esta Documentação

### **Para Desenvolvedores**
1. Comece pela [Introdução](01-overview/01-introduction.md)
2. Siga o [Getting Started](02-getting-started/01-prerequisites.md)
3. Estude a [Arquitetura](04-architecture/01-layered-architecture.md)

### **Para DevOps**
1. Leia [Configuração](02-getting-started/03-configuration.md)
2. Veja [Deploy](11-deployment/03-railway-deploy.md)
3. Configure [Monitoramento](11-deployment/04-monitoring.md)

### **Para Product Owners**
1. Entenda os [Modelos de Negócio](01-overview/04-business-models.md)
2. Veja as [Funcionalidades](01-overview/01-introduction.md)
3. Analise [Métricas](10-analytics/01-metrics.md)

---

## 📝 Convenções da Documentação

- 📘 **Azul** - Informação geral
- ✅ **Verde** - Boas práticas
- ⚠️ **Amarelo** - Avisos importantes
- 🔴 **Vermelho** - Perigos e erros críticos
- 💡 **Lâmpada** - Dicas e truques
- 🔐 **Cadeado** - Segurança

---

## 🤝 Contribuindo

Para atualizar esta documentação:

1. Edite os arquivos `.md` correspondentes
2. Mantenha a estrutura de pastas
3. Use Markdown padrão
4. Adicione exemplos de código quando relevante
5. Atualize o [Changelog](13-reference/04-changelog.md)

---

## 📄 Licença

MIT License - Veja LICENSE para detalhes

---

**Versão da Documentação:** 1.0.0  
**Última Atualização:** 2026-02-25  
**Versão da Aplicação:** 1.0.0

