# 10.2 Testes de Integração

## 🔗 Testando Integração entre Componentes

Testes de integração validam que diferentes partes do sistema funcionam juntas corretamente.

---

## 🎯 O que Testar

- ✅ Fluxo completo de mensagem WhatsApp → AI → Banco
- ✅ Integração com banco de dados real
- ✅ Webhooks e APIs externas
- ✅ Transações e rollbacks
- ✅ Multi-tenancy

---

## 🔧 Configuração

### **application-test.properties:**

```properties
# Database H2 em memória
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Flyway desabilitado para testes
spring.flyway.enabled=false

# OpenAI Mock
spring.ai.openai.api-key=test-key
```

---

## 🧪 Teste de Fluxo Completo

### **WhatsAppMessageFlowTest.java:**

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WhatsAppMessageFlowTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private HealthLogRepository healthLogRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @MockBean
    private WhatsAppService whatsAppService;
    
    @MockBean
    private ChatClient chatClient;
    
    private Account account;
    
    @BeforeEach
    void setUp() {
        account = Account.builder()
            .name("Test Account")
            .status(AccountStatus.ACTIVE)
            .build();
    }
    
    @Test
    void shouldProcessWhatsAppMessageEndToEnd() throws Exception {
        // Given
        String webhookPayload = """
            {
                "event": "messages.upsert",
                "instance": "test-instance",
                "data": {
                    "key": {
                        "remoteJid": "5511999999999@s.whatsapp.net",
                        "fromMe": false,
                        "id": "msg123"
                    },
                    "message": {
                        "conversation": "Estou com dor de cabeça nível 8"
                    },
                    "pushName": "Test User"
                }
            }
            """;
        
        // Mock AI response
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(callResponseSpec.content())
            .thenReturn("Sinto muito que esteja com dor nível 8. Já tomou algum medicamento?");
        
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(chatClient.prompt()).thenReturn(requestSpec);
        
        // When
        mockMvc.perform(post("/webhook/whatsapp")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Evolution-Key", "test-key")
                .content(webhookPayload))
            .andExpect(status().isOk());
        
        // Then
        // 1. Paciente foi criado
        Optional<Patient> patient = patientRepository
            .findByWhatsappNumber("5511999999999");
        assertThat(patient).isPresent();
        assertThat(patient.get().getName()).isEqualTo("Test User");
        
        // 2. Mensagens foram salvas
        List<ChatMessage> messages = chatMessageRepository
            .findByPatient(patient.get().getId());
        assertThat(messages).hasSize(2); // User + AI
        
        // 3. WhatsApp service foi chamado
        verify(whatsAppService, times(2)) // Welcome + Response
            .sendMessage(eq("5511999999999"), anyString());
    }
}
```

---

## 🗄️ Teste com Banco Real

### **PatientIntegrationTest.java:**

```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PatientIntegrationTest {
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private HealthLogRepository healthLogRepository;
    
    @Test
    void shouldSavePatientWithHealthLogs() {
        // Given
        Account account = Account.builder()
            .name("Test Account")
            .status(AccountStatus.ACTIVE)
            .build();
        account = accountRepository.save(account);
        
        Patient patient = Patient.builder()
            .account(account)
            .name("John Doe")
            .whatsappNumber("5511999999999")
            .isActive(true)
            .build();
        patient = patientRepository.save(patient);
        
        // When
        HealthLog log1 = HealthLog.builder()
            .account(account)
            .patient(patient)
            .painLevel(7)
            .mood("ansioso")
            .build();
        
        HealthLog log2 = HealthLog.builder()
            .account(account)
            .patient(patient)
            .painLevel(5)
            .mood("calmo")
            .build();
        
        healthLogRepository.saveAll(Arrays.asList(log1, log2));
        
        // Then
        List<HealthLog> logs = healthLogRepository.findByPatient(patient.getId());
        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).getPainLevel()).isEqualTo(7);
        assertThat(logs.get(1).getPainLevel()).isEqualTo(5);
    }
    
    @Test
    void shouldCascadeDeleteHealthLogs() {
        // Given
        Account account = accountRepository.save(
            Account.builder().name("Test").status(AccountStatus.ACTIVE).build()
        );
        
        Patient patient = patientRepository.save(
            Patient.builder()
                .account(account)
                .name("Test")
                .whatsappNumber("5511999999999")
                .isActive(true)
                .build()
        );
        
        healthLogRepository.save(
            HealthLog.builder()
                .account(account)
                .patient(patient)
                .painLevel(5)
                .build()
        );
        
        // When
        patientRepository.delete(patient);
        
        // Then
        List<HealthLog> logs = healthLogRepository.findByPatient(patient.getId());
        assertThat(logs).isEmpty();
    }
}
```

---

## 🔒 Teste de Multi-Tenancy

### **MultiTenancyIntegrationTest.java:**

```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MultiTenancyIntegrationTest {
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Test
    void shouldIsolateDataByTenant() {
        // Given - Criar 2 accounts
        Account account1 = accountRepository.save(
            Account.builder().name("Account 1").status(AccountStatus.ACTIVE).build()
        );
        
        Account account2 = accountRepository.save(
            Account.builder().name("Account 2").status(AccountStatus.ACTIVE).build()
        );
        
        // Criar pacientes em cada account
        Patient patient1 = patientRepository.save(
            Patient.builder()
                .account(account1)
                .name("Patient 1")
                .whatsappNumber("5511111111111")
                .isActive(true)
                .build()
        );
        
        Patient patient2 = patientRepository.save(
            Patient.builder()
                .account(account2)
                .name("Patient 2")
                .whatsappNumber("5511222222222")
                .isActive(true)
                .build()
        );
        
        // When - Buscar por account
        List<Patient> account1Patients = patientRepository.findByAccount(account1.getId());
        List<Patient> account2Patients = patientRepository.findByAccount(account2.getId());
        
        // Then - Dados isolados
        assertThat(account1Patients).hasSize(1);
        assertThat(account1Patients.get(0).getName()).isEqualTo("Patient 1");
        
        assertThat(account2Patients).hasSize(1);
        assertThat(account2Patients.get(0).getName()).isEqualTo("Patient 2");
    }
}
```

---

## 🔄 Teste de Scheduler

### **CheckinSchedulerIntegrationTest.java:**

```java
@SpringBootTest
@ActiveProfiles("test")
class CheckinSchedulerIntegrationTest {
    
