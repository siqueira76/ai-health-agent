package com.healthlink.ai_health_agent.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade para armazenar logs de saúde dos pacientes
 * Preparada para Function Calling da IA
 * 
 * Isolamento Multi-Tenant: account_id + patient_id
 */
@Entity
@Table(name = "health_logs", indexes = {
        @Index(name = "idx_health_logs_patient", columnList = "patient_id"),
        @Index(name = "idx_health_logs_account", columnList = "account_id"),
        @Index(name = "idx_health_logs_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * ISOLAMENTO MULTI-TENANT
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * DADOS DE SAÚDE ESTRUTURADOS
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "pain_level")
    private Integer painLevel;  // 0-10

    @Column(name = "mood")
    private String mood;  // "bem", "ansioso", "triste", "irritado", "deprimido"

    @Column(name = "sleep_quality")
    private String sleepQuality;  // "ótimo", "bom", "regular", "ruim", "péssimo"

    @Column(name = "sleep_hours")
    private Double sleepHours;  // Horas de sono

    @Column(name = "medication_taken")
    private Boolean medicationTaken;

    @Column(name = "medication_name")
    private String medicationName;

    @Column(name = "energy_level")
    private Integer energyLevel;  // 0-10

    @Column(name = "stress_level")
    private Integer stressLevel;  // 0-10

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;  // Observações adicionais

    /**
     * AUDITORIA: JSON bruto extraído pela IA
     * Armazena o JSON completo que a IA gerou via Function Calling
     */
    @Column(name = "raw_ai_extraction", columnDefinition = "TEXT")
    private String rawAiExtraction;

    /**
     * METADADOS
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    /**
     * Helper: Obter tenantId
     */
    public UUID getTenantId() {
        return account != null ? account.getId() : null;
    }

    /**
     * Helper: Verificar se tem dados de dor
     */
    public boolean hasPainData() {
        return painLevel != null;
    }

    /**
     * Helper: Verificar se tem dados de humor
     */
    public boolean hasMoodData() {
        return mood != null && !mood.isBlank();
    }

    /**
     * Helper: Verificar se tem dados de sono
     */
    public boolean hasSleepData() {
        return sleepQuality != null || sleepHours != null;
    }

    /**
     * Helper: Resumo do log para exibição
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (painLevel != null) {
            summary.append("Dor: ").append(painLevel).append("/10");
        }
        
        if (mood != null) {
            if (summary.length() > 0) summary.append(" | ");
            summary.append("Humor: ").append(mood);
        }
        
        if (sleepQuality != null) {
            if (summary.length() > 0) summary.append(" | ");
            summary.append("Sono: ").append(sleepQuality);
        }
        
        if (medicationTaken != null) {
            if (summary.length() > 0) summary.append(" | ");
            summary.append("Medicação: ").append(medicationTaken ? "Sim" : "Não");
        }
        
        return summary.toString();
    }
}

