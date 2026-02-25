package com.healthlink.ai_health_agent.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade para armazenar alertas gerados pelo sistema
 * Alertas são criados quando padrões críticos são detectados
 */
@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_patient_created", columnList = "patient_id, created_at DESC"),
        @Index(name = "idx_alerts_account_severity", columnList = "account_id, severity, acknowledged"),
        @Index(name = "idx_alerts_type", columnList = "alert_type, acknowledged")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Relacionamento com Account (tenant)
     * Garante isolamento multi-tenant
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /**
     * Relacionamento com Patient
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Tipo de alerta
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    /**
     * Severidade do alerta
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    /**
     * Mensagem descritiva do alerta
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Detalhes adicionais em JSON
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * Data/hora de criação
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Se o alerta foi reconhecido pelo profissional
     */
    @Column(nullable = false)
    private Boolean acknowledged;

    /**
     * Data/hora do reconhecimento
     */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    /**
     * Usuário que reconheceu (futuro: sistema de autenticação)
     */
    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    /**
     * Tipos de alerta
     */
    public enum AlertType {
        HIGH_PAIN_LEVEL,           // Dor muito alta (>= 8)
        PAIN_INCREASE,             // Aumento significativo de dor
        MEDICATION_SKIP,           // Medicação não tomada por vários dias
        SLEEP_DEPRIVATION,         // Sono insuficiente
        NEGATIVE_MOOD_PATTERN,     // Padrão de humor negativo
        INACTIVITY,                // Paciente sem interagir há muito tempo
        CRISIS_KEYWORDS,           // Palavras-chave de crise detectadas
        SUDDEN_BEHAVIOR_CHANGE     // Mudança súbita de comportamento
    }

    /**
     * Níveis de severidade
     */
    public enum Severity {
        LOW,      // Informativo
        MEDIUM,   // Atenção necessária
        HIGH,     // Ação recomendada
        CRITICAL  // Ação imediata necessária
    }

    /**
     * Helper para criar alerta de dor alta
     */
    public static Alert highPainAlert(Account account, Patient patient, int painLevel) {
        return Alert.builder()
                .account(account)
                .patient(patient)
                .alertType(AlertType.HIGH_PAIN_LEVEL)
                .severity(painLevel >= 9 ? Severity.CRITICAL : Severity.HIGH)
                .message(String.format("Paciente %s reportou dor nível %d", patient.getName(), painLevel))
                .details(String.format("{\"painLevel\": %d}", painLevel))
                .createdAt(LocalDateTime.now())
                .acknowledged(false)
                .build();
    }

    /**
     * Helper para criar alerta de medicação
     */
    public static Alert medicationSkipAlert(Account account, Patient patient, int daysSkipped) {
        return Alert.builder()
                .account(account)
                .patient(patient)
                .alertType(AlertType.MEDICATION_SKIP)
                .severity(daysSkipped >= 3 ? Severity.HIGH : Severity.MEDIUM)
                .message(String.format("Paciente %s não tomou medicação por %d dias", patient.getName(), daysSkipped))
                .details(String.format("{\"daysSkipped\": %d}", daysSkipped))
                .createdAt(LocalDateTime.now())
                .acknowledged(false)
                .build();
    }

    /**
     * Helper para criar alerta de inatividade
     */
    public static Alert inactivityAlert(Account account, Patient patient, int daysInactive) {
        return Alert.builder()
                .account(account)
                .patient(patient)
                .alertType(AlertType.INACTIVITY)
                .severity(daysInactive >= 14 ? Severity.HIGH : Severity.MEDIUM)
                .message(String.format("Paciente %s sem interagir há %d dias", patient.getName(), daysInactive))
                .details(String.format("{\"daysInactive\": %d}", daysInactive))
                .createdAt(LocalDateTime.now())
                .acknowledged(false)
                .build();
    }

    /**
     * Reconhece o alerta
     */
    public void acknowledge(String acknowledgedBy) {
        this.acknowledged = true;
        this.acknowledgedAt = LocalDateTime.now();
        this.acknowledgedBy = acknowledgedBy;
    }
}

