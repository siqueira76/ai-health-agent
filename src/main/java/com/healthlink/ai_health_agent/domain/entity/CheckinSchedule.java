package com.healthlink.ai_health_agent.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Entidade que representa um agendamento de check-in proativo.
 * Permite configurar mensagens automáticas para pacientes em horários específicos.
 */
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

    // ============================================
    // MULTI-TENANCY
    // ============================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // ============================================
    // CONFIGURAÇÃO DO AGENDAMENTO
    // ============================================

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;

    @Column(name = "time_of_day", nullable = false)
    private LocalTime timeOfDay;

    /**
     * Dias da semana para execução (1=Segunda, 7=Domingo)
     * Exemplo: [1,2,3,4,5] = Segunda a Sexta
     * NULL = Todos os dias
     */
    @Column(name = "days_of_week")
    private int[] daysOfWeek;

    @Column(name = "timezone")
    private String timezone = "America/Sao_Paulo";

    // ============================================
    // PERSONALIZAÇÃO DA MENSAGEM
    // ============================================

    @Column(name = "custom_message", columnDefinition = "TEXT")
    private String customMessage;

    /**
     * Se true, usa IA para gerar mensagem personalizada
     * Se false, usa custom_message fixo
     */
    @Column(name = "use_ai_generation")
    private Boolean useAiGeneration = true;

    // ============================================
    // CONTROLE DE EXECUÇÃO
    // ============================================

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_execution_at")
    private LocalDateTime lastExecutionAt;

    /**
     * Próxima execução calculada automaticamente
     */
    @Column(name = "next_execution_at")
    private LocalDateTime nextExecutionAt;

    // ============================================
    // RATE LIMITING
    // ============================================

    @Column(name = "max_messages_per_day")
    private Integer maxMessagesPerDay = 3;

    @Column(name = "messages_sent_today")
    private Integer messagesSentToday = 0;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate = LocalDate.now();

    // ============================================
    // AUDITORIA
    // ============================================

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    // ============================================
    // ENUMS
    // ============================================

    public enum ScheduleType {
        DAILY,      // Todos os dias no horário especificado
        WEEKLY,     // Dias específicos da semana
        CUSTOM      // Personalizado (futuro)
    }

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.lastResetDate == null) {
            this.lastResetDate = LocalDate.now();
        }
    }
}

