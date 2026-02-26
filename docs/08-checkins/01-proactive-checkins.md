# 8.1 Check-ins Proativos

## 🔔 Mensagens Proativas Automáticas

Check-ins proativos são mensagens automáticas enviadas pelo bot para coletar dados de saúde regularmente.

---

## 🎯 Por que Check-ins Proativos?

### **Problema:**
Pacientes esquecem de reportar sintomas regularmente.

### **Solução:**
Bot envia mensagens automáticas perguntando como o paciente está.

### **Benefícios:**
- ✅ Coleta de dados consistente
- ✅ Detecção precoce de pioras
- ✅ Engajamento do paciente
- ✅ Dados longitudinais para análise

---

## 📊 Modelo de Dados

### **CheckinSchedule:**

```java
@Entity
@Table(name = "checkin_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckinFrequency frequency; // DAILY, WEEKLY, CUSTOM
    
    @Column(name = "time_of_day")
    private LocalTime timeOfDay; // Ex: 09:00
    
    @Column(name = "days_of_week")
    private String daysOfWeek; // "MON,WED,FRI" para WEEKLY
    
    @Column(name = "custom_message")
    private String customMessage;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### **CheckinExecution:**

```java
@Entity
@Table(name = "checkin_executions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private CheckinSchedule schedule;
    
    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
    
    @Column(name = "message_sent")
    private String messageSent;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status; // SUCCESS, FAILED, PENDING
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "patient_responded")
    private Boolean patientResponded = false;
    
    @Column(name = "response_time")
    private LocalDateTime responseTime;
}
```

---

## ⏰ Scheduler

### **CheckinSchedulerService.java:**

```java
@Service
@Slf4j
public class CheckinSchedulerService {
    
    private final CheckinScheduleRepository scheduleRepository;
    private final CheckinExecutionRepository executionRepository;
    private final WhatsAppService whatsAppService;
    
    @Scheduled(cron = "0 */5 * * * *") // A cada 5 minutos
    @SchedulerLock(
        name = "executeCheckins",
        lockAtMostFor = "4m",
        lockAtLeastFor = "1m"
    )
    public void executeCheckins() {
        log.info("Starting checkin execution");
        
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        
        // Buscar schedules ativos que devem executar agora
        List<CheckinSchedule> schedules = scheduleRepository
            .findActiveSchedulesForTime(currentTime);
        
        log.info("Found {} schedules to execute", schedules.size());
        
        for (CheckinSchedule schedule : schedules) {
            try {
                executeCheckin(schedule, now);
            } catch (Exception e) {
                log.error("Error executing checkin for schedule {}", schedule.getId(), e);
            }
        }
        
        log.info("Checkin execution completed");
    }
    
    private void executeCheckin(CheckinSchedule schedule, LocalDateTime now) {
        // Verificar se já executou hoje
        if (alreadyExecutedToday(schedule, now)) {
            log.debug("Checkin already executed today for schedule {}", schedule.getId());
            return;
        }
        
        // Verificar dia da semana (para WEEKLY)
        if (schedule.getFrequency() == CheckinFrequency.WEEKLY) {
            if (!shouldExecuteToday(schedule, now)) {
                log.debug("Not scheduled for today: {}", schedule.getId());
                return;
            }
        }
        
        Patient patient = schedule.getPatient();
        String message = buildCheckinMessage(schedule);
        
        try {
            // Enviar mensagem
            whatsAppService.sendMessage(patient.getWhatsappNumber(), message);
            
            // Registrar execução
            CheckinExecution execution = CheckinExecution.builder()
                .account(schedule.getAccount())
                .schedule(schedule)
                .patient(patient)
                .executedAt(now)
                .messageSent(message)
                .status(ExecutionStatus.SUCCESS)
                .patientResponded(false)
                .build();
            
            executionRepository.save(execution);
            
            log.info("Checkin sent successfully to patient {}", patient.getId());
            
        } catch (Exception e) {
            log.error("Failed to send checkin to patient {}", patient.getId(), e);
            
            // Registrar falha
            CheckinExecution execution = CheckinExecution.builder()
                .account(schedule.getAccount())
                .schedule(schedule)
                .patient(patient)
                .executedAt(now)
                .messageSent(message)
                .status(ExecutionStatus.FAILED)
                .errorMessage(e.getMessage())
                .build();
            
            executionRepository.save(execution);
        }
    }
    
