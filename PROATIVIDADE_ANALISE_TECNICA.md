# 🤖 Sistema de Mensagens Proativas - Análise Técnica Completa

## 📋 Sumário Executivo

Este documento apresenta a arquitetura técnica completa para implementar **mensagens proativas** (check-ins automáticos) no AI Health Agent, considerando:
- ✅ Multi-tenancy e isolamento de dados
- ✅ Escalabilidade horizontal (múltiplas instâncias)
- ✅ Personalização por tenant (B2B vs B2C)
- ✅ Integração com IA (custom prompts)
- ✅ Rate limiting e controle de custos

---

## 🎯 1. MODELO DE DADOS

### **1.1. Tabela: `checkin_schedules`**

**Justificativa:**
- Separação de responsabilidades (SRP)
- Flexibilidade para múltiplos cronogramas por paciente
- Histórico de execuções
- Facilita queries de agendamento

```sql
CREATE TABLE checkin_schedules (
    -- Identificação
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Multi-tenancy
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    
    -- Configuração do Agendamento
    schedule_type VARCHAR(50) NOT NULL, -- 'DAILY', 'WEEKLY', 'CUSTOM'
    time_of_day TIME NOT NULL,          -- Ex: '09:00:00', '20:00:00'
    days_of_week INTEGER[],             -- [1,2,3,4,5] = Seg-Sex, NULL = todos os dias
    timezone VARCHAR(50) DEFAULT 'America/Sao_Paulo',
    
    -- Personalização da Mensagem
    custom_message TEXT,                -- Mensagem customizada (opcional)
    use_ai_generation BOOLEAN DEFAULT true, -- Se true, usa LLM para gerar mensagem
    
    -- Controle de Execução
    is_active BOOLEAN DEFAULT true,
    last_execution_at TIMESTAMP,
    next_execution_at TIMESTAMP,        -- Calculado automaticamente
    
    -- Rate Limiting
    max_messages_per_day INTEGER DEFAULT 3,
    messages_sent_today INTEGER DEFAULT 0,
    last_reset_date DATE DEFAULT CURRENT_DATE,
    
    -- Auditoria
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),            -- ID do psicólogo (B2B) ou 'system' (B2C)
    
    -- Índices para performance
    CONSTRAINT unique_patient_schedule UNIQUE (patient_id, schedule_type, time_of_day)
);

-- Índices
CREATE INDEX idx_checkin_schedules_account ON checkin_schedules(account_id);
CREATE INDEX idx_checkin_schedules_next_execution ON checkin_schedules(next_execution_at) 
    WHERE is_active = true;
CREATE INDEX idx_checkin_schedules_active ON checkin_schedules(is_active, next_execution_at);
```

### **1.2. Tabela: `checkin_executions` (Histórico)**

```sql
CREATE TABLE checkin_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Relacionamentos
    schedule_id UUID NOT NULL REFERENCES checkin_schedules(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    
    -- Detalhes da Execução
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,        -- 'SUCCESS', 'FAILED', 'SKIPPED'
    failure_reason TEXT,
    
    -- Mensagem Enviada
    message_sent TEXT,
    message_id VARCHAR(255),            -- ID da Evolution API
    
    -- Resposta do Paciente
    patient_responded BOOLEAN DEFAULT false,
    response_received_at TIMESTAMP,
    
    -- Auditoria
    execution_duration_ms INTEGER,
    
    -- Índices
    CONSTRAINT idx_executions_schedule FOREIGN KEY (schedule_id) REFERENCES checkin_schedules(id)
);

CREATE INDEX idx_checkin_executions_schedule ON checkin_executions(schedule_id);
CREATE INDEX idx_checkin_executions_date ON checkin_executions(executed_at);
```

### **1.3. Alteração na Tabela `accounts` (Opcional)**

```sql
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS default_checkin_time TIME DEFAULT '09:00:00';
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS default_checkin_enabled BOOLEAN DEFAULT false;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS max_checkins_per_day INTEGER DEFAULT 3;
```

---

## 🏗️ 2. ARQUITETURA DE JOBS

### **2.1. Comparação de Tecnologias**

| Critério | @Scheduled | Quartz | ShedLock |
|----------|-----------|--------|----------|
| **Simplicidade** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| **Escalabilidade** | ❌ Duplica em múltiplas instâncias | ✅ Cluster nativo | ✅ Lock distribuído |
| **Persistência** | ❌ Apenas em memória | ✅ Banco de dados | ✅ Banco de dados |
| **Flexibilidade** | ❌ Cron fixo | ✅ Dinâmico | ⭐⭐⭐ |
| **Overhead** | Baixo | Alto | Baixo |
| **Recomendado para** | Desenvolvimento | Sistemas complexos | **Produção escalável** |

