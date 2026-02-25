package com.healthlink.ai_health_agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlink.ai_health_agent.domain.entity.Account;
import com.healthlink.ai_health_agent.domain.entity.HealthLog;
import com.healthlink.ai_health_agent.domain.entity.Patient;
import com.healthlink.ai_health_agent.dto.HealthStatsRequest;
import com.healthlink.ai_health_agent.repository.AccountRepository;
import com.healthlink.ai_health_agent.repository.HealthLogRepository;
import com.healthlink.ai_health_agent.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service para gerenciar HealthLogs
 * Usado pelo Function Calling da IA
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HealthLogService {

    private final HealthLogRepository healthLogRepository;
    private final PatientRepository patientRepository;
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;
    private final AlertService alertService;

    /**
     * Registra dados de saúde extraídos pela IA
     * Esta é a função chamada pelo Function Calling
     * 
     * @param tenantId ID do tenant
     * @param patientId ID do paciente
     * @param request Dados extraídos pela IA
     * @return Mensagem de confirmação
     */
    @Transactional
    public String recordHealthStats(UUID tenantId, UUID patientId, HealthStatsRequest request) {
        log.info("📊 Registrando dados de saúde - Tenant: {}, Patient: {}", tenantId, patientId);
        log.debug("Dados recebidos: {}", request.getSummary());

        // Validar se há dados para salvar
        if (!request.hasAnyData()) {
            log.warn("⚠️ Nenhum dado de saúde para registrar");
            return "Nenhum dado de saúde foi identificado para registro.";
        }

        // Buscar paciente com validação de tenant
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado: " + patientId));

        // Validação de segurança: garantir que o paciente pertence ao tenant
        if (!patient.getTenantId().equals(tenantId)) {
            log.error("🚨 Tentativa de acesso cross-tenant! Patient: {}, Tenant esperado: {}, Tenant real: {}",
                      patientId, tenantId, patient.getTenantId());
            throw new SecurityException("Acesso negado: paciente não pertence ao tenant");
        }

        // Buscar account
        Account account = accountRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Account não encontrada: " + tenantId));

        // Criar HealthLog
        HealthLog healthLog = HealthLog.builder()
                .account(account)
                .patient(patient)
                .timestamp(LocalDateTime.now())
                .painLevel(request.getPainLevel())
                .mood(request.getMood())
                .sleepQuality(request.getSleepQuality())
                .sleepHours(request.getSleepHours())
                .medicationTaken(request.getMedicationTaken())
                .medicationName(request.getMedicationName())
                .energyLevel(request.getEnergyLevel())
                .stressLevel(request.getStressLevel())
                .notes(request.getNotes())
                .rawAiExtraction(serializeToJson(request))
                .build();

        // Salvar
        HealthLog saved = healthLogRepository.save(healthLog);

        log.info("✅ Dados de saúde registrados com sucesso - ID: {}", saved.getId());

        // Analisar e criar alertas se necessário
        alertService.analyzeHealthLogAndCreateAlerts(tenantId, patientId, saved);

        // Retornar mensagem de confirmação para a IA
        return buildConfirmationMessage(request);
    }

    /**
     * Busca últimos N logs de um paciente
     * Útil para contexto da IA
     */
    public List<HealthLog> getRecentLogs(UUID tenantId, UUID patientId, int limit) {
        return healthLogRepository.findLastNLogs(patientId, tenantId, PageRequest.of(0, limit));
    }

    /**
     * Busca logs de hoje
     */
    public List<HealthLog> getTodayLogs(UUID tenantId, UUID patientId) {
        return healthLogRepository.findTodayLogs(patientId, tenantId);
    }

    /**
     * Calcula média de dor em um período
     */
    public Double getAveragePain(UUID tenantId, UUID patientId, LocalDateTime start, LocalDateTime end) {
        return healthLogRepository.calculateAveragePain(patientId, tenantId, start, end);
    }

    /**
     * Serializa o request para JSON (auditoria)
     */
    private String serializeToJson(HealthStatsRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar HealthStatsRequest", e);
            return "{}";
        }
    }

    /**
     * Constrói mensagem de confirmação personalizada
     */
    private String buildConfirmationMessage(HealthStatsRequest request) {
        StringBuilder message = new StringBuilder("Registrado com sucesso: ");
        
        List<String> items = new java.util.ArrayList<>();
        
        if (request.getPainLevel() != null) {
            items.add("dor nível " + request.getPainLevel());
        }
        
        if (request.getMood() != null) {
            items.add("humor " + request.getMood());
        }
        
        if (request.getSleepQuality() != null) {
            items.add("sono " + request.getSleepQuality());
        }
        
        if (request.getMedicationTaken() != null) {
            items.add("medicação " + (request.getMedicationTaken() ? "tomada" : "não tomada"));
        }
        
        if (items.isEmpty()) {
            return "Dados registrados com sucesso.";
        }
        
        message.append(String.join(", ", items));
        message.append(".");
        
        return message.toString();
    }
}