    private boolean alreadyExecutedToday(CheckinSchedule schedule, LocalDateTime now) {
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        
        return executionRepository.existsByScheduleAndExecutedAtAfter(
            schedule, startOfDay);
    }
    
    private boolean shouldExecuteToday(CheckinSchedule schedule, LocalDateTime now) {
        if (schedule.getDaysOfWeek() == null) return true;
        
        String today = now.getDayOfWeek().name().substring(0, 3); // MON, TUE, etc
        return schedule.getDaysOfWeek().contains(today);
    }
    
    private String buildCheckinMessage(CheckinSchedule schedule) {
        if (schedule.getCustomMessage() != null) {
            return schedule.getCustomMessage();
        }
        
        return switch (schedule.getFrequency()) {
            case DAILY -> "Olá! Como você está se sentindo hoje? 😊";
            case WEEKLY -> "Olá! Como foi sua semana? Gostaria de me contar como está?";
            default -> "Olá! Tudo bem? Como você está?";
        };
    }
}
```

---

## 📋 Repository

### **CheckinScheduleRepository.java:**

```java
@Repository
public interface CheckinScheduleRepository extends JpaRepository<CheckinSchedule, UUID> {
    
    @Query("""
        SELECT cs FROM CheckinSchedule cs
        JOIN cs.patient p
        JOIN cs.account a
        WHERE cs.isActive = true
        AND p.isActive = true
        AND (a.status = 'ACTIVE' OR a.status = 'TRIAL')
        AND cs.timeOfDay BETWEEN :startTime AND :endTime
        """)
    List<CheckinSchedule> findActiveSchedulesForTime(
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
    
    default List<CheckinSchedule> findActiveSchedulesForTime(LocalTime currentTime) {
        // Buscar schedules com +/- 5 minutos de tolerância
        LocalTime startTime = currentTime.minusMinutes(5);
        LocalTime endTime = currentTime.plusMinutes(5);
        return findActiveSchedulesForTime(startTime, endTime);
    }
    
    List<CheckinSchedule> findByPatientAndIsActive(Patient patient, Boolean isActive);
}
```

---

## 🎯 Criando Check-ins

### **CheckinService.java:**

```java
@Service
public class CheckinService {
    
    private final CheckinScheduleRepository scheduleRepository;
    
    public CheckinSchedule createDailyCheckin(Patient patient, LocalTime time) {
        CheckinSchedule schedule = CheckinSchedule.builder()
            .account(patient.getAccount())
            .patient(patient)
            .frequency(CheckinFrequency.DAILY)
            .timeOfDay(time)
            .isActive(true)
            .build();
        
        return scheduleRepository.save(schedule);
    }
    
    public CheckinSchedule createWeeklyCheckin(
            Patient patient, 
            LocalTime time, 
            List<DayOfWeek> days) {
        
        String daysOfWeek = days.stream()
            .map(day -> day.name().substring(0, 3))
            .collect(Collectors.joining(","));
        
        CheckinSchedule schedule = CheckinSchedule.builder()
            .account(patient.getAccount())
            .patient(patient)
            .frequency(CheckinFrequency.WEEKLY)
            .timeOfDay(time)
            .daysOfWeek(daysOfWeek)
            .isActive(true)
            .build();
        
        return scheduleRepository.save(schedule);
    }
}
```

---

## 📊 Métricas de Engajamento

### **Calcular Taxa de Resposta:**

```java
@Service
public class CheckinAnalyticsService {
    
    public EngagementMetrics calculateEngagement(UUID patientId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        List<CheckinExecution> executions = executionRepository
            .findByPatientAndExecutedAtAfter(patientId, since);
        
        long total = executions.size();
        long responded = executions.stream()
            .filter(CheckinExecution::getPatientResponded)
            .count();
        
        double responseRate = total > 0 ? (responded * 100.0 / total) : 0;
        
        return EngagementMetrics.builder()
            .totalCheckins(total)
            .respondedCheckins(responded)
            .responseRate(responseRate)
            .build();
    }
}
```

---

## 🎯 Próximos Passos

1. 📊 [Analytics](../09-analytics/01-health-analytics.md)
2. 🧪 [Testes](../10-testing/01-unit-tests.md)
3. 🚀 [Deploy](../11-deployment/01-railway-deploy.md)

---

[⬅️ Anterior: Evolution API Setup](../07-whatsapp/01-evolution-api-setup.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Analytics](../09-analytics/01-health-analytics.md)

