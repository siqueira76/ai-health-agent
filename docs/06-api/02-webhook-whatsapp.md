# 6.2 Webhook WhatsApp

## 🔗 Recebendo Mensagens do WhatsApp

O webhook recebe mensagens enviadas pelos pacientes via WhatsApp através da Evolution API.

---

## 📋 Fluxo Completo

```
1. Paciente envia mensagem no WhatsApp
   ↓
2. Evolution API recebe a mensagem
   ↓
3. Evolution API envia POST para /webhook/whatsapp
   ↓
4. WebhookController processa
   ↓
5. WhatsAppMessageService orquestra
   ↓
6. AiConversationService gera resposta
   ↓
7. WhatsAppService envia resposta
   ↓
8. Paciente recebe resposta no WhatsApp
```

---

## 🎯 Endpoint do Webhook

### **WebhookController.java:**

```java
@RestController
@RequestMapping("/webhook")
@Slf4j
public class WebhookController {
    
    private final WhatsAppMessageService messageService;
    
    @PostMapping("/whatsapp")
    public ResponseEntity<String> receiveWhatsAppMessage(
            @RequestBody WebhookPayload payload,
            @RequestHeader(value = "X-Evolution-Key", required = false) String apiKey) {
        
        try {
            log.info("Received WhatsApp webhook: {}", payload);
            
            // Validar API key (segurança)
            if (!isValidApiKey(apiKey)) {
                log.warn("Invalid API key received");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid API key");
            }
            
            // Processar mensagem de forma assíncrona
            messageService.processMessageAsync(payload);
            
            return ResponseEntity.ok("Message received");
            
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing message");
        }
    }
    
    private boolean isValidApiKey(String apiKey) {
        String expectedKey = System.getenv("EVOLUTION_WEBHOOK_KEY");
        return expectedKey != null && expectedKey.equals(apiKey);
    }
}
```

---

## 📦 Payload do Webhook

### **WebhookPayload.java:**

```java
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayload {
    
    @JsonProperty("event")
    private String event; // "messages.upsert"
    
    @JsonProperty("instance")
    private String instance; // Nome da instância
    
    @JsonProperty("data")
    private MessageData data;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageData {
        
        @JsonProperty("key")
        private MessageKey key;
        
        @JsonProperty("message")
        private Message message;
        
        @JsonProperty("messageTimestamp")
        private Long messageTimestamp;
        
        @JsonProperty("pushName")
        private String pushName; // Nome do contato
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageKey {
        
        @JsonProperty("remoteJid")
        private String remoteJid; // Número do WhatsApp (5511999999999@s.whatsapp.net)
        
        @JsonProperty("fromMe")
        private Boolean fromMe;
        
        @JsonProperty("id")
        private String id; // ID único da mensagem
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        
        @JsonProperty("conversation")
        private String conversation; // Texto da mensagem
        
        @JsonProperty("extendedTextMessage")
        private ExtendedTextMessage extendedTextMessage;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtendedTextMessage {
        
        @JsonProperty("text")
        private String text;
    }
    
    // Métodos auxiliares
    public String getPhoneNumber() {
        if (data == null || data.getKey() == null) return null;
        String remoteJid = data.getKey().getRemoteJid();
        return remoteJid != null ? remoteJid.split("@")[0] : null;
    }
    
    public String getMessageText() {
        if (data == null || data.getMessage() == null) return null;
        
        Message msg = data.getMessage();
        
        // Mensagem simples
        if (msg.getConversation() != null) {
            return msg.getConversation();
        }
        
        // Mensagem estendida (resposta, etc)
        if (msg.getExtendedTextMessage() != null) {
            return msg.getExtendedTextMessage().getText();
        }
        
        return null;
    }
    
    public boolean isFromMe() {
        return data != null && 
               data.getKey() != null && 
               Boolean.TRUE.equals(data.getKey().getFromMe());
    }
}
```

---

## 🔄 Processamento da Mensagem

### **WhatsAppMessageService.java:**