### **2.2. Solução Recomendada: ShedLock + @Scheduled**

**Justificativa:**
- ✅ Simplicidade do Spring @Scheduled
- ✅ Lock distribuído via ShedLock (evita duplicação)
- ✅ Baixo overhead
- ✅ Suporte a PostgreSQL nativo
- ✅ Ideal para Railway/Docker com múltiplas instâncias

**Dependência:**
```xml
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-spring</artifactId>
    <version>5.10.2</version>
</dependency>
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-provider-jdbc-template</artifactId>
    <version>5.10.2</version>
</dependency>
```

**Tabela de Lock:**
```sql
CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
```

---

## 🔧 3. LÓGICA DE EXECUÇÃO

### **3.1. ProactiveCheckinService**

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ProactiveCheckinService {

    private final CheckinScheduleRepository scheduleRepository;
    private final CheckinExecutionRepository executionRepository;
    private final AIService aiService;
    private final EvolutionApiService evolutionApiService;
    private final ChatHistoryService chatHistoryService;
    private final AccountRepository accountRepository;
    private final PatientRepository patientRepository;

    /**
     * Executa check-ins agendados
     * Roda a cada 1 minuto, mas com lock distribuído (ShedLock)
     */
    @Scheduled(cron = "0 * * * * *") // A cada minuto
    @SchedulerLock(
        name = "proactiveCheckinJob",
        lockAtMostFor = "50s",
        lockAtLeastFor = "10s"
    )
    @Transactional
    public void executeScheduledCheckins() {
        log.info("🤖 Iniciando execução de check-ins proativos");

        LocalDateTime now = LocalDateTime.now();

        // Buscar agendamentos prontos para execução
        List<CheckinSchedule> schedules = scheduleRepository
                .findSchedulesReadyForExecution(now);

        log.info("📊 Encontrados {} check-ins para executar", schedules.size());

        for (CheckinSchedule schedule : schedules) {
            try {
                executeCheckin(schedule);
            } catch (Exception e) {
                log.error("❌ Erro ao executar check-in: {}", schedule.getId(), e);
                recordFailedExecution(schedule, e.getMessage());
            }
        }
    }

    private void executeCheckin(CheckinSchedule schedule) {
        UUID tenantId = schedule.getAccount().getId();
        UUID patientId = schedule.getPatient().getId();

        log.info("🚀 Executando check-in - Tenant: {}, Patient: {}", 
                 tenantId, patientId);

        // PASSO 1: Verificar rate limiting
        if (!canSendMessage(schedule)) {
            log.warn("⏭️ Check-in pulado - Rate limit atingido");
            recordSkippedExecution(schedule, "Rate limit exceeded");
            return;
        }

        // PASSO 2: Estabelecer contexto de tenant
        TenantContext context = new TenantContext(
                tenantId,
                schedule.getAccount().getAccountType(),
                schedule.getAccount().getCustomPrompt()
        );
        TenantContextHolder.setContext(context);

        try {
            // PASSO 3: Gerar mensagem proativa
            String message = generateProactiveMessage(schedule);

            // PASSO 4: Enviar via Evolution API
            String messageId = sendProactiveMessage(schedule, message);

            // PASSO 5: Registrar execução bem-sucedida
            recordSuccessfulExecution(schedule, message, messageId);

            // PASSO 6: Atualizar próxima execução
            updateNextExecution(schedule);

        } finally {
            TenantContextHolder.clear();
        }
    }

    private String generateProactiveMessage(CheckinSchedule schedule) {
        if (!schedule.getUseAiGeneration()) {
            return schedule.getCustomMessage();
        }

        // Buscar histórico recente (últimas 5 mensagens)
        List<Message> recentHistory = chatHistoryService.loadRecentMessages(
                schedule.getAccount().getId(),
                schedule.getPatient().getId(),
                5
        );

        // Criar prompt para IA
        String systemPrompt = buildProactiveSystemPrompt(schedule);

        // Gerar mensagem com IA
        return aiService.generateProactiveMessage(
                schedule.getAccount().getId(),
                schedule.getPatient().getId(),
                systemPrompt,
                recentHistory
        );
    }

    private String buildProactiveSystemPrompt(CheckinSchedule schedule) {
        String basePrompt = schedule.getAccount().getCustomPrompt();
        
        return basePrompt + """
                
                
                CONTEXTO ADICIONAL - MENSAGEM PROATIVA:
                Você está iniciando uma conversa proativa com o paciente.
                Seja empático, breve e direto.
                Pergunte como o paciente está se sentindo hoje.
                Mencione o histórico recente se relevante.
                
                Exemplo: "Bom dia! Como você está se sentindo hoje? 
                Vi que ontem você mencionou dor nível 7. Melhorou?"
                """;
    }

    private boolean canSendMessage(CheckinSchedule schedule) {
        // Reset contador diário
        if (!schedule.getLastResetDate().equals(LocalDate.now())) {
            schedule.setMessagesSentToday(0);
            schedule.setLastResetDate(LocalDate.now());
            scheduleRepository.save(schedule);
        }

        return schedule.getMessagesSentToday() < schedule.getMaxMessagesPerDay();
    }

    private String sendProactiveMessage(CheckinSchedule schedule, String message) {
        return evolutionApiService.sendMessage(
                schedule.getPatient().getWhatsappNumber(),
                message
        );
    }

    private void recordSuccessfulExecution(
            CheckinSchedule schedule, 
            String message, 
            String messageId) {
        
        CheckinExecution execution = CheckinExecution.builder()
                .schedule(schedule)
                .account(schedule.getAccount())
                .patient(schedule.getPatient())
                .status("SUCCESS")
                .messageSent(message)
                .messageId(messageId)
                .build();

        executionRepository.save(execution);

        // Incrementar contador
        schedule.setMessagesSentToday(schedule.getMessagesSentToday() + 1);
        schedule.setLastExecutionAt(LocalDateTime.now());
        scheduleRepository.save(schedule);
    }

    private void updateNextExecution(CheckinSchedule schedule) {
        LocalDateTime next = calculateNextExecution(schedule);
        schedule.setNextExecutionAt(next);
        scheduleRepository.save(schedule);
    }

    private LocalDateTime calculateNextExecution(CheckinSchedule schedule) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime timeOfDay = schedule.getTimeOfDay();

        switch (schedule.getScheduleType()) {
            case DAILY:
                return now.plusDays(1).with(timeOfDay);
            
            case WEEKLY:
                // Próximo dia da semana configurado
                return findNextWeeklyExecution(now, timeOfDay, schedule.getDaysOfWeek());
            
            default:
                return now.plusDays(1).with(timeOfDay);
        }
    }
}
```

---

## 🔐 4. FLUXO DE SEGURANÇA E ISOLAMENTO

### **4.1. Repository com Multi-Tenancy**

```java
@Repository
public interface CheckinScheduleRepository extends JpaRepository<CheckinSchedule, UUID> {

