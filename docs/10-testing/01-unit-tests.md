# 10.1 Testes Unitários

## 🧪 Testando a Aplicação

Testes garantem qualidade, confiabilidade e facilitam refatoração.

---

## 📦 Dependências

### **pom.xml:**

```xml
<dependencies>
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- H2 Database (para testes) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 🎯 Testando Services

### **HealthLogServiceTest.java:**

```java
@ExtendWith(MockitoExtension.class)
class HealthLogServiceTest {
    
    @Mock
    private HealthLogRepository healthLogRepository;
    
    @Mock
    private AlertService alertService;
    
    @InjectMocks
    private HealthLogService healthLogService;
    
    private Patient patient;
    private Account account;
    
    @BeforeEach
    void setUp() {
        account = Account.builder()
            .id(UUID.randomUUID())
            .name("Test Account")
            .status(AccountStatus.ACTIVE)
            .build();
        
        patient = Patient.builder()
            .id(UUID.randomUUID())
            .account(account)
            .name("Test Patient")
            .whatsappNumber("5511999999999")
            .isActive(true)
            .build();
    }
    
    @Test
    void shouldSaveHealthLog() {
        // Given
        HealthLog healthLog = HealthLog.builder()
            .account(account)
            .patient(patient)
            .painLevel(7)
            .mood("ansioso")
            .build();
        
        when(healthLogRepository.save(any(HealthLog.class)))
            .thenReturn(healthLog);
        
        // When
        HealthLog saved = healthLogService.save(healthLog);
        
        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getPainLevel()).isEqualTo(7);
        assertThat(saved.getMood()).isEqualTo("ansioso");
        
        verify(healthLogRepository, times(1)).save(healthLog);
        verify(alertService, times(1)).evaluateAlerts(patient, healthLog);
    }
    
    @Test
    void shouldFindByPatient() {
        // Given
        List<HealthLog> logs = Arrays.asList(
            HealthLog.builder().painLevel(5).build(),
            HealthLog.builder().painLevel(7).build()
        );
        
        when(healthLogRepository.findByPatient(patient.getId()))
            .thenReturn(logs);
        
        // When
        List<HealthLog> result = healthLogService.findByPatient(patient.getId());
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPainLevel()).isEqualTo(5);
        assertThat(result.get(1).getPainLevel()).isEqualTo(7);
    }
}
```

---

## 🔧 Testando Repositories

### **PatientRepositoryTest.java:**

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PatientRepositoryTest {
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    private Account account;
    
    @BeforeEach
    void setUp() {
        account = Account.builder()
            .name("Test Account")
            .status(AccountStatus.ACTIVE)
            .build();
        account = accountRepository.save(account);
    }
    
    @Test
    void shouldFindByWhatsappNumber() {
        // Given
        Patient patient = Patient.builder()
            .account(account)
            .name("John Doe")
            .whatsappNumber("5511999999999")
            .isActive(true)
            .build();
        patientRepository.save(patient);
        
        // When
        Optional<Patient> found = patientRepository
            .findByWhatsappNumber("5511999999999");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
    }
    
    @Test
    void shouldReturnEmptyWhenNotFound() {
        // When
        Optional<Patient> found = patientRepository
            .findByWhatsappNumber("9999999999");
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldFindByAccount() {
        // Given
        Patient patient1 = Patient.builder()
            .account(account)
            .name("Patient 1")
            .whatsappNumber("5511111111111")
            .isActive(true)
            .build();
        
        Patient patient2 = Patient.builder()
            .account(account)
            .name("Patient 2")
            .whatsappNumber("5511222222222")
            .isActive(true)
            .build();
        
        patientRepository.saveAll(Arrays.asList(patient1, patient2));
        
        // When
        List<Patient> patients = patientRepository.findByAccount(account.getId());
        
        // Then
        assertThat(patients).hasSize(2);
    }
}
```

---

## 🌐 Testando Controllers

### **PatientControllerTest.java:**