    @Autowired
    private CheckinSchedulerService schedulerService;
    
    @Autowired
    private CheckinScheduleRepository scheduleRepository;
    
    @Autowired
    private CheckinExecutionRepository executionRepository;
    
    @Autowired
    private PatientRepository patientRepository;
    
    @MockBean
    private WhatsAppService whatsAppService;
    
    @Test
    void shouldExecuteScheduledCheckins() {
        // Given
        Patient patient = patientRepository.save(
            Patient.builder()
                .name("Test")
                .whatsappNumber("5511999999999")
                .isActive(true)
                .build()
        );
        
        CheckinSchedule schedule = scheduleRepository.save(
            CheckinSchedule.builder()
                .patient(patient)
                .frequency(CheckinFrequency.DAILY)
                .timeOfDay(LocalTime.now())
                .isActive(true)
                .build()
        );
        
        // When
        schedulerService.executeCheckins();
        
        // Then
        List<CheckinExecution> executions = executionRepository
            .findBySchedule(schedule.getId());
        
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        
        verify(whatsAppService, times(1))
            .sendMessage(eq("5511999999999"), anyString());
    }
}
```

---

## ▶️ Executando Testes de Integração

### **Todos os testes:**

```bash
mvn verify
```

### **Apenas testes de integração:**

```bash
mvn test -Dtest=*IntegrationTest
```

### **Com perfil específico:**

```bash
mvn test -Dspring.profiles.active=test
```

---

## 📊 Cobertura de Código

### **Configurar JaCoCo:**

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### **Gerar relatório:**

```bash
mvn clean test jacoco:report
```

Abrir: `target/site/jacoco/index.html`

---

## 🎯 Próximos Passos

1. 🚀 [Deploy Railway](../11-deployment/01-railway-deploy.md)
2. 🐳 [Deploy Docker](../11-deployment/02-docker-deploy.md)
3. 📚 [Referências](../13-references/01-glossary.md)

---

[⬅️ Anterior: Testes Unitários](01-unit-tests.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Deploy Railway](../11-deployment/01-railway-deploy.md)