    /**
     * Busca agendamentos prontos para execução
     * COM ISOLAMENTO MULTI-TENANT
     */
    @Query("""
        SELECT cs FROM CheckinSchedule cs
        JOIN FETCH cs.account a
        JOIN FETCH cs.patient p
        WHERE cs.isActive = true
        AND cs.nextExecutionAt <= :now
        AND a.isActive = true
        AND p.isActive = true
        ORDER BY cs.nextExecutionAt ASC
        """)
    List<CheckinSchedule> findSchedulesReadyForExecution(@Param("now") LocalDateTime now);

    /**
     * Busca agendamentos de um tenant específico
     */
    @Query("SELECT cs FROM CheckinSchedule cs WHERE cs.account.id = :tenantId")
    List<CheckinSchedule> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Busca agendamentos de um paciente
     */
    @Query("""
        SELECT cs FROM CheckinSchedule cs 
        WHERE cs.patient.id = :patientId 
        AND cs.account.id = :tenantId
        """)
    List<CheckinSchedule> findByPatientAndTenant(
        @Param("patientId") UUID patientId,
        @Param("tenantId") UUID tenantId
    );
}
```

### **4.2. Garantias de Isolamento**

```java
// ✅ CORRETO: Contexto estabelecido antes de processar
TenantContext context = new TenantContext(
    schedule.getAccount().getId(),
    schedule.getAccount().getAccountType(),
    schedule.getAccount().getCustomPrompt()
);
TenantContextHolder.setContext(context);

