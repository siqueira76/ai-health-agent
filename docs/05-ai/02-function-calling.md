# 5.2 Function Calling

## 🔧 O que é Function Calling?

Function Calling permite que a IA **chame funções estruturadas** para extrair dados ou executar ações baseadas na conversa.

---

## 🎯 Por que usar?

### **Sem Function Calling:**
```
User: "Estou com dor de cabeça nível 8 e não dormi bem"
AI: "Sinto muito que esteja com dor. Espero que melhore!"
```
❌ Dados não estruturados, difícil de analisar

### **Com Function Calling:**
```
User: "Estou com dor de cabeça nível 8 e não dormi bem"
AI chama: extractHealthData({
  painLevel: 8,
  painType: "dor de cabeça",
  sleepQuality: "ruim"
})
```
✅ Dados estruturados, salvos automaticamente no banco

---

## 📋 Definindo uma Function

### **1. Criar DTO de Request:**

```java
@Data
@Builder
public class HealthDataRequest {
    
    @JsonProperty("pain_level")
    @JsonPropertyDescription("Nível de dor de 0 a 10, onde 0 é sem dor e 10 é dor insuportável")
    private Integer painLevel;
    
    @JsonProperty("mood")
    @JsonPropertyDescription("Estado emocional: feliz, triste, ansioso, irritado, calmo, etc")
    private String mood;
    
    @JsonProperty("sleep_quality")
    @JsonPropertyDescription("Qualidade do sono: ótimo, bom, regular, ruim, péssimo")
    private String sleepQuality;
    
    @JsonProperty("medications_taken")
    @JsonPropertyDescription("Lista de medicamentos tomados hoje")
    private List<String> medicationsTaken;
    
    @JsonProperty("energy_level")
    @JsonPropertyDescription("Nível de energia: alto, médio, baixo")
    private String energyLevel;
    
    @JsonProperty("stress_level")
    @JsonPropertyDescription("Nível de estresse: baixo, médio, alto")
    private String stressLevel;
    
    @JsonProperty("notes")
    @JsonPropertyDescription("Observações adicionais sobre a saúde")
    private String notes;
}
```

**Importante:**
- `@JsonProperty` - Nome do campo no JSON
- `@JsonPropertyDescription` - Descrição para a IA entender o campo

---

### **2. Criar a Function:**

```java
@Component
@Description("Extrai e salva dados de saúde da conversa com o paciente")
public class ExtractHealthDataFunction implements Function<HealthDataRequest, String> {
    
    private final HealthLogService healthLogService;
    
    @Autowired
    public ExtractHealthDataFunction(HealthLogService healthLogService) {
        this.healthLogService = healthLogService;
    }
    
    @Override
    public String apply(HealthDataRequest request) {
        // Obter paciente do contexto
        Patient patient = PatientContext.getCurrentPatient();
        
        // Criar HealthLog
        HealthLog log = HealthLog.builder()
            .account(patient.getAccount())
            .patient(patient)
            .painLevel(request.getPainLevel())
            .mood(request.getMood())
            .sleepQuality(request.getSleepQuality())
            .medicationsTaken(String.join(", ", request.getMedicationsTaken()))
            .energyLevel(request.getEnergyLevel())
            .stressLevel(request.getStressLevel())
            .notes(request.getNotes())
            .build();
        
        // Salvar
        healthLogService.save(log);
        
        return "Dados de saúde salvos com sucesso!";
    }
}
```

---

### **3. Registrar a Function:**

```java
@Configuration
public class FunctionConfig {
    
    @Bean
    public FunctionCallback extractHealthDataCallback(ExtractHealthDataFunction function) {
        return FunctionCallback.builder()
            .function("extractHealthData", function)
            .description("Extrai e salva dados de saúde da conversa")
            .inputType(HealthDataRequest.class)
            .build();
    }
}
```

---

## 🚀 Usando Function Calling

### **Service com Function Calling:**

```java
@Service
public class AiConversationService {
    
    private final ChatClient chatClient;
    private final ExtractHealthDataFunction extractHealthDataFunction;
    
    public String chat(Patient patient, String userMessage) {
        // Definir contexto do paciente
        PatientContext.setCurrentPatient(patient);
        
        try {
            // Chamar IA com function disponível
            ChatResponse response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .functions("extractHealthData") // 🔧 Registrar function
                .call()
                .chatResponse();
            
            return response.getResult().getOutput().getContent();
            
        } finally {
            PatientContext.clear();
        }
    }
}
```

---

## 📊 Fluxo Completo