```java
@WebMvcTest(PatientController.class)
class PatientControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PatientService patientService;
    
    @Test
    void shouldGetPatient() throws Exception {
        // Given
        UUID patientId = UUID.randomUUID();
        PatientDTO patientDTO = PatientDTO.builder()
            .id(patientId)
            .name("John Doe")
            .whatsappNumber("5511999999999")
            .isActive(true)
            .build();
        
        when(patientService.findById(patientId)).thenReturn(patientDTO);
        
        // When & Then
        mockMvc.perform(get("/api/patients/{id}", patientId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(patientId.toString()))
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.whatsappNumber").value("5511999999999"));
    }
    
    @Test
    void shouldCreatePatient() throws Exception {
        // Given
        PatientDTO patientDTO = PatientDTO.builder()
            .id(UUID.randomUUID())
            .name("Jane Doe")
            .whatsappNumber("5511888888888")
            .isActive(true)
            .build();
        
        when(patientService.create(any())).thenReturn(patientDTO);
        
        String requestBody = """
            {
                "name": "Jane Doe",
                "whatsappNumber": "5511888888888"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Jane Doe"));
    }
}
```

---

## 🤖 Testando AI Service

### **AiConversationServiceTest.java:**

```java
@ExtendWith(MockitoExtension.class)
class AiConversationServiceTest {
    
    @Mock
    private ChatClient chatClient;
    
    @Mock
    private ChatMessageRepository chatMessageRepository;
    
    @InjectMocks
    private AiConversationService aiService;
    
    @Test
    void shouldGenerateResponse() {
        // Given
        Patient patient = Patient.builder()
            .id(UUID.randomUUID())
            .name("Test Patient")
            .build();
        
        String userMessage = "Estou com dor de cabeça";
        String expectedResponse = "Sinto muito que esteja com dor. Em uma escala de 0 a 10, qual o nível?";
        
        // Mock ChatClient
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(callResponseSpec.content()).thenReturn(expectedResponse);
        
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        
        when(chatClient.prompt()).thenReturn(requestSpec);
        
        // When
        String response = aiService.chat(patient, userMessage);
        
        // Then
        assertThat(response).isEqualTo(expectedResponse);
    }
}
```

---

## 🔄 Testando Scheduler

### **CheckinSchedulerServiceTest.java:**

```java
@ExtendWith(MockitoExtension.class)
class CheckinSchedulerServiceTest {
    
    @Mock
    private CheckinScheduleRepository scheduleRepository;
    
    @Mock
    private CheckinExecutionRepository executionRepository;
    
    @Mock
    private WhatsAppService whatsAppService;
    
    @InjectMocks
    private CheckinSchedulerService schedulerService;
    
    @Test
    void shouldExecuteCheckin() {
        // Given
        Patient patient = Patient.builder()
            .id(UUID.randomUUID())
            .whatsappNumber("5511999999999")
            .build();
        
        CheckinSchedule schedule = CheckinSchedule.builder()
            .id(UUID.randomUUID())
            .patient(patient)
            .frequency(CheckinFrequency.DAILY)
            .timeOfDay(LocalTime.of(9, 0))
            .isActive(true)
            .build();
        
        when(scheduleRepository.findActiveSchedulesForTime(any()))
            .thenReturn(Arrays.asList(schedule));
        
        when(executionRepository.existsByScheduleAndExecutedAtAfter(any(), any()))
            .thenReturn(false);
        
        // When
        schedulerService.executeCheckins();
        
        // Then
        verify(whatsAppService, times(1))
            .sendMessage(eq("5511999999999"), anyString());
        
        verify(executionRepository, times(1))
            .save(any(CheckinExecution.class));
    }
}
```

---

## ▶️ Executando Testes

### **Todos os testes:**

```bash
mvn test
```

### **Testes específicos:**

```bash
mvn test -Dtest=PatientServiceTest
```

### **Com cobertura:**

```bash
mvn test jacoco:report
```

Relatório em: `target/site/jacoco/index.html`

---

## 🎯 Próximos Passos

1. 🧪 [Testes de Integração](02-integration-tests.md)
2. 🚀 [Deploy](../11-deployment/01-railway-deploy.md)
3. 📚 [Referências](../13-references/01-glossary.md)

---

[⬅️ Anterior: Analytics](../09-analytics/01-health-analytics.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Testes de Integração](02-integration-tests.md)

