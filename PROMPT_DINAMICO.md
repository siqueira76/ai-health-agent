# 🤖 Sistema de Prompt Dinâmico - AI Health Agent

## 📋 Visão Geral

O sistema implementa **prompts dinâmicos baseados no tenant**, permitindo que cada Account (B2C ou B2B) tenha sua própria personalização da IA.

---

## 🎯 Arquitetura

### Fluxo de Processamento

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Mensagem do WhatsApp                                         │
│    └─> whatsappNumber: "5511999999999"                          │
│    └─> message: "Estou com muita dor hoje"                      │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. Identificar Paciente e Tenant                                │
│    └─> Patient.findByWhatsappNumber()                           │
│    └─> tenantId = patient.getAccount().getId()                  │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. Buscar Account e Prompt Customizado                          │
│    └─> Account.findById(tenantId)                               │
│    └─> customPrompt = account.getCustomPrompt()                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. Construir System Message Dinâmico                            │
│    └─> Se customPrompt existe → usa ele                         │
│    └─> Senão → usa prompt padrão (B2C ou B2B)                   │
│    └─> Adiciona contexto do paciente (nome, diagnóstico)        │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. Chamar OpenAI com Prompt Personalizado                       │
│    └─> SystemMessage(customPrompt)                              │
│    └─> UserMessage("Estou com muita dor hoje")                  │
│    └─> ChatClient.call()                                        │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. Retornar Resposta Personalizada                              │
│    └─> "Sinto muito que esteja com dor. Em uma escala..."       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Componentes Principais

### 1. **PromptService**

Responsável por gerenciar os prompts do sistema.

<augment_code_snippet path="src/main/java/com/healthlink/ai_health_agent/service/PromptService.java" mode="EXCERPT">
````java
@Service
public class PromptService {
    
    // Prompt padrão B2C (Fibromialgia)
    private static final String DEFAULT_B2C_PROMPT = """
        Você é um assistente terapêutico especializado em fibromialgia...
        """;
    
    // Prompt padrão B2B (Psicólogos)
    private static final String DEFAULT_B2B_PROMPT = """
        Você é um assistente terapêutico configurável...
        """;
    
    public String buildSystemMessage(Account account) {
        // Se tem prompt customizado, usa ele
        if (account.getCustomPrompt() != null) {
            return account.getCustomPrompt();
        }
        // Senão, usa o padrão baseado no tipo
        return account.getType() == AccountType.B2C 
            ? DEFAULT_B2C_PROMPT 
            : DEFAULT_B2B_PROMPT;
    }
}
````
</augment_code_snippet>

### 2. **AIService**

Service principal que processa mensagens com contexto multi-tenant.

<augment_code_snippet path="src/main/java/com/healthlink/ai_health_agent/service/AIService.java" mode="EXCERPT">
````java
@Service
public class AIService {
    
    public String processMessage(String whatsappNumber, String userMessage) {
        // 1. Identificar paciente e tenant
        Patient patient = patientRepository.findByWhatsappNumber(whatsappNumber);
        UUID tenantId = patient.getTenantId();
        
        // 2. Buscar account
        Account account = accountRepository.findById(tenantId);
        
        // 3. Construir System Message dinâmico
        String systemPrompt = promptService.buildSystemMessageWithContext(
            account, patient.getName(), patient.getDiagnosis()
        );
        
        // 4. Chamar IA
        Prompt prompt = new Prompt(List.of(
            new SystemMessage(systemPrompt),
            new UserMessage(userMessage)
        ));
        
        return chatClient.prompt(prompt).call().chatResponse();
    }
}
````
</augment_code_snippet>

---

## 📊 Exemplos de Uso

### Cenário 1: Conta B2C com Prompt Padrão

**Account:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "type": "B2C",
  "customPrompt": null
}
```

**System Message Gerado:**
```
Você é um assistente terapêutico especializado em fibromialgia e dor crônica.

