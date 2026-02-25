package com.healthlink.ai_health_agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO com estatísticas completas de um paciente
 * Usado no dashboard de psicólogos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientStatsDTO {

    // Informações básicas
    private UUID patientId;
    private String name;
    private String whatsappNumber;
    private String diagnosis;
    private LocalDateTime lastInteractionAt;
    private Boolean isActive;

    // Estatísticas de conversação
    private Long totalMessages;
    private Long messagesLast7Days;
    private Long messagesLast30Days;
    private Double averageMessagesPerDay;

    // Estatísticas de saúde (últimos 30 dias)
    private HealthStatsDTO healthStats;

    // Alertas ativos
    private List<AlertSummaryDTO> activeAlerts;

    // Tendências
    private TrendDTO painTrend;
    private TrendDTO moodTrend;
    private TrendDTO sleepTrend;

    /**
     * Estatísticas de saúde
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthStatsDTO {
        private Double averagePainLevel;
        private Double maxPainLevel;
        private Double minPainLevel;
        private Integer totalHealthLogs;
        private Integer daysWithPain;
        private Integer daysWithMedication;
        private Double medicationAdherence; // Percentual
        private List<String> commonMoods;
        private Double averageSleepHours;
    }

    /**
     * Resumo de alerta
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertSummaryDTO {
        private UUID alertId;
        private String type;
        private String severity;
        private String message;
        private LocalDateTime createdAt;
        private Boolean acknowledged;
    }

    /**
     * Tendência (crescente, estável, decrescente)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDTO {
        private String direction; // UP, DOWN, STABLE
        private Double changePercentage;
        private String description;
    }

    /**
     * Verifica se o paciente está em risco
     */
    public boolean isAtRisk() {
        if (activeAlerts == null || activeAlerts.isEmpty()) {
            return false;
        }
        return activeAlerts.stream()
                .anyMatch(alert -> "HIGH".equals(alert.getSeverity()) || "CRITICAL".equals(alert.getSeverity()));
    }

    /**
     * Verifica se o paciente está inativo
     */
    public boolean isInactive() {
        if (lastInteractionAt == null) {
            return true;
        }
        return lastInteractionAt.isBefore(LocalDateTime.now().minusDays(7));
    }

    /**
     * Calcula score de engajamento (0-100)
     */
    public int getEngagementScore() {
        if (averageMessagesPerDay == null) {
            return 0;
        }
        
        // Score baseado em mensagens por dia
        int score = (int) Math.min(averageMessagesPerDay * 20, 100);
        
        // Penalizar se inativo
        if (isInactive()) {
            score = score / 2;
        }
        
        return score;
    }

    /**
     * Retorna status geral do paciente
     */
    public String getOverallStatus() {
        if (!isActive) {
            return "INACTIVE";
        }
        if (isAtRisk()) {
            return "AT_RISK";
        }
        if (isInactive()) {
            return "DISENGAGED";
        }
        if (getEngagementScore() > 70) {
            return "ENGAGED";
        }
        return "STABLE";
    }
}

