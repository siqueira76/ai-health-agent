# 4.3 Design Patterns

## 🎨 Padrões de Projeto Utilizados

O AI Health Agent implementa diversos design patterns para garantir código limpo, manutenível e escalável.

---

## 1️⃣ Repository Pattern

### **Descrição:**
Abstrai o acesso a dados, separando lógica de negócio da persistência.

### **Implementação:**

```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    
    Optional<Patient> findByWhatsappNumber(String whatsappNumber);
    
    @Query("SELECT p FROM Patient p WHERE p.account.id = :accountId")
    List<Patient> findByAccount(@Param("accountId") UUID accountId);
}
```

### **Benefícios:**
- ✅ Testabilidade (fácil mockar)
- ✅ Troca de banco de dados sem impacto
- ✅ Queries centralizadas

---

## 2️⃣ Service Layer Pattern

### **Descrição:**
Encapsula lógica de negócio, orquestrando múltiplos repositórios.

### **Implementação:**

```java
@Service
@Transactional
public class WhatsAppMessageService {
    
    private final PatientRepository patientRepository;
    private final AiConversationService aiService;
    private final ChatMessageRepository chatRepository;
    
    public void processMessage(WebhookPayload payload) {
        Patient patient = patientRepository.findByWhatsappNumber(payload.getPhone())
            .orElseThrow(() -> new PatientNotFoundException());
        
        String aiResponse = aiService.chat(patient, payload.getMessage());
        
        chatRepository.save(new ChatMessage(patient, payload.getMessage(), MessageSender.PATIENT));
        chatRepository.save(new ChatMessage(patient, aiResponse, MessageSender.AI));
    }
}
```

### **Benefícios:**
- ✅ Lógica de negócio centralizada
- ✅ Transações gerenciadas
- ✅ Reutilização de código

---

## 3️⃣ DTO Pattern (Data Transfer Object)

### **Descrição:**
Objetos para transferir dados entre camadas, evitando expor entidades.

### **Implementação:**

```java
@Data
@Builder
public class PatientDTO {
    private UUID id;
    private String name;
    private String whatsappNumber;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    public static PatientDTO fromEntity(Patient patient) {
        return PatientDTO.builder()
            .id(patient.getId())
            .name(patient.getName())
            .whatsappNumber(patient.getWhatsappNumber())
            .isActive(patient.getIsActive())
            .createdAt(patient.getCreatedAt())
            .build();
    }
}
```

### **Benefícios:**
- ✅ Controle sobre dados expostos
- ✅ Evita lazy loading exceptions
- ✅ Versionamento de API facilitado

---

## 4️⃣ Builder Pattern

### **Descrição:**
Construção fluente de objetos complexos.

### **Implementação:**

```java
@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthLog {
    private UUID id;
    private Patient patient;
    private Integer painLevel;
    private String mood;
    private String sleepQuality;
}

// Uso
HealthLog log = HealthLog.builder()
    .patient(patient)
    .painLevel(7)
    .mood("ansioso")
    .sleepQuality("ruim")
    .build();
```

### **Benefícios:**
- ✅ Código legível
- ✅ Parâmetros opcionais
- ✅ Imutabilidade (com @Builder)

---

## 5️⃣ Strategy Pattern

### **Descrição:**
Define família de algoritmos intercambiáveis.

### **Implementação:**

```java
public interface AlertStrategy {
    boolean shouldTrigger(Patient patient, HealthLog log);
    Alert createAlert(Patient patient, HealthLog log);
}

@Component
public class HighPainAlertStrategy implements AlertStrategy {
    
    @Override
    public boolean shouldTrigger(Patient patient, HealthLog log) {
        return log.getPainLevel() != null && log.getPainLevel() >= 8;
    }
    
    @Override
    public Alert createAlert(Patient patient, HealthLog log) {
        return Alert.builder()
            .patient(patient)
            .alertType(AlertType.CRISIS)
            .severity(AlertSeverity.HIGH)
            .message("Dor intensa detectada: nível " + log.getPainLevel())
            .build();
    }
}

@Service
public class AlertService {
    
    private final List<AlertStrategy> strategies;
    
    public void evaluateAlerts(Patient patient, HealthLog log) {
        strategies.stream()
            .filter(strategy -> strategy.shouldTrigger(patient, log))
            .map(strategy -> strategy.createAlert(patient, log))
            .forEach(alertRepository::save);
    }
}
```

### **Benefícios:**
- ✅ Fácil adicionar novos tipos de alerta
- ✅ Código desacoplado
- ✅ Testável individualmente

---

## 6️⃣ Factory Pattern

### **Descrição:**
Criação de objetos sem expor lógica de criação.

### **Implementação:**

