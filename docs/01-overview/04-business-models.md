# 1.4 Modelos de Negócio

## 💼 Visão Geral

O AI Health Agent suporta **dois modelos de negócio distintos**, cada um com características e precificação específicas.

---

## 🏠 Modelo B2C (Business-to-Consumer)

### **Descrição**
Pacientes individuais que usam o sistema para monitorar sua própria saúde.

### **Características**

| Característica | Descrição |
|----------------|-----------|
| **Tenant** | 1 account = 1 paciente |
| **Usuários** | Apenas o próprio paciente |
| **Customização** | Limitada (prompts padrão) |
| **Dashboard** | Visão pessoal de dados |
| **Suporte** | Self-service + FAQ |

### **Funcionalidades**

✅ **Incluídas:**
- Conversação ilimitada via WhatsApp
- Extração automática de dados de saúde
- Check-ins proativos (até 3/dia)
- Alertas automáticos de crises
- Dashboard pessoal
- Histórico de 90 dias

❌ **Não incluídas:**
- Múltiplos pacientes
- Prompts customizados
- Relatórios profissionais
- Integração com prontuários

### **Precificação Sugerida**

| Plano | Preço | Mensagens IA/mês | Check-ins/dia |
|-------|-------|------------------|---------------|
| **Free** | R$ 0 | 50 | 1 |
| **Basic** | R$ 29,90 | 500 | 2 |
| **Premium** | R$ 49,90 | Ilimitado | 3 |

### **Custos Operacionais (por usuário/mês)**

```
Custo OpenAI:
- 500 mensagens/mês × R$ 0,02 = R$ 10,00
- Margem: R$ 19,90 (66%)

Custo Infraestrutura:
- Railway: R$ 5,00/usuário
- WhatsApp (Evolution API): R$ 0,00 (self-hosted)
- Total: R$ 5,00

Margem Líquida: R$ 14,90 (50%)
```

### **Persona Típica**

**Nome:** Maria Silva  
**Idade:** 35 anos  
**Condição:** Enxaqueca crônica  
**Objetivo:** Monitorar padrões de dor e identificar gatilhos  
**Uso:** 2-3 mensagens/dia, check-in noturno  

---

## 🏢 Modelo B2B (Business-to-Business)

### **Descrição**
Profissionais de saúde (psicólogos, terapeutas) que monitoram múltiplos pacientes.

### **Características**

| Característica | Descrição |
|----------------|-----------|
| **Tenant** | 1 account = 1 profissional |
| **Usuários** | Profissional + N pacientes |
| **Customização** | Total (prompts personalizados) |
| **Dashboard** | Visão consolidada de todos os pacientes |
| **Suporte** | Prioritário + onboarding |

### **Funcionalidades**

✅ **Incluídas (todas do B2C +):**
- Múltiplos pacientes (slots)
- Prompts customizados por profissional
- Dashboard consolidado
- Alertas prioritários
- Relatórios exportáveis (PDF)
- Histórico ilimitado
- API de integração
- Suporte prioritário

### **Precificação Sugerida**

| Plano | Preço | Slots | Mensagens/mês |
|-------|-------|-------|---------------|
| **Starter** | R$ 199/mês | 10 pacientes | 5.000 |
| **Professional** | R$ 499/mês | 30 pacientes | 15.000 |
| **Enterprise** | R$ 999/mês | 100 pacientes | Ilimitado |

**Adicional:**
- Slot extra: R$ 15/mês
- Mensagens extras (pacote 1.000): R$ 20

### **Custos Operacionais (Plano Professional)**

```
Custo OpenAI:
- 15.000 mensagens/mês × R$ 0,02 = R$ 300,00

Custo Infraestrutura:
- Railway (instância dedicada): R$ 50,00
- Armazenamento: R$ 20,00
- Total: R$ 70,00

Custo Total: R$ 370,00
Margem Líquida: R$ 129,00 (26%)

Por paciente: R$ 16,63/mês
```