```
1. User: "Estou com dor nível 7 e ansioso"
   ↓
2. ChatClient envia para OpenAI com function disponível
   ↓
3. OpenAI analisa e decide chamar extractHealthData
   ↓
4. OpenAI retorna:
   {
     "function_call": {
       "name": "extractHealthData",
       "arguments": {
         "pain_level": 7,
         "mood": "ansioso"
       }
     }
   }
   ↓
5. Spring AI executa ExtractHealthDataFunction.apply()
   ↓
6. Dados salvos no banco
   ↓
7. AI responde: "Entendi, registrei que você está com dor nível 7 
                 e se sentindo ansioso. Gostaria de me contar mais?"
```

---

## 🎯 Múltiplas Functions

### **Criar mais functions:**

```java
@Component
@Description("Agenda um check-in proativo para o paciente")
public class ScheduleCheckinFunction implements Function<ScheduleCheckinRequest, String> {
    
    @Override
    public String apply(ScheduleCheckinRequest request) {
        Patient patient = PatientContext.getCurrentPatient();
        
        CheckinSchedule schedule = CheckinSchedule.builder()
            .patient(patient)
            .frequency(request.getFrequency())
            .timeOfDay(request.getTimeOfDay())
            .build();
        
        checkinService.save(schedule);
        
        return "Check-in agendado com sucesso!";
    }
}
```

### **Registrar múltiplas functions:**

```java
ChatResponse response = chatClient.prompt()
    .user(userMessage)
    .functions("extractHealthData", "scheduleCheckin", "createAlert")
    .call()
    .chatResponse();
```

---

## 🔄 Function Calling Automático vs Manual

### **Automático (Recomendado):**

```java
// Spring AI executa a function automaticamente
String response = chatClient.prompt()
    .user(userMessage)
    .functions("extractHealthData")
    .call()
    .content();
```

### **Manual (Controle Total):**

```java
ChatResponse response = chatClient.prompt()
    .user(userMessage)
    .functions("extractHealthData")
    .call()
    .chatResponse();

// Verificar se IA quer chamar function
if (response.getResult().getOutput().hasToolCalls()) {
    ToolCall toolCall = response.getResult().getOutput().getToolCalls().get(0);
    
    // Executar manualmente
    String result = extractHealthDataFunction.apply(
        objectMapper.readValue(toolCall.getArguments(), HealthDataRequest.class)
    );
    
    // Enviar resultado de volta para IA
    String finalResponse = chatClient.prompt()
        .user("Resultado: " + result)
        .call()
        .content();
}
```

---

## 🎨 Customizando Behavior

### **Forçar uso de function:**

```java
ChatResponse response = chatClient.prompt()
    .user(userMessage)
    .functions("extractHealthData")
    .options(OpenAiChatOptions.builder()
        .functionCallbacks(List.of(extractHealthDataCallback))
        .toolChoice("extractHealthData") // Força uso
        .build())
    .call()
    .chatResponse();
```

### **Desabilitar function em contextos específicos:**

```java
// Apenas conversa, sem extrair dados
String response = chatClient.prompt()
    .user(userMessage)
    .call() // Sem .functions()
    .content();
```

---

## 🐛 Debugging

### **Logs detalhados:**

```java
@Slf4j
@Component
public class ExtractHealthDataFunction implements Function<HealthDataRequest, String> {
    
    @Override
    public String apply(HealthDataRequest request) {
        log.info("Function called with: {}", request);
        
        try {
            HealthLog log = healthLogService.save(request);
            log.info("HealthLog saved: {}", log.getId());
            return "Sucesso!";
        } catch (Exception e) {
            log.error("Error saving health data", e);
            return "Erro ao salvar dados";
        }
    }
}
```

---

## 📈 Boas Práticas

### ✅ **O QUE FAZER:**

1. ✅ Usar `@JsonPropertyDescription` detalhadas
2. ✅ Validar dados antes de salvar
3. ✅ Retornar mensagens claras
4. ✅ Logar execuções
5. ✅ Tratar erros gracefully

### ❌ **O QUE NÃO FAZER:**

1. ❌ Functions com side effects complexos
2. ❌ Functions que demoram muito (>5s)
3. ❌ Retornar objetos complexos (apenas String)
4. ❌ Esquecer de limpar contexto (ThreadLocal)

---

## 🎯 Próximos Passos

1. 📝 [Prompts](03-prompts.md)
2. 📊 [Extração de Dados](04-data-extraction.md)
3. 💬 [WhatsApp Integration](../07-whatsapp/01-evolution-api-setup.md)

---

[⬅️ Anterior: Spring AI Setup](01-spring-ai-setup.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Prompts](03-prompts.md)

