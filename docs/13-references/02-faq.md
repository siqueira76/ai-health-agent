# 13.2 FAQ (Perguntas Frequentes)

## ❓ Perguntas Gerais

### **O que é o AI Health Agent?**

É um agente de IA conversacional via WhatsApp que monitora a saúde de pacientes, extrai dados automaticamente e gera alertas proativos.

---

### **Quais condições de saúde são suportadas?**

Atualmente focado em:
- Dor crônica (enxaqueca, fibromialgia)
- Saúde mental (ansiedade, depressão)
- Condições que requerem monitoramento diário

---

### **Quanto custa usar o sistema?**

**B2C:**
- Free: R$ 0/mês (50 mensagens)
- Basic: R$ 29,90/mês (500 mensagens)
- Premium: R$ 49,90/mês (ilimitado)

**B2B:**
- Starter: R$ 199/mês (10 pacientes)
- Professional: R$ 499/mês (30 pacientes)
- Enterprise: R$ 999/mês (100 pacientes)

---

## 🔧 Perguntas Técnicas

### **Qual versão do Java é necessária?**

Java 21 ou superior.

---

### **Posso usar outro banco de dados além do PostgreSQL?**

Tecnicamente sim (MySQL, MariaDB), mas o projeto foi otimizado para PostgreSQL 16+ e usa recursos específicos como `gen_random_uuid()`.

---

### **Como funciona o multi-tenancy?**

Usamos **shared database, shared schema** com isolamento via `account_id`. Todas as queries incluem filtro por tenant automaticamente.

---

### **Posso usar outro modelo de IA além do GPT-4o-mini?**

Sim! Spring AI suporta:
- OpenAI (GPT-4, GPT-4-turbo, GPT-3.5)
- Azure OpenAI
- Anthropic Claude
- Google Vertex AI
- Ollama (local)

Basta alterar a configuração em `application.properties`.

---

### **Como funciona o Function Calling?**

A IA recebe uma lista de funções disponíveis (ex: `extractHealthData`) e decide quando chamá-las baseado na conversa. Isso permite extração estruturada de dados.

---

### **Posso usar Telegram ao invés de WhatsApp?**

Sim, mas requer adaptação do `WhatsAppWebhookController` para o formato de webhook do Telegram.

---

## 🚀 Deployment

### **Onde posso fazer deploy?**

Recomendamos:
- **Railway** (mais fácil, deploy automático)
- **Render** (free tier generoso)
- **AWS** (mais controle, requer configuração)
- **Google Cloud Run** (serverless)
- **Azure App Service**

---

### **Preciso de um servidor dedicado?**

Não. O sistema roda bem em:
- Railway: $5-20/mês
- Render: Free tier ou $7/mês
- VPS básica: 1GB RAM, 1 vCPU

---

### **Como escalar para muitos usuários?**

1. **Horizontal scaling:** Múltiplas instâncias (Railway/Render fazem automaticamente)
2. **Database:** Connection pool otimizado (HikariCP)
3. **Cache:** Redis para sessões (futuro)
4. **CDN:** Para assets estáticos

---

## 🔐 Segurança

### **Os dados dos pacientes são seguros?**

Sim:
- ✅ Isolamento multi-tenant (impossível acessar dados de outro tenant)
- ✅ HTTPS obrigatório em produção
- ✅ Senhas hasheadas com BCrypt
- ✅ Variáveis de ambiente para secrets
- ✅ Compliance LGPD (dados no Brasil)

---

### **Como funciona a autenticação?**

Atualmente Basic Auth (desenvolvimento). Em produção, recomendamos JWT ou OAuth2.

---

### **Posso usar autenticação de dois fatores (2FA)?**

Não implementado nativamente, mas pode ser adicionado via Spring Security + Google Authenticator.

---

## 💬 WhatsApp

### **Preciso de uma conta WhatsApp Business?**

Sim, para usar a Evolution API você precisa de um número de WhatsApp Business.

---

### **Posso usar meu WhatsApp pessoal?**

Não recomendado. Use um número dedicado para evitar misturar conversas pessoais e profissionais.

---

### **Quantas mensagens posso enviar por dia?**

Depende do tier do WhatsApp Business:
- **Tier 1:** 1.000 conversas/dia
- **Tier 2:** 10.000 conversas/dia
- **Tier 3:** 100.000 conversas/dia

---

### **Como funciona a cobrança do WhatsApp?**

WhatsApp cobra por **conversa iniciada** (janela de 24h), não por mensagem individual.

---

## 🤖 Inteligência Artificial

### **A IA pode diagnosticar doenças?**

**NÃO!** O sistema apenas:
- ✅ Coleta dados relatados pelo paciente
- ✅ Identifica padrões
- ✅ Gera alertas para profissionais

**Nunca substitui consulta médica.**

---

### **Como garantir que a IA não dê conselhos médicos?**

Via **system prompt** que instrui a IA a:
- Não diagnosticar
- Não prescrever medicamentos
- Sempre recomendar consulta médica em casos graves

---

### **Posso customizar o comportamento da IA?**

Sim! Contas B2B podem ter `custom_prompt` personalizado.

---

### **Quanto custa a API da OpenAI?**

GPT-4o-mini:
- Input: $0.15 / 1M tokens (~R$ 0,75)
- Output: $0.60 / 1M tokens (~R$ 3,00)

Custo médio: R$ 0,02 por mensagem.

---

## 📊 Analytics

### **Quais métricas posso acompanhar?**

- Número de pacientes ativos
- Mensagens enviadas/recebidas
- Alertas gerados
- Tendências de dor/humor
- Taxa de resposta a check-ins

---

### **Posso exportar dados?**

Sim, via API REST ou exportação CSV (futuro).

---

## 🐛 Problemas Comuns

### **Erro: "Port 8080 already in use"**

```bash
# Encontrar processo
lsof -i :8080

# Matar processo
kill -9 <PID>
```

---

### **Erro: "OpenAI API key not found"**

Configure a variável de ambiente:
```bash
export OPENAI_API_KEY=sk-proj-xxx
```

---

### **Erro: "Could not connect to database"**

Verifique se PostgreSQL está rodando:
```bash
docker ps | grep postgres
```

---

### **Flyway migration failed**

Resetar banco (CUIDADO - perde dados):
```bash
docker-compose down -v
docker-compose up -d
```

---

## 🎯 Próximos Passos

1. 🐛 [Troubleshooting Detalhado](03-troubleshooting.md)
2. 📝 [Changelog](04-changelog.md)
3. 🚀 [Voltar ao Getting Started](../02-getting-started/01-prerequisites.md)

---

[⬅️ Anterior: Glossário](01-glossary.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Troubleshooting](03-troubleshooting.md)

