package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.domain.entity.Account;
import com.healthlink.ai_health_agent.domain.entity.Alert;
import com.healthlink.ai_health_agent.domain.entity.HealthLog;
import com.healthlink.ai_health_agent.domain.entity.Patient;
import com.healthlink.ai_health_agent.repository.AccountRepository;
import com.healthlink.ai_health_agent.repository.AlertRepository;
import com.healthlink.ai_health_agent.repository.HealthLogRepository;
import com.healthlink.ai_health_agent.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service para detecção de crises e geração de alertas
 * Monitora padrões críticos e notifica profissionais
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final HealthLogRepository healthLogRepository;
    private final PatientRepository patientRepository;
    private final AccountRepository accountRepository;

    // Thresholds configuráveis
    private static final int HIGH_PAIN_THRESHOLD = 8;
    private static final int CRITICAL_PAIN_THRESHOLD = 9;
    private static final int MEDICATION_SKIP_DAYS_THRESHOLD = 3;
    private static final int INACTIVITY_DAYS_THRESHOLD = 7;

    /**
     * Analisa HealthLog recém-criado e gera alertas se necessário
     * Chamado após salvar um HealthLog
     */
    @Transactional
    public void analyzeHealthLogAndCreateAlerts(UUID tenantId, UUID patientId, HealthLog healthLog) {
        log.debug("🔍 Analisando HealthLog para alertas - Patient: {}", patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        Account account = accountRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Account não encontrada"));

        // Verificar dor alta
        if (healthLog.getPainLevel() != null && healthLog.getPainLevel() >= HIGH_PAIN_THRESHOLD) {
            createHighPainAlert(account, patient, healthLog.getPainLevel());
        }

        // Verificar medicação não tomada
        if (Boolean.FALSE.equals(healthLog.getMedicationTaken())) {
            checkMedicationSkipPattern(account, patient);
        }

        // Verificar sono insuficiente
        if (healthLog.getSleepHours() != null && healthLog.getSleepHours() < 4) {
            createSleepDeprivationAlert(account, patient, healthLog.getSleepHours());
        }
    }

    /**
     * Cria alerta de dor alta
     */
    private void createHighPainAlert(Account account, Patient patient, int painLevel) {
        // Verificar se já existe alerta similar nas últimas 24h
        if (alertRepository.existsRecentAlert(
                patient.getId(),
                account.getId(),
                Alert.AlertType.HIGH_PAIN_LEVEL,
                LocalDateTime.now().minusHours(24))) {
            log.debug("⚠️ Alerta de dor alta já existe (últimas 24h)");
            return;
        }

        Alert alert = Alert.highPainAlert(account, patient, painLevel);
        alertRepository.save(alert);

        log.warn("🚨 ALERTA CRIADO: Dor nível {} - Paciente: {}", painLevel, patient.getName());
    }

    /**
     * Verifica padrão de medicação não tomada
     */
    private void checkMedicationSkipPattern(Account account, Patient patient) {
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
        var recentLogs = healthLogRepository.findByPatientAndPeriod(
                patient.getId(),
                account.getId(),
                last7Days,
                LocalDateTime.now()
        );

        long daysWithoutMedication = recentLogs.stream()
                .filter(hl -> Boolean.FALSE.equals(hl.getMedicationTaken()))
                .map(hl -> hl.getTimestamp().toLocalDate())
                .distinct()
                .count();

        if (daysWithoutMedication >= MEDICATION_SKIP_DAYS_THRESHOLD) {
            // Verificar se já existe alerta similar
            if (alertRepository.existsRecentAlert(
                    patient.getId(),
                    account.getId(),
                    Alert.AlertType.MEDICATION_SKIP,
                    LocalDateTime.now().minusDays(3))) {
                return;
            }

            Alert alert = Alert.medicationSkipAlert(account, patient, (int) daysWithoutMedication);
            alertRepository.save(alert);

            log.warn("🚨 ALERTA CRIADO: Medicação não tomada por {} dias - Paciente: {}",
                    daysWithoutMedication, patient.getName());
        }
    }

    /**
     * Cria alerta de privação de sono
     */
    private void createSleepDeprivationAlert(Account account, Patient patient, double sleepHours) {
        if (alertRepository.existsRecentAlert(
                patient.getId(),
                account.getId(),
                Alert.AlertType.SLEEP_DEPRIVATION,
                LocalDateTime.now().minusHours(24))) {
            return;
        }

        Alert alert = Alert.builder()
                .account(account)
                .patient(patient)
                .alertType(Alert.AlertType.SLEEP_DEPRIVATION)
                .severity(Alert.Severity.MEDIUM)
                .message(String.format("Paciente %s dormiu apenas %.1f horas", patient.getName(), sleepHours))
                .details(String.format("{\"sleepHours\": %.1f}", sleepHours))
                .createdAt(LocalDateTime.now())
                .acknowledged(false)
                .build();

        alertRepository.save(alert);

        log.warn("🚨 ALERTA CRIADO: Privação de sono ({} horas) - Paciente: {}",
                sleepHours, patient.getName());
    }

    /**
     * Cria alerta de inatividade
     */
    private void createInactivityAlert(Account account, Patient patient, int daysInactive) {
        if (alertRepository.existsRecentAlert(
                patient.getId(),
                account.getId(),
                Alert.AlertType.INACTIVITY,
                LocalDateTime.now().minusDays(7))) {
            return;
        }

        Alert alert = Alert.inactivityAlert(account, patient, daysInactive);
        alertRepository.save(alert);

        log.warn("🚨 ALERTA CRIADO: Inatividade de {} dias - Paciente: {}",
                daysInactive, patient.getName());
    }

    /**
     * Reconhece um alerta
     */
    @Transactional
    public void acknowledgeAlert(UUID tenantId, UUID alertId, String acknowledgedBy) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));

        // Validar tenant
        if (!alert.getAccount().getId().equals(tenantId)) {
            throw new SecurityException("Acesso negado");
        }

        alert.acknowledge(acknowledgedBy);
        alertRepository.save(alert);

        log.info("✅ Alerta reconhecido - ID: {}, Por: {}", alertId, acknowledgedBy);
    }

    /**
     * Busca alertas ativos de um paciente
     */
    public List<Alert> getActiveAlerts(UUID tenantId, UUID patientId) {
        return alertRepository.findActiveAlertsByPatient(patientId, tenantId);
    }

    /**
     * Busca todos os alertas ativos de um tenant
     */
    public List<Alert> getAllActiveAlerts(UUID tenantId) {
        return alertRepository.findActiveAlertsByTenant(tenantId);
    }

    /**
     * Busca alertas críticos
     */
    public List<Alert> getCriticalAlerts(UUID tenantId) {
        return alertRepository.findCriticalAlerts(tenantId);
    }

    /**
     * Conta alertas ativos de um paciente
     */
    public Long countActiveAlerts(UUID tenantId, UUID patientId) {
        return alertRepository.countActiveAlertsByPatient(patientId, tenantId);
    }

    /**
     * Verifica inatividade de pacientes
     * Deve ser executado periodicamente (cron job)
     */
    @Transactional
    public void checkInactivePatients(UUID tenantId) {
        log.info("🔍 Verificando pacientes inativos - Tenant: {}", tenantId);

        var patients = patientRepository.findByAccountId(tenantId);
        Account account = accountRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Account não encontrada"));

        for (Patient patient : patients) {
            if (!patient.getIsActive()) {
                continue;
            }

            if (patient.getLastInteractionAt() == null) {
                continue;
            }

            long daysInactive = java.time.temporal.ChronoUnit.DAYS.between(
                    patient.getLastInteractionAt(),
                    LocalDateTime.now()
            );

            if (daysInactive >= INACTIVITY_DAYS_THRESHOLD) {
                createInactivityAlert(account, patient, (int) daysInactive);
            }
        }
    }
}