Seu papel é:
- Acompanhar diariamente o paciente com empatia e acolhimento
- Fazer perguntas sobre níveis de dor, humor e qualidade do sono
...

--- CONTEXTO DO PACIENTE ---
Nome: Maria Silva
Diagnóstico: Fibromialgia

Personalize suas respostas considerando este contexto.
```

### Cenário 2: Conta B2B com Prompt Customizado

**Account:**
```json
{
  "id": "987e6543-e21b-43d2-b654-426614174111",
  "type": "B2B",
  "customPrompt": "Você é um assistente de Terapia Cognitivo-Comportamental (TCC). Seu papel é fazer check-ins diários focados em identificar pensamentos automáticos negativos e ajudar o paciente a reestruturá-los. Use linguagem acolhedora mas diretiva. Sempre pergunte sobre situações específicas do dia."
}
```

**System Message Gerado:**
```
Você é um assistente de Terapia Cognitivo-Comportamental (TCC). 
Seu papel é fazer check-ins diários focados em identificar pensamentos 
automáticos negativos e ajudar o paciente a reestruturá-los...

--- CONTEXTO DO PACIENTE ---
Nome: João Santos
Diagnóstico: Transtorno de Ansiedade Generalizada

Personalize suas respostas considerando este contexto.
```

---

## 🛡️ Validação de Prompts Customizados

O sistema valida prompts customizados para garantir segurança:

```java
public boolean validateCustomPrompt(String customPrompt) {
    // 1. Não pode ser vazio
    if (customPrompt == null || customPrompt.isBlank()) {
        return false;
    }
    
    // 2. Limite de 5000 caracteres
    if (customPrompt.length() > 5000) {
        return false;
    }
    
    // 3. Deve conter diretrizes de segurança
    boolean hasSafetyGuidelines = customPrompt.toLowerCase().contains("nunca substitua") 
            || customPrompt.toLowerCase().contains("não substitui");
    
    return hasSafetyGuidelines;
}
```

---

## 🚀 API Endpoints

### 1. Processar Mensagem

```bash
POST /api/ai/message
Content-Type: application/json

{
  "whatsappNumber": "5511999999999",
  "message": "Estou com muita dor hoje"
}
```

**Resposta:**
```json
{
  "whatsappNumber": "5511999999999",
  "response": "Sinto muito que esteja com dor, Maria. Em uma escala de 0 a 10, como você classificaria sua dor neste momento?"
}
```

### 2. Preview do Prompt

```bash
GET /api/ai/prompt/preview/{tenantId}
```

**Resposta:**
```json
{
  "tenantId": "123e4567-e89b-12d3-a456-426614174000",
  "systemPrompt": "Você é um assistente terapêutico..."
}
```

### 3. Atualizar Prompt Customizado

```bash
PUT /api/ai/prompt/{tenantId}
Content-Type: application/json

{
  "customPrompt": "Você é um assistente de TCC..."
}
```

---

## 💡 Benefícios da Abordagem

| Benefício | Descrição |
|-----------|-----------|
| **Personalização** | Cada psicólogo pode customizar o tom e abordagem |
| **Flexibilidade** | Suporta diferentes linhas terapêuticas (TCC, Psicanálise, etc) |
| **Isolamento** | Prompts são isolados por tenant (multi-tenancy) |
| **Segurança** | Validação automática de prompts perigosos |
| **Performance** | Prompt é carregado uma vez por mensagem |
| **Auditoria** | Histórico de mudanças no prompt pode ser rastreado |

---

## 📝 Próximos Passos

1. ✅ PromptService criado
2. ✅ AIService com suporte a prompts dinâmicos
3. ✅ Controller com endpoints de teste
4. ⏳ Implementar histórico de conversas
5. ⏳ Adicionar Function Calling para ações específicas
6. ⏳ Integrar com Evolution API (WhatsApp)

---

**Documentação criada em:** 2026-02-19

