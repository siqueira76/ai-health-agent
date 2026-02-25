# 4.1 Arquitetura em Camadas

## 🏗️ Visão Geral

O AI Health Agent segue uma **arquitetura em camadas** (Layered Architecture) baseada em princípios de Clean Architecture e Domain-Driven Design (DDD).

---

## 📊 Diagrama de Camadas

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│  (Controllers, DTOs, Request/Response Handlers)         │
│                                                          │
│  - WhatsAppWebhookController                            │
│  - DashboardController                                  │
│  - PatientController                                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                    SERVICE LAYER                         │
│  (Business Logic, Orchestration, Use Cases)             │
│                                                          │
│  - WhatsAppMessageService                               │
│  - AiConversationService                                │
│  - CheckinService                                       │
│  - AlertService                                         │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   DOMAIN LAYER                           │
│  (Entities, Value Objects, Domain Logic)                │
│                                                          │
│  - Patient, Account, HealthLog                          │
│  - Alert, ChatMessage                                   │
│  - Business Rules                                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                 INFRASTRUCTURE LAYER                     │
│  (Repositories, External APIs, Database)                │
│                                                          │
│  - PatientRepository                                    │
│  - HealthLogRepository                                  │
│  - EvolutionApiClient                                   │
│  - OpenAI Integration (Spring AI)                       │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 Camada de Apresentação (Presentation)

### **Responsabilidades:**
- Receber requisições HTTP
- Validar entrada
- Converter DTOs ↔ Entidades
- Retornar respostas formatadas

### **Componentes:**

```java
@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {
    
    private final WhatsAppMessageService messageService;
    
    @PostMapping("/whatsapp")
    public ResponseEntity<Void> handleWebhook(@RequestBody WebhookPayload payload) {
        messageService.processIncomingMessage(payload);
        return ResponseEntity.ok().build();
    }
}
```

### **Pacotes:**
```
com.healthlink.ai_health_agent.controller/
├── WhatsAppWebhookController.java
├── DashboardController.java
├── PatientController.java
└── dto/
    ├── PatientDTO.java
    ├── HealthLogDTO.java
    └── WebhookPayload.java
```

---

## 💼 Camada de Serviço (Service)

### **Responsabilidades:**
- Implementar lógica de negócio
- Orquestrar múltiplos repositórios
- Gerenciar transações
- Chamar APIs externas

### **Componentes:**

```java
@Service
@Transactional
public class WhatsAppMessageService {
    
    private final PatientRepository patientRepository;
    private final AiConversationService aiService;
    private final HealthLogRepository healthLogRepository;
    
    public void processIncomingMessage(WebhookPayload payload) {
        // 1. Buscar paciente
        Patient patient = patientRepository.findByWhatsappNumber(payload.getPhone())
            .orElseThrow(() -> new PatientNotFoundException());
        
        // 2. Processar com IA
        String aiResponse = aiService.chat(patient, payload.getMessage());
        
        // 3. Salvar mensagens
        saveChatMessages(patient, payload.getMessage(), aiResponse);
        
        // 4. Enviar resposta
        sendWhatsAppMessage(patient.getWhatsappNumber(), aiResponse);
    }
}
```

### **Pacotes:**
```
com.healthlink.ai_health_agent.service/
├── WhatsAppMessageService.java
├── AiConversationService.java
├── CheckinService.java
├── AlertService.java
└── HealthLogService.java
```

---

## 🏛️ Camada de Domínio (Domain)

### **Responsabilidades:**
- Definir entidades e regras de negócio
- Encapsular lógica de domínio
- Validações de negócio

### **Componentes:**

```java
@Entity
@Table(name = "patients")
public class Patient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;
    private String whatsappNumber;
    private Boolean isActive;
    
    // Business logic
    public boolean canReceiveCheckin() {
        return isActive && account.isActive();
    }
    
    public void deactivate() {
        if (!healthLogs.isEmpty()) {
            throw new BusinessException("Cannot deactivate patient with health logs");
        }
        this.isActive = false;
    }
}
```

### **Pacotes:**
```
com.healthlink.ai_health_agent.domain/
├── entity/
│   ├── Patient.java
│   ├── Account.java
│   ├── HealthLog.java
│   └── Alert.java
├── enums/
│   ├── AccountStatus.java
│   ├── AlertSeverity.java
│   └── MessageSender.java
└── exception/
    ├── PatientNotFoundException.java
    └── BusinessException.java
```

---

## 🔧 Camada de Infraestrutura (Infrastructure)

### **Responsabilidades:**
- Acesso a dados (Repositories)
- Integração com APIs externas
- Configurações técnicas

### **Componentes:**

```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    
    Optional<Patient> findByWhatsappNumber(String whatsappNumber);
    
    @Query("SELECT p FROM Patient p WHERE p.account.id = :accountId AND p.isActive = true")
    List<Patient> findActiveByAccount(@Param("accountId") UUID accountId);
}
```

```java
@Component
public class EvolutionApiClient {
    
    private final RestTemplate restTemplate;
    
    public void sendMessage(String phone, String message) {
        // Chamada HTTP para Evolution API
    }
}
```

### **Pacotes:**
```
com.healthlink.ai_health_agent/
├── repository/
│   ├── PatientRepository.java
│   ├── HealthLogRepository.java
│   └── AlertRepository.java
├── integration/
│   ├── EvolutionApiClient.java
│   └── OpenAiClient.java
└── config/
    ├── DatabaseConfig.java
    ├── SecurityConfig.java
    └── OpenAiConfig.java
```

---

## 🔄 Fluxo de Dados

### **Exemplo: Receber mensagem do WhatsApp**

```
1. WhatsAppWebhookController (Presentation)
   ↓ recebe WebhookPayload
   
2. WhatsAppMessageService (Service)
   ↓ busca Patient via PatientRepository (Infrastructure)
   ↓ processa com AiConversationService (Service)
   ↓ salva HealthLog via HealthLogRepository (Infrastructure)
   ↓ envia resposta via EvolutionApiClient (Infrastructure)
   
3. Retorna ResponseEntity (Presentation)
```

---

## ✅ Benefícios da Arquitetura

1. **Separação de Responsabilidades** - Cada camada tem um propósito claro
2. **Testabilidade** - Fácil mockar dependências
3. **Manutenibilidade** - Mudanças isoladas em cada camada
4. **Escalabilidade** - Fácil adicionar novos recursos
5. **Reutilização** - Services podem ser usados por múltiplos controllers

---

## 🎯 Próximos Passos

1. 🔐 [Multi-Tenancy](02-multi-tenancy.md)
2. 🎨 [Design Patterns](03-design-patterns.md)
3. 🔒 [Segurança](04-security.md)

---

[⬅️ Anterior: Relacionamentos](../03-database/04-relationships.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Multi-Tenancy](02-multi-tenancy.md)

