package com.healthlink.ai_health_agent.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa uma execução de check-in proativo.
 * Armazena histórico de todas as tentativas de envio de mensagens proativas.
 */
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

    // ============================================
    // RELACIONAMENTOS
    // ============================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private CheckinSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // ============================================
    // DETALHES DA EXECUÇÃO
    // ============================================

    @Column(name = "executed_at")
    private LocalDateTime executedAt = LocalDateTime.now();

    /**
     * Status da execução:
     * - SUCCESS: Mensagem enviada com sucesso
     * - FAILED: Erro ao enviar mensagem
     * - SKIPPED: Pulado por rate limiting ou outra razão
     */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // ============================================
    // MENSAGEM ENVIADA
    // ============================================

    @Column(name = "message_sent", columnDefinition = "TEXT")
    private String messageSent;

    /**
     * ID da mensagem retornado pela Evolution API
     */
    @Column(name = "message_id")
    private String messageId;

    // ============================================
    // RESPOSTA DO PACIENTE
    // ============================================

    @Column(name = "patient_responded")
    private Boolean patientResponded = false;

    @Column(name = "response_received_at")
    private LocalDateTime responseReceivedAt;

    // ============================================
    // MÉTRICAS
    // ============================================

    @Column(name = "execution_duration_ms")
    private Integer executionDurationMs;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    public void prePersist() {
        if (this.executedAt == null) {
            this.executedAt = LocalDateTime.now();
        }
        if (this.patientResponded == null) {
            this.patientResponded = false;
        }
    }
}