```java
@Component
public class CheckinMessageFactory {
    
    public String createMessage(CheckinSchedule schedule) {
        return switch (schedule.getFrequency()) {
            case DAILY -> "Olá! Como você está se sentindo hoje?";
            case WEEKLY -> "Olá! Como foi sua semana?";
            case CUSTOM -> schedule.getCustomMessage();
            default -> "Olá! Tudo bem?";
        };
    }
}
```

---

## 7️⃣ Observer Pattern (Spring Events)

### **Descrição:**
Notificação automática de mudanças de estado.

### **Implementação:**

```java
// Evento
public class HealthLogCreatedEvent extends ApplicationEvent {
    private final HealthLog healthLog;
    
    public HealthLogCreatedEvent(Object source, HealthLog healthLog) {
        super(source);
        this.healthLog = healthLog;
    }
}

// Publisher
@Service
public class HealthLogService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public HealthLog save(HealthLog log) {
        HealthLog saved = repository.save(log);
        eventPublisher.publishEvent(new HealthLogCreatedEvent(this, saved));
        return saved;
    }
}

// Listener
@Component
public class AlertListener {
    
    @EventListener
    public void onHealthLogCreated(HealthLogCreatedEvent event) {
        HealthLog log = event.getHealthLog();
        alertService.evaluateAlerts(log.getPatient(), log);
    }
}
```

### **Benefícios:**
- ✅ Desacoplamento
- ✅ Fácil adicionar novos listeners
- ✅ Processamento assíncrono (com @Async)

---

## 8️⃣ Template Method Pattern

### **Descrição:**
Define esqueleto de algoritmo, delegando passos para subclasses.

### **Implementação:**

```java
public abstract class BaseMessageProcessor {
    
    public final void processMessage(WebhookPayload payload) {
        validate(payload);
        Patient patient = findPatient(payload);
        String response = generateResponse(patient, payload);
        sendResponse(patient, response);
        logInteraction(patient, payload, response);
    }
    
    protected abstract void validate(WebhookPayload payload);
    protected abstract String generateResponse(Patient patient, WebhookPayload payload);
    
    protected Patient findPatient(WebhookPayload payload) {
        return patientRepository.findByWhatsappNumber(payload.getPhone())
            .orElseThrow(() -> new PatientNotFoundException());
    }
}
```

---

## 9️⃣ Singleton Pattern (Spring Beans)

### **Descrição:**
Garante única instância de uma classe.

### **Implementação:**

```java
@Component // Singleton por padrão no Spring
public class OpenAiClient {
    
    private final RestTemplate restTemplate;
    private final String apiKey;
    
    // Única instância gerenciada pelo Spring
}
```

---

## 🔟 Dependency Injection Pattern

### **Descrição:**
Inversão de controle via injeção de dependências.

### **Implementação:**

```java
@Service
public class WhatsAppMessageService {
    
    private final PatientRepository patientRepository;
    private final AiConversationService aiService;
    
    // Constructor injection (recomendado)
    @Autowired
    public WhatsAppMessageService(
        PatientRepository patientRepository,
        AiConversationService aiService
    ) {
        this.patientRepository = patientRepository;
        this.aiService = aiService;
    }
}
```

### **Benefícios:**
- ✅ Testabilidade (fácil mockar)
- ✅ Baixo acoplamento
- ✅ Configuração centralizada

---

## 1️⃣1️⃣ Aspect-Oriented Programming (AOP)

### **Descrição:**
Separação de cross-cutting concerns (logging, segurança, transações).

### **Implementação:**

```java
@Aspect
@Component
public class LoggingAspect {
    
    @Around("execution(* com.healthlink..service.*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        
        Object result = joinPoint.proceed();
        
        long executionTime = System.currentTimeMillis() - start;
        log.info("{} executed in {}ms", joinPoint.getSignature(), executionTime);
        
        return result;
    }
}
```

---

## 📊 Resumo dos Patterns

| Pattern | Onde Usar | Benefício Principal |
|---------|-----------|---------------------|
| Repository | Acesso a dados | Abstração de persistência |
| Service Layer | Lógica de negócio | Orquestração |
| DTO | Transferência de dados | Controle de exposição |
| Builder | Criação de objetos | Legibilidade |
| Strategy | Algoritmos variáveis | Extensibilidade |
| Factory | Criação complexa | Encapsulamento |
| Observer | Eventos | Desacoplamento |
| Template Method | Fluxos similares | Reutilização |
| Singleton | Recursos compartilhados | Economia de memória |
| Dependency Injection | Toda a aplicação | Testabilidade |
| AOP | Cross-cutting concerns | Separação de responsabilidades |

---

## 🎯 Próximos Passos

1. 🔒 [Segurança](04-security.md)
2. 🤖 [Spring AI Setup](../05-ai/01-spring-ai-setup.md)
3. 💬 [WhatsApp Integration](../07-whatsapp/01-evolution-api-setup.md)

---

[⬅️ Anterior: Multi-Tenancy](02-multi-tenancy.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Segurança](04-security.md)