try {
    // Processar com contexto ativo
    aiService.generateProactiveMessage(...);
} finally {
    // SEMPRE limpar contexto
    TenantContextHolder.clear();
}
```

---

## 💰 5. GESTÃO DE CUSTO E RATE LIMITING

### **5.1. Estratégias de Rate Limiting**

| Nível | Estratégia | Implementação |
|-------|-----------|---------------|
| **Por Paciente** | Max 3 mensagens/dia | `max_messages_per_day` na tabela |
| **Por Tenant** | Max 100 mensagens/dia | Contador na tabela `accounts` |
| **Global** | Max 1000 mensagens/hora | Redis counter ou DB |

### **5.2. Implementação de Rate Limiting**

```java
@Service
public class RateLimitService {

    private final CheckinScheduleRepository scheduleRepository;
    private final AccountRepository accountRepository;

    public boolean canSendCheckin(CheckinSchedule schedule) {
        // Nível 1: Verificar limite do paciente
        if (!checkPatientLimit(schedule)) {
            return false;
        }

        // Nível 2: Verificar limite do tenant
        if (!checkTenantLimit(schedule.getAccount())) {
            return false;
        }

        // Nível 3: Verificar limite global (opcional)
        if (!checkGlobalLimit()) {
            return false;
        }

        return true;
    }

    private boolean checkPatientLimit(CheckinSchedule schedule) {
        // Reset diário
        if (!schedule.getLastResetDate().equals(LocalDate.now())) {
            schedule.setMessagesSentToday(0);
            schedule.setLastResetDate(LocalDate.now());
            scheduleRepository.save(schedule);
        }

        return schedule.getMessagesSentToday() < schedule.getMaxMessagesPerDay();
    }

    private boolean checkTenantLimit(Account account) {
        // Contar mensagens enviadas hoje pelo tenant
        long count = scheduleRepository.countMessagesSentTodayByTenant(
            account.getId(),
            LocalDate.now()
        );

        int limit = account.getAccountType() == AccountType.B2B ? 100 : 50;
        return count < limit;
    }
}
```

---

## 📊 6. APIS DE GERENCIAMENTO

### **6.1. CheckinScheduleController**

```java
@RestController
@RequestMapping("/api/checkin-schedules")
@Tag(name = "Checkin Schedules", description = "Gerenciamento de check-ins proativos")
public class CheckinScheduleController {

    @PostMapping
    @Operation(summary = "Criar agendamento de check-in")
    public ResponseEntity<CheckinSchedule> createSchedule(
            @RequestParam UUID tenantId,
            @RequestBody CreateCheckinScheduleRequest request) {
        // Implementação
    }

    @GetMapping
    @Operation(summary = "Listar agendamentos do tenant")
    public ResponseEntity<List<CheckinSchedule>> listSchedules(
            @RequestParam UUID tenantId) {
        // Implementação
    }

    @PutMapping("/{scheduleId}")
    @Operation(summary = "Atualizar agendamento")
    public ResponseEntity<CheckinSchedule> updateSchedule(
            @PathVariable UUID scheduleId,
            @RequestParam UUID tenantId,
            @RequestBody UpdateCheckinScheduleRequest request) {
        // Implementação
    }

    @DeleteMapping("/{scheduleId}")
    @Operation(summary = "Deletar agendamento")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable UUID scheduleId,
            @RequestParam UUID tenantId) {
        // Implementação
    }
}
```

---

## 🎯 7. RESUMO DA ARQUITETURA

```
┌─────────────────────────────────────────────────────────────────┐
│ @Scheduled (a cada 1 minuto)                                    │
│ + ShedLock (lock distribuído)                                   │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ ProactiveCheckinService.executeScheduledCheckins()              │
│ - Busca schedules com next_execution_at <= NOW                  │
│ - Filtra apenas is_active = true                                │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Para cada CheckinSchedule:                                      │
│ 1. Verificar rate limiting                                      │
│ 2. Estabelecer TenantContext                                    │
│ 3. Gerar mensagem com IA (custom_prompt + histórico)            │
│ 4. Enviar via Evolution API                                     │
│ 5. Registrar execução                                           │
│ 6. Calcular próxima execução                                    │
│ 7. Limpar contexto                                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## ✅ ENTREGÁVEIS

| Item | Status | Arquivo |
|------|--------|---------|
| Modelo de Dados | ✅ | SQL schemas acima |
| Lógica de Execução | ✅ | ProactiveCheckinService |
| Rate Limiting | ✅ | RateLimitService |
| Fluxo de Segurança | ✅ | TenantContext integration |
| APIs de Gerenciamento | ✅ | CheckinScheduleController |

---

**🎉 Arquitetura completa e pronta para implementação!**

