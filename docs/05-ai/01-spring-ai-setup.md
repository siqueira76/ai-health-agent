# 5.1 Spring AI Setup

## 🤖 Configuração do Spring AI

Spring AI é o framework oficial do Spring para integração com modelos de IA (LLMs).

---

## 📦 Dependências

### **pom.xml:**

```xml
<dependencies>
    <!-- Spring AI OpenAI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        <version>1.0.0-M5</version>
    </dependency>
    
    <!-- Spring AI Core -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-core</artifactId>
        <version>1.0.0-M5</version>
    </dependency>
</dependencies>

<!-- Repository para versões Milestone -->
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
    </repository>
</repositories>
```

---

## ⚙️ Configuração

### **application.properties:**

```properties
# ============================================
# SPRING AI - OPENAI
# ============================================
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=500

# Timeout
spring.ai.openai.chat.options.timeout=30s

# Retry
spring.ai.retry.max-attempts=3
spring.ai.retry.backoff.initial-interval=2s
```

### **Variáveis de Ambiente:**

```bash
# .env.local
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

---

## 🔧 Configuração Java

### **OpenAiConfig.java:**

```java
@Configuration
public class OpenAiConfig {
    
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    
    @Bean
    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4o-mini")
            .temperature(0.7)
            .maxTokens(500)
            .build();
    }
    
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
```

---

## 💬 Uso Básico

### **Conversação Simples:**

```java
@Service
public class AiConversationService {
    
    private final ChatClient chatClient;
    
    @Autowired
    public AiConversationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    
    public String chat(String userMessage) {
        return chatClient.prompt()
            .user(userMessage)
            .call()
            .content();
    }
}
```

### **Exemplo de Uso:**

```java
String response = aiService.chat("Olá, como você está?");
// Resposta: "Olá! Estou bem, obrigado por perguntar. Como posso ajudá-lo hoje?"
```

---

## 🎯 Conversação com Contexto

### **System Prompt:**

```java
@Service
public class AiConversationService {
    
    private final ChatClient chatClient;
    
    private static final String SYSTEM_PROMPT = """
        Você é um assistente de saúde empático e profissional.
        Seu objetivo é coletar informações sobre a saúde do paciente.
        
        REGRAS:
        - Seja empático e acolhedor
        - Faça perguntas claras e objetivas
        - NUNCA diagnostique doenças
        - NUNCA prescreva medicamentos
        - Em casos graves, recomende procurar um médico
        - Colete dados sobre: dor, humor, sono, medicamentos, energia
        """;
    
    public String chat(String userMessage) {
        return chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(userMessage)
            .call()
            .content();
    }
}
```

---

## 📝 Conversação com Histórico

### **Manter Contexto:**

```java
@Service
public class AiConversationService {
    
    private final ChatClient chatClient;
    private final ChatMessageRepository chatMessageRepository;
    
    public String chat(Patient patient, String userMessage) {
        // 1. Buscar últimas 10 mensagens
        List<ChatMessage> history = chatMessageRepository
            .findRecentByPatient(patient.getId(), PageRequest.of(0, 10));
        
        // 2. Construir contexto
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        
        // Adicionar histórico
        for (ChatMessage msg : history) {
            if (msg.getSender() == MessageSender.PATIENT) {
                messages.add(new UserMessage(msg.getMessageText()));
            } else {
                messages.add(new AssistantMessage(msg.getMessageText()));
            }
        }
        
        // Adicionar mensagem atual
        messages.add(new UserMessage(userMessage));
        
        // 3. Chamar IA
        String response = chatClient.prompt()
            .messages(messages)
            .call()
            .content();
        
        // 4. Salvar mensagens
        saveChatMessages(patient, userMessage, response);
        
        return response;
    }
}
```

---

## 🎨 Customização por Tenant

### **Prompt Personalizado:**

```java
@Service
public class AiConversationService {
    