```java
@Service
@Slf4j
public class WhatsAppMessageService {
    
    private final PatientRepository patientRepository;
    private final AiConversationService aiService;
    private final WhatsAppService whatsAppService;
    private final ChatMessageRepository chatMessageRepository;
    
    @Async
    public void processMessageAsync(WebhookPayload payload) {
        try {
            processMessage(payload);
        } catch (Exception e) {
            log.error("Error processing message async", e);
        }
    }
    
    @Transactional
    public void processMessage(WebhookPayload payload) {
        // 1. Validar payload
        if (payload.isFromMe()) {
            log.debug("Ignoring message from bot");
            return;
        }
        
        String phoneNumber = payload.getPhoneNumber();
        String messageText = payload.getMessageText();
        
        if (phoneNumber == null || messageText == null) {
            log.warn("Invalid payload: missing phone or message");
            return;
        }
        
        log.info("Processing message from {}: {}", phoneNumber, messageText);
        
        // 2. Buscar ou criar paciente
        Patient patient = patientRepository.findByWhatsappNumber(phoneNumber)
            .orElseGet(() -> createNewPatient(phoneNumber, payload.getData().getPushName()));
        
        // 3. Salvar mensagem do paciente
        saveChatMessage(patient, messageText, MessageSender.PATIENT);
        
        // 4. Gerar resposta da IA
        String aiResponse = aiService.chat(patient, messageText);
        
        // 5. Salvar resposta da IA
        saveChatMessage(patient, aiResponse, MessageSender.AI);
        
        // 6. Enviar resposta via WhatsApp
        whatsAppService.sendMessage(phoneNumber, aiResponse);
        
        log.info("Message processed successfully for patient {}", patient.getId());
    }
    
    private Patient createNewPatient(String phoneNumber, String name) {
        // Buscar conta padrão ou criar nova
        Account account = accountRepository.findDefaultAccount()
            .orElseGet(() -> createDefaultAccount());
        
        Patient patient = Patient.builder()
            .account(account)
            .name(name != null ? name : "Paciente " + phoneNumber)
            .whatsappNumber(phoneNumber)
            .isActive(true)
            .build();
        
        Patient saved = patientRepository.save(patient);
        log.info("New patient created: {} ({})", saved.getName(), phoneNumber);
        
        // Enviar mensagem de boas-vindas
        whatsAppService.sendMessage(phoneNumber, getWelcomeMessage());
        
        return saved;
    }
    
    private void saveChatMessage(Patient patient, String text, MessageSender sender) {
        ChatMessage message = ChatMessage.builder()
            .account(patient.getAccount())
            .patient(patient)
            .messageText(text)
            .sender(sender)
            .build();
        
        chatMessageRepository.save(message);
    }
    
    private String getWelcomeMessage() {
        return """
            Olá! 👋 Bem-vindo ao HealthBot!
            
            Sou seu assistente de saúde virtual e estou aqui para ajudar você a 
            monitorar sua saúde de forma simples e natural.
            
            Você pode me contar como está se sentindo, e eu vou registrar 
            informações importantes sobre sua saúde.
            
            Como você está se sentindo hoje?
            """;
    }
}
```

---

## 📤 Enviando Mensagens

### **WhatsAppService.java:**

```java
@Service
@Slf4j
public class WhatsAppService {
    
    @Value("${evolution.api.url}")
    private String evolutionApiUrl;
    
    @Value("${evolution.api.key}")
    private String evolutionApiKey;
    
    @Value("${evolution.instance.name}")
    private String instanceName;
    
    private final RestTemplate restTemplate;
    
    public void sendMessage(String phoneNumber, String message) {
        try {
            String url = String.format("%s/message/sendText/%s", 
                evolutionApiUrl, instanceName);
            
            SendMessageRequest request = SendMessageRequest.builder()
                .number(phoneNumber)
                .text(message)
                .build();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", evolutionApiKey);
            
            HttpEntity<SendMessageRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Message sent successfully to {}", phoneNumber);
            } else {
                log.error("Failed to send message: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error sending WhatsApp message to {}", phoneNumber, e);
            throw new WhatsAppException("Failed to send message", e);
        }
    }
}

@Data
@Builder
class SendMessageRequest {
    private String number;
    private String text;
}
```

---

## 🔒 Segurança do Webhook

### **1. Validação de API Key:**

```java
@Component
public class WebhookSecurityFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        if (request.getRequestURI().startsWith("/webhook/")) {
            String apiKey = request.getHeader("X-Evolution-Key");
            String expectedKey = System.getenv("EVOLUTION_WEBHOOK_KEY");
            
            if (expectedKey == null || !expectedKey.equals(apiKey)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Unauthorized");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### **2. Rate Limiting:**

```java
@Component
public class WebhookRateLimiter {
    
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    public boolean allowRequest(String phoneNumber) {
        RateLimiter limiter = limiters.computeIfAbsent(
            phoneNumber, 
            k -> RateLimiter.create(10.0) // 10 mensagens por segundo
        );
        
        return limiter.tryAcquire();
    }
}
```

---

## 🎯 Próximos Passos

1. 💬 [Evolution API Setup](../07-whatsapp/01-evolution-api-setup.md)
2. 🔔 [Check-ins Proativos](../08-checkins/01-proactive-checkins.md)
3. 📊 [Analytics](../09-analytics/01-health-analytics.md)

---

[⬅️ Anterior: API Overview](01-api-overview.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Evolution API Setup](../07-whatsapp/01-evolution-api-setup.md)

