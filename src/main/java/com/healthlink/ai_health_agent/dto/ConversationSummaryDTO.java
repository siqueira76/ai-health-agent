package com.healthlink.ai_health_agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO com resumo de conversas de um paciente
 * Usado para visualização no dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDTO {

    private UUID patientId;
    private String patientName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalMessages;
    private List<MessageDTO> recentMessages;
    private List<TopicDTO> mainTopics;
    private SentimentAnalysisDTO sentimentAnalysis;

    /**
     * Mensagem individual
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageDTO {
        private UUID messageId;
        private String role; // USER ou ASSISTANT
        private String content;
        private LocalDateTime timestamp;
        private Integer contentLength;

        /**
         * Retorna preview da mensagem (primeiros 100 caracteres)
         */
        public String getPreview() {
            if (content == null) {
                return "";
            }
            return content.length() > 100 
                ? content.substring(0, 100) + "..." 
                : content;
        }
    }

    /**
     * Tópico identificado nas conversas
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicDTO {
        private String topic;
        private Integer frequency;
        private Double percentage;
    }

    /**
     * Análise de sentimento
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentAnalysisDTO {
        private Integer positiveMessages;
        private Integer neutralMessages;
        private Integer negativeMessages;
        private Double positivePercentage;
        private Double negativePercentage;
        private String overallSentiment; // POSITIVE, NEUTRAL, NEGATIVE

        /**
         * Calcula sentimento geral
         */
        public static String calculateOverallSentiment(int positive, int neutral, int negative) {
            int total = positive + neutral + negative;
            if (total == 0) {
                return "NEUTRAL";
            }

            double positiveRatio = (double) positive / total;
            double negativeRatio = (double) negative / total;

            if (positiveRatio > 0.6) {
                return "POSITIVE";
            } else if (negativeRatio > 0.6) {
                return "NEGATIVE";
            } else {
                return "NEUTRAL";
            }
        }
    }

    /**
     * Calcula densidade de conversação (mensagens por dia)
     */
    public Double getConversationDensity() {
        if (startDate == null || endDate == null || totalMessages == null) {
            return 0.0;
        }

        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (days == 0) {
            return totalMessages.doubleValue();
        }

        return totalMessages.doubleValue() / days;
    }

    /**
     * Verifica se há conversas recentes
     */
    public boolean hasRecentActivity() {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return false;
        }

        LocalDateTime lastMessage = recentMessages.get(0).getTimestamp();
        return lastMessage.isAfter(LocalDateTime.now().minusDays(3));
    }
}