### **Persona Típica**

**Nome:** Dr. João Santos  
**Profissão:** Psicólogo clínico  
**Pacientes:** 25 ativos  
**Objetivo:** Monitorar humor e adesão ao tratamento  
**Uso:** Check-ins diários automáticos, revisão semanal  

---

## 📊 Comparação de Modelos

| Aspecto | B2C | B2B |
|---------|-----|-----|
| **Público-alvo** | Pacientes individuais | Profissionais de saúde |
| **Ticket médio** | R$ 29-49/mês | R$ 199-999/mês |
| **LTV (12 meses)** | R$ 348-588 | R$ 2.388-11.988 |
| **CAC alvo** | R$ 50-100 | R$ 300-500 |
| **Payback** | 2-3 meses | 2-3 meses |
| **Churn esperado** | 15-20%/mês | 5-10%/mês |
| **Margem** | 50-60% | 25-35% |

---

## 🎯 Estratégia de Go-to-Market

### **Fase 1: B2C (MVP)**
**Objetivo:** Validar produto e gerar receita inicial

1. **Lançamento Soft (Mês 1-2)**
   - 50 beta testers gratuitos
   - Coletar feedback
   - Ajustar produto

2. **Lançamento Público (Mês 3-4)**
   - Plano Free (aquisição)
   - Plano Basic (conversão)
   - Marketing digital (Instagram, TikTok)

3. **Meta:** 500 usuários pagantes em 6 meses

---

### **Fase 2: B2B (Escala)**
**Objetivo:** Aumentar ticket médio e reduzir churn

1. **Piloto com Psicólogos (Mês 4-6)**
   - 10 profissionais selecionados
   - Onboarding personalizado
   - Casos de sucesso

2. **Lançamento B2B (Mês 7-9)**
   - Plano Starter
   - Plano Professional
   - Vendas diretas + parcerias

3. **Meta:** 50 profissionais em 12 meses

---

## 💰 Projeção de Receita (12 meses)

### **Cenário Conservador**

| Mês | B2C (R$) | B2B (R$) | Total (R$) | MRR |
|-----|----------|----------|------------|-----|
| 1-3 | 0 | 0 | 0 | 0 |
| 4 | 1.500 | 0 | 1.500 | 1.500 |
| 6 | 7.500 | 2.000 | 9.500 | 9.500 |
| 9 | 15.000 | 10.000 | 25.000 | 25.000 |
| 12 | 25.000 | 25.000 | 50.000 | 50.000 |

**ARR (12 meses):** R$ 600.000

---

## 🚀 Oportunidades de Expansão

### **Curto Prazo (6-12 meses)**
1. **Plano Família** - R$ 79/mês para até 4 pessoas
2. **Add-ons:**
   - Relatórios PDF: R$ 9,90/mês
   - Integração com wearables: R$ 14,90/mês
   - Análise preditiva: R$ 19,90/mês

### **Médio Prazo (12-24 meses)**
1. **B2B2C (Clínicas e Hospitais)**
   - White-label
   - Integração com prontuários
   - Precificação customizada

2. **Marketplace de Integrações**
   - Desenvolvedores terceiros
   - Revenue share (70/30)

### **Longo Prazo (24+ meses)**
1. **Expansão Internacional**
   - Inglês, Espanhol
   - Compliance local (HIPAA, GDPR)

2. **Novos Verticais**
   - Nutrição
   - Fisioterapia
   - Gestantes

---

## 🎯 Próximos Passos

1. 🚀 Comece com o [Getting Started](../02-getting-started/01-prerequisites.md)
2. 🗄️ Explore a [Estrutura do Banco](../03-database/01-database-structure.md)
3. 🏗️ Entenda a [Arquitetura em Camadas](../04-architecture/01-layered-architecture.md)

---

[⬅️ Anterior: Tecnologias](03-technologies.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Getting Started](../02-getting-started/01-prerequisites.md)