    public String chat(Patient patient, String userMessage) {
        Account account = patient.getAccount();
        
        // Usar prompt customizado do tenant (se existir)
        String systemPrompt = account.getCustomPrompt() != null 
            ? account.getCustomPrompt() 
            : DEFAULT_SYSTEM_PROMPT;
        
        return chatClient.prompt()
            .system(systemPrompt)
            .user(userMessage)
            .call()
            .content();
    }
}
```

### **Exemplo de Custom Prompt (B2B):**

```
Você é um assistente especializado em enxaqueca.
Foque em coletar informações sobre:
- Intensidade da dor (0-10)
- Localização (unilateral/bilateral)
- Sintomas associados (náusea, fotofobia, aura)
- Gatilhos (estresse, alimentos, sono)
- Medicamentos tomados

Seja técnico mas acessível.
```

---

## 🔄 Streaming de Respostas

### **Resposta em Tempo Real:**

```java
@Service
public class AiConversationService {
    
    public Flux<String> chatStream(String userMessage) {
        return chatClient.prompt()
            .user(userMessage)
            .stream()
            .content();
    }
}
```

### **Uso com WebFlux:**

```java
@RestController
public class ChatController {
    
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String message) {
        return aiService.chatStream(message);
    }
}
```

---

## 📊 Modelos Disponíveis

### **OpenAI:**

| Modelo | Custo (Input) | Custo (Output) | Contexto | Uso |
|--------|---------------|----------------|----------|-----|
| gpt-4o | $2.50/1M | $10.00/1M | 128k | Máxima qualidade |
| gpt-4o-mini | $0.15/1M | $0.60/1M | 128k | **Recomendado** |
| gpt-4-turbo | $10.00/1M | $30.00/1M | 128k | Tarefas complexas |
| gpt-3.5-turbo | $0.50/1M | $1.50/1M | 16k | Simples e rápido |

### **Trocar Modelo:**

```properties
spring.ai.openai.chat.options.model=gpt-4o
```

---

## 🔧 Configurações Avançadas

### **Temperatura:**

```properties
# 0.0 = Determinístico (sempre mesma resposta)
# 1.0 = Criativo (respostas variadas)
spring.ai.openai.chat.options.temperature=0.7
```

### **Max Tokens:**

```properties
# Limitar tamanho da resposta
spring.ai.openai.chat.options.max-tokens=500
```

### **Top P (Nucleus Sampling):**

```properties
# Alternativa à temperatura
spring.ai.openai.chat.options.top-p=0.9
```

### **Frequency Penalty:**

```properties
# Penalizar repetições (0.0 a 2.0)
spring.ai.openai.chat.options.frequency-penalty=0.5
```

---

## 🐛 Tratamento de Erros

### **Retry e Fallback:**

```java
@Service
public class AiConversationService {
    
    @Retryable(
        value = {OpenAiException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public String chat(String userMessage) {
        try {
            return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
        } catch (RateLimitException e) {
            log.warn("Rate limit exceeded, retrying...");
            throw e;
        } catch (Exception e) {
            log.error("AI call failed", e);
            return getFallbackResponse();
        }
    }
    
    private String getFallbackResponse() {
        return "Desculpe, estou com dificuldades técnicas no momento. " +
               "Por favor, tente novamente em alguns instantes.";
    }
}
```

---

## 📈 Monitoramento

### **Logs:**

```java
@Slf4j
@Service
public class AiConversationService {
    
    public String chat(String userMessage) {
        long startTime = System.currentTimeMillis();
        
        String response = chatClient.prompt()
            .user(userMessage)
            .call()
            .content();
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("AI response generated in {}ms", duration);
        
        return response;
    }
}
```

---

## 🎯 Próximos Passos

1. 🔧 [Function Calling](02-function-calling.md)
2. 📝 [Prompts](03-prompts.md)
3. 📊 [Extração de Dados](04-data-extraction.md)

---

[⬅️ Anterior: Segurança](../04-architecture/04-security.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Function Calling](02-function-calling.md)

