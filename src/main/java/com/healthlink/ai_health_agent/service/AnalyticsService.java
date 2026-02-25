package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.domain.entity.Alert;
import com.healthlink.ai_health_agent.domain.entity.HealthLog;
import com.healthlink.ai_health_agent.domain.entity.Patient;
import com.healthlink.ai_health_agent.dto.ConversationSummaryDTO;
import com.healthlink.ai_health_agent.dto.PatientStatsDTO;
import com.healthlink.ai_health_agent.repository.AlertRepository;
import com.healthlink.ai_health_agent.repository.ChatMessageRepository;
import com.healthlink.ai_health_agent.repository.HealthLogRepository;
import com.healthlink.ai_health_agent.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service para calcular estatísticas e analytics
 * Usado no dashboard de psicólogos
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final PatientRepository patientRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final HealthLogRepository healthLogRepository;
    private final AlertRepository alertRepository;

    /**
     * Calcula estatísticas completas de um paciente
     */
    public PatientStatsDTO calculatePatientStats(UUID tenantId, UUID patientId) {
        log.info("📊 Calculando estatísticas - Tenant: {}, Patient: {}", tenantId, patientId);

        // Buscar paciente
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // Validar tenant
        if (!patient.getTenantId().equals(tenantId)) {
            throw new SecurityException("Acesso negado");
        }

        // Calcular estatísticas de mensagens
        Long totalMessages = chatMessageRepository.countByPatient(patientId, tenantId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last7Days = now.minusDays(7);
        LocalDateTime last30Days = now.minusDays(30);

        var messagesLast7 = chatMessageRepository.findByPatientAndPeriod(patientId, tenantId, last7Days, now);
        var messagesLast30 = chatMessageRepository.findByPatientAndPeriod(patientId, tenantId, last30Days, now);

        Double avgMessagesPerDay = messagesLast30.size() / 30.0;

        // Calcular estatísticas de saúde
        var healthStats = calculateHealthStats(tenantId, patientId, last30Days, now);

        // Buscar alertas ativos
        var activeAlerts = alertRepository.findActiveAlertsByPatient(patientId, tenantId)
                .stream()
                .map(this::toAlertSummary)
                .collect(Collectors.toList());

        // Calcular tendências
        var painTrend = calculatePainTrend(tenantId, patientId);
        var moodTrend = calculateMoodTrend(tenantId, patientId);
        var sleepTrend = calculateSleepTrend(tenantId, patientId);

        return PatientStatsDTO.builder()
                .patientId(patientId)
                .name(patient.getName())
                .whatsappNumber(patient.getWhatsappNumber())
                .diagnosis(patient.getDiagnosis())
                .lastInteractionAt(patient.getLastInteractionAt())
                .isActive(patient.getIsActive())
                .totalMessages(totalMessages)
                .messagesLast7Days((long) messagesLast7.size())
                .messagesLast30Days((long) messagesLast30.size())
                .averageMessagesPerDay(avgMessagesPerDay)
                .healthStats(healthStats)
                .activeAlerts(activeAlerts)
                .painTrend(painTrend)
                .moodTrend(moodTrend)
                .sleepTrend(sleepTrend)
                .build();
    }

    /**
     * Calcula estatísticas de saúde
     */
    private PatientStatsDTO.HealthStatsDTO calculateHealthStats(UUID tenantId, UUID patientId,
                                                                  LocalDateTime start, LocalDateTime end) {
        var healthLogs = healthLogRepository.findByPatientAndPeriod(patientId, tenantId, start, end);

        if (healthLogs.isEmpty()) {
            return PatientStatsDTO.HealthStatsDTO.builder()
                    .totalHealthLogs(0)
                    .build();
        }

        // Calcular médias de dor
        var painLevels = healthLogs.stream()
                .filter(hl -> hl.getPainLevel() != null)
                .map(HealthLog::getPainLevel)
                .collect(Collectors.toList());

        Double avgPain = painLevels.isEmpty() ? null :
                painLevels.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        Double maxPain = painLevels.isEmpty() ? null :
                Double.valueOf(painLevels.stream().mapToInt(Integer::intValue).max().orElse(0));
        Double minPain = painLevels.isEmpty() ? null :
                Double.valueOf(painLevels.stream().mapToInt(Integer::intValue).min().orElse(0));

        // Contar dias com dor
        int daysWithPain = (int) healthLogs.stream()
                .filter(hl -> hl.getPainLevel() != null && hl.getPainLevel() > 0)
                .map(hl -> hl.getTimestamp().toLocalDate())
                .distinct()
                .count();

        // Calcular aderência à medicação
        long totalDays = ChronoUnit.DAYS.between(start, end);
        int daysWithMedication = (int) healthLogs.stream()
                .filter(hl -> Boolean.TRUE.equals(hl.getMedicationTaken()))
                .map(hl -> hl.getTimestamp().toLocalDate())
                .distinct()
                .count();

        Double medicationAdherence = totalDays > 0 ? (daysWithMedication * 100.0 / totalDays) : 0.0;

        // Moods mais comuns
        List<String> commonMoods = healthLogs.stream()
                .filter(hl -> hl.getMood() != null && !hl.getMood().isBlank())
                .map(HealthLog::getMood)
                .collect(Collectors.groupingBy(mood -> mood, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Média de horas de sono
        Double avgSleep = healthLogs.stream()
                .filter(hl -> hl.getSleepHours() != null)
                .mapToDouble(HealthLog::getSleepHours)
                .average()
                .orElse(0.0);

        return PatientStatsDTO.HealthStatsDTO.builder()
                .averagePainLevel(avgPain)
                .maxPainLevel(maxPain)
                .minPainLevel(minPain)
                .totalHealthLogs(healthLogs.size())
                .daysWithPain(daysWithPain)
                .daysWithMedication(daysWithMedication)
                .medicationAdherence(medicationAdherence)
                .commonMoods(commonMoods)
                .averageSleepHours(avgSleep)
                .build();
    }

    /**
     * Calcula tendência de dor (últimos 14 dias vs 14 dias anteriores)
     */
    private PatientStatsDTO.TrendDTO calculatePainTrend(UUID tenantId, UUID patientId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last14Days = now.minusDays(14);
        LocalDateTime previous14Days = now.minusDays(28);

        var recentLogs = healthLogRepository.findByPatientAndPeriod(patientId, tenantId, last14Days, now);
        var previousLogs = healthLogRepository.findByPatientAndPeriod(patientId, tenantId, previous14Days, last14Days);

        Double recentAvg = recentLogs.stream()
                .filter(hl -> hl.getPainLevel() != null)
                .mapToInt(HealthLog::getPainLevel)
                .average()
                .orElse(0.0);

        Double previousAvg = previousLogs.stream()
                .filter(hl -> hl.getPainLevel() != null)
                .mapToInt(HealthLog::getPainLevel)
                .average()
                .orElse(0.0);

        return calculateTrend(recentAvg, previousAvg, "dor");
    }

    /**
     * Calcula tendência de humor
     */
    private PatientStatsDTO.TrendDTO calculateMoodTrend(UUID tenantId, UUID patientId) {
        // Simplificado: retorna STABLE por enquanto
        // Implementação completa requer análise de sentimento
        return PatientStatsDTO.TrendDTO.builder()
                .direction("STABLE")
                .changePercentage(0.0)
                .description("Humor estável")
                .build();
    }

    /**
     * Calcula tendência de sono
     */
    private PatientStatsDTO.TrendDTO calculateSleepTrend(UUID tenantId, UUID patientId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last14Days = now.minusDays(14);
        LocalDateTime previous14Days = now.minusDays(28);

        var recentLogs = healthLogRepository.findByPatientAndPeriod(patientId, tenantId, last14Days, now);
        var previousLogs = healthLogRepository.findByPatientAndPeriod(patientId, tenantId, previous14Days, last14Days);

        Double recentAvg = recentLogs.stream()
                .filter(hl -> hl.getSleepHours() != null)
                .mapToDouble(HealthLog::getSleepHours)
                .average()
                .orElse(0.0);

        Double previousAvg = previousLogs.stream()
                .filter(hl -> hl.getSleepHours() != null)
                .mapToDouble(HealthLog::getSleepHours)
                .average()
                .orElse(0.0);

        return calculateTrend(recentAvg, previousAvg, "sono");
    }

    /**
     * Calcula tendência genérica
     */
    private PatientStatsDTO.TrendDTO calculateTrend(Double recent, Double previous, String metric) {
        if (previous == 0.0) {
            return PatientStatsDTO.TrendDTO.builder()
                    .direction("STABLE")
                    .changePercentage(0.0)
                    .description("Dados insuficientes")
                    .build();
        }

        double change = ((recent - previous) / previous) * 100;
        String direction;
        String description;

        if (Math.abs(change) < 5) {
            direction = "STABLE";
            description = String.format("%s estável", metric);
        } else if (change > 0) {
            direction = "UP";
            description = String.format("%s aumentou %.1f%%", metric, change);
        } else {
            direction = "DOWN";
            description = String.format("%s diminuiu %.1f%%", metric, Math.abs(change));
        }

        return PatientStatsDTO.TrendDTO.builder()
                .direction(direction)
                .changePercentage(change)
                .description(description)
                .build();
    }

    /**
     * Converte Alert para AlertSummaryDTO
     */
    private PatientStatsDTO.AlertSummaryDTO toAlertSummary(Alert alert) {
        return PatientStatsDTO.AlertSummaryDTO.builder()
                .alertId(alert.getId())
                .type(alert.getAlertType().name())
                .severity(alert.getSeverity().name())
                .message(alert.getMessage())
                .createdAt(alert.getCreatedAt())
                .acknowledged(alert.getAcknowledged())
                .build();
    }

    /**
     * Busca resumo de conversas de um paciente
     */
    public ConversationSummaryDTO getConversationSummary(UUID tenantId, UUID patientId,
                                                          LocalDateTime startDate, LocalDateTime endDate) {
        log.info("💬 Gerando resumo de conversas - Patient: {}", patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        if (!patient.getTenantId().equals(tenantId)) {
            throw new SecurityException("Acesso negado");
        }

        var messages = chatMessageRepository.findByPatientAndPeriod(patientId, tenantId, startDate, endDate);

        var recentMessages = messages.stream()
                .limit(20)
                .map(cm -> ConversationSummaryDTO.MessageDTO.builder()
                        .messageId(cm.getId())
                        .role(cm.getRole().name())
                        .content(cm.getContent())
                        .timestamp(cm.getTimestamp())
                        .contentLength(cm.getContent() != null ? cm.getContent().length() : 0)
                        .build())
                .collect(Collectors.toList());

        return ConversationSummaryDTO.builder()
                .patientId(patientId)
                .patientName(patient.getName())
                .startDate(startDate)
                .endDate(endDate)
                .totalMessages(messages.size())
                .recentMessages(recentMessages)
                .mainTopics(new ArrayList<>()) // Implementar análise de tópicos futuramente
                .sentimentAnalysis(null) // Implementar análise de sentimento futuramente
                .build();
    }

    /**
     * Lista todos os pacientes de um tenant com estatísticas resumidas
     */
    public List<PatientStatsDTO> getAllPatientsStats(UUID tenantId) {
        log.info("📊 Calculando estatísticas de todos os pacientes - Tenant: {}", tenantId);

        var patients = patientRepository.findByAccountId(tenantId);

        return patients.stream()
                .map(patient -> calculatePatientStats(tenantId, patient.getId()))
                .sorted(Comparator.comparing(PatientStatsDTO::getLastInteractionAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
}
