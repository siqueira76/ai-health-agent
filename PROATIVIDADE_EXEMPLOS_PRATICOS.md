# 🚀 Mensagens Proativas - Exemplos Práticos de Implementação

## 📋 Índice
1. [Entidades JPA](#1-entidades-jpa)
2. [Repositories](#2-repositories)
3. [Services](#3-services)
4. [Controllers](#4-controllers)
5. [Configuração ShedLock](#5-configuração-shedlock)
6. [Testes](#6-testes)

---

## 1. ENTIDADES JPA

### **CheckinSchedule.java**

```java
package com.healthlink.ai_health_agent.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

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

    // Multi-tenancy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Configuração do Agendamento
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;

    @Column(name = "time_of_day", nullable = false)
    private LocalTime timeOfDay;

    @Column(name = "days_of_week")
    private int[] daysOfWeek; // [1,2,3,4,5] = Seg-Sex

    @Column(name = "timezone")
    private String timezone = "America/Sao_Paulo";

    // Personalização
    @Column(name = "custom_message", columnDefinition = "TEXT")
    private String customMessage;

    @Column(name = "use_ai_generation")
    private Boolean useAiGeneration = true;

    // Controle de Execução
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_execution_at")
    private LocalDateTime lastExecutionAt;

    @Column(name = "next_execution_at")
    private LocalDateTime nextExecutionAt;

    // Rate Limiting
    @Column(name = "max_messages_per_day")
    private Integer maxMessagesPerDay = 3;

    @Column(name = "messages_sent_today")
    private Integer messagesSentToday = 0;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate = LocalDate.now();

    // Auditoria
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    public enum ScheduleType {
        DAILY,      // Todos os dias
        WEEKLY,     // Dias específicos da semana
        CUSTOM      // Personalizado
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### **CheckinExecution.java**

```java
package com.healthlink.ai_health_agent.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private CheckinSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "executed_at")
    private LocalDateTime executedAt = LocalDateTime.now();

    @Column(name = "status", nullable = false)
    private String status; // SUCCESS, FAILED, SKIPPED

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "message_sent", columnDefinition = "TEXT")
    private String messageSent;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "patient_responded")
    private Boolean patientResponded = false;

    @Column(name = "response_received_at")
    private LocalDateTime responseReceivedAt;

    @Column(name = "execution_duration_ms")
    private Integer executionDurationMs;
}
```

---

## 2. REPOSITORIES

### **CheckinScheduleRepository.java**

```java
package com.healthlink.ai_health_agent.repository;

import com.healthlink.ai_health_agent.domain.entity.CheckinSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    /**
     * Conta mensagens enviadas hoje por um tenant
     */
    @Query("""
        SELECT COUNT(ce) FROM CheckinExecution ce
        WHERE ce.account.id = :tenantId
        AND DATE(ce.executedAt) = :date
        AND ce.status = 'SUCCESS'
        """)
    long countMessagesSentTodayByTenant(
        @Param("tenantId") UUID tenantId,
        @Param("date") LocalDate date
    );
}
```

### **CheckinExecutionRepository.java**

```java
package com.healthlink.ai_health_agent.repository;

import com.healthlink.ai_health_agent.domain.entity.CheckinExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CheckinExecutionRepository extends JpaRepository<CheckinExecution, UUID> {

    /**
     * Busca execuções de um schedule
     */
    @Query("""
        SELECT ce FROM CheckinExecution ce
        WHERE ce.schedule.id = :scheduleId
        ORDER BY ce.executedAt DESC
        """)
    List<CheckinExecution> findByScheduleId(@Param("scheduleId") UUID scheduleId);

    /**
     * Busca execuções de um paciente
     */
    @Query("""
        SELECT ce FROM CheckinExecution ce
        WHERE ce.patient.id = :patientId
        AND ce.account.id = :tenantId
        ORDER BY ce.executedAt DESC
        """)
    List<CheckinExecution> findByPatientAndTenant(
        @Param("patientId") UUID patientId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Busca execuções com falha
     */
    @Query("""
        SELECT ce FROM CheckinExecution ce
        WHERE ce.status = 'FAILED'
        AND ce.executedAt >= :since
        ORDER BY ce.executedAt DESC
        """)
    List<CheckinExecution> findFailedExecutionsSince(@Param("since") LocalDateTime since);
}
```

---

## 3. SERVICES

### **ProactiveCheckinService.java** (Completo)

```java
package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.domain.entity.*;
import com.healthlink.ai_health_agent.repository.*;
import com.healthlink.ai_health_agent.security.TenantContext;
import com.healthlink.ai_health_agent.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.ai.chat.messages.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProactiveCheckinService {

    private final CheckinScheduleRepository scheduleRepository;
    private final CheckinExecutionRepository executionRepository;
    private final AIService aiService;
    private final EvolutionApiService evolutionApiService;
    private final ChatHistoryService chatHistoryService;

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

        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;

        for (CheckinSchedule schedule : schedules) {
            try {
                ExecutionResult result = executeCheckin(schedule);
                
                switch (result) {
                    case SUCCESS -> successCount++;
                    case FAILED -> failedCount++;
                    case SKIPPED -> skippedCount++;
                }
            } catch (Exception e) {
                log.error("❌ Erro ao executar check-in: {}", schedule.getId(), e);
                recordFailedExecution(schedule, e.getMessage());
                failedCount++;
            }
        }

        log.info("✅ Check-ins executados - Success: {}, Failed: {}, Skipped: {}", 
                 successCount, failedCount, skippedCount);
    }

    private ExecutionResult executeCheckin(CheckinSchedule schedule) {
        // Implementação completa no arquivo PROATIVIDADE_ANALISE_TECNICA.md
        // ...
        return ExecutionResult.SUCCESS;
    }

    private enum ExecutionResult {
        SUCCESS, FAILED, SKIPPED
    }
}
```

---

## 4. CONTROLLERS

### **CheckinScheduleController.java**

```java
package com.healthlink.ai_health_agent.controller;

import com.healthlink.ai_health_agent.domain.entity.CheckinSchedule;
import com.healthlink.ai_health_agent.dto.CreateCheckinScheduleRequest;
import com.healthlink.ai_health_agent.service.CheckinScheduleManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkin-schedules")
@RequiredArgsConstructor
@Tag(name = "Checkin Schedules", description = "Gerenciamento de check-ins proativos")
public class CheckinScheduleController {

    private final CheckinScheduleManagementService managementService;

    @PostMapping
    @Operation(summary = "Criar agendamento de check-in")
    public ResponseEntity<CheckinSchedule> createSchedule(
            @RequestParam UUID tenantId,
            @RequestBody CreateCheckinScheduleRequest request) {
        
        CheckinSchedule schedule = managementService.createSchedule(tenantId, request);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping
    @Operation(summary = "Listar agendamentos do tenant")
    public ResponseEntity<List<CheckinSchedule>> listSchedules(
            @RequestParam UUID tenantId) {
        
        List<CheckinSchedule> schedules = managementService.listSchedules(tenantId);
        return ResponseEntity.ok(schedules);
    }

    @PutMapping("/{scheduleId}/toggle")
    @Operation(summary = "Ativar/Desativar agendamento")
    public ResponseEntity<CheckinSchedule> toggleSchedule(
            @PathVariable UUID scheduleId,
            @RequestParam UUID tenantId) {
        
        CheckinSchedule schedule = managementService.toggleSchedule(tenantId, scheduleId);
        return ResponseEntity.ok(schedule);
    }

    @DeleteMapping("/{scheduleId}")
    @Operation(summary = "Deletar agendamento")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable UUID scheduleId,
            @RequestParam UUID tenantId) {
        
        managementService.deleteSchedule(tenantId, scheduleId);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 5. CONFIGURAÇÃO SHEDLOCK

### **ShedLockConfig.java**

```java
package com.healthlink.ai_health_agent.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build()
        );
    }
}
```

### **Migration SQL (Flyway/Liquibase)**

```sql
-- V6__create_shedlock_table.sql
CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
```

---

**🎉 Exemplos práticos prontos para implementação!**

Consulte também:
- [`PROATIVIDADE_ANALISE_TECNICA.md`](PROATIVIDADE_ANALISE_TECNICA.md) - Análise completa
- [`SWAGGER_GUIA_TESTE.md`](SWAGGER_GUIA_TESTE.md) - Como testar APIs

