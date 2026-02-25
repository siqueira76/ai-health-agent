package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.domain.entity.Account;
import com.healthlink.ai_health_agent.domain.entity.CheckinSchedule;
import com.healthlink.ai_health_agent.domain.entity.Patient;
import com.healthlink.ai_health_agent.dto.CreateCheckinScheduleRequest;
import com.healthlink.ai_health_agent.dto.UpdateCheckinScheduleRequest;
import com.healthlink.ai_health_agent.repository.AccountRepository;
import com.healthlink.ai_health_agent.repository.CheckinScheduleRepository;
import com.healthlink.ai_health_agent.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service para gerenciar agendamentos de check-ins proativos
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CheckinScheduleService {

    private final CheckinScheduleRepository scheduleRepository;
    private final PatientRepository patientRepository;
    private final AccountRepository accountRepository;

    /**
     * Cria novo agendamento de check-in
     */
    @Transactional
    public CheckinSchedule createSchedule(UUID tenantId, CreateCheckinScheduleRequest request) {
        log.info("Criando agendamento de check-in - Tenant: {}, Patient: {}", 
                 tenantId, request.getPatientId());

        // Validar tenant
        Account account = accountRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Account não encontrada"));

        // Validar paciente
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // Validar que paciente pertence ao tenant
        if (!patient.getAccount().getId().equals(tenantId)) {
            throw new RuntimeException("Paciente não pertence a este tenant");
        }

        // Criar schedule
        CheckinSchedule schedule = CheckinSchedule.builder()
                .account(account)
                .patient(patient)
                .scheduleType(request.getScheduleType())
                .timeOfDay(request.getTimeOfDay())
                .daysOfWeek(request.getDaysOfWeek())
                .timezone(request.getTimezone())
                .customMessage(request.getCustomMessage())
                .useAiGeneration(request.getUseAiGeneration())
                .maxMessagesPerDay(request.getMaxMessagesPerDay())
                .isActive(request.getIsActive())
                .messagesSentToday(0)
                .lastResetDate(LocalDate.now())
                .nextExecutionAt(calculateFirstExecution(request))
                .build();

        CheckinSchedule saved = scheduleRepository.save(schedule);
        log.info("Agendamento criado com sucesso - ID: {}", saved.getId());

        return saved;
    }

    /**
     * Atualiza agendamento existente
     */
    @Transactional
    public CheckinSchedule updateSchedule(
            UUID tenantId, 
            UUID scheduleId, 
            UpdateCheckinScheduleRequest request) {
        
        log.info("Atualizando agendamento - ID: {}", scheduleId);

        CheckinSchedule schedule = scheduleRepository.findByIdAndTenant(scheduleId, tenantId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        // Atualizar campos (apenas se fornecidos)
        if (request.getScheduleType() != null) {
            schedule.setScheduleType(request.getScheduleType());
        }
        if (request.getTimeOfDay() != null) {
            schedule.setTimeOfDay(request.getTimeOfDay());
        }
        if (request.getDaysOfWeek() != null) {
            schedule.setDaysOfWeek(request.getDaysOfWeek());
        }
        if (request.getTimezone() != null) {
            schedule.setTimezone(request.getTimezone());
        }
        if (request.getCustomMessage() != null) {
            schedule.setCustomMessage(request.getCustomMessage());
        }
        if (request.getUseAiGeneration() != null) {
            schedule.setUseAiGeneration(request.getUseAiGeneration());
        }
        if (request.getMaxMessagesPerDay() != null) {
            schedule.setMaxMessagesPerDay(request.getMaxMessagesPerDay());
        }
        if (request.getIsActive() != null) {
            schedule.setIsActive(request.getIsActive());
        }

        CheckinSchedule updated = scheduleRepository.save(schedule);
        log.info("Agendamento atualizado com sucesso");

        return updated;
    }

    /**
     * Lista agendamentos de um tenant
     */
    public List<CheckinSchedule> listSchedules(UUID tenantId) {
        return scheduleRepository.findByTenantId(tenantId);
    }

    /**
     * Busca agendamento por ID
     */
    public CheckinSchedule getSchedule(UUID tenantId, UUID scheduleId) {
        return scheduleRepository.findByIdAndTenant(scheduleId, tenantId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));
    }

    /**
     * Deleta agendamento
     */
    @Transactional
    public void deleteSchedule(UUID tenantId, UUID scheduleId) {
        CheckinSchedule schedule = scheduleRepository.findByIdAndTenant(scheduleId, tenantId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        scheduleRepository.delete(schedule);
        log.info("Agendamento deletado - ID: {}", scheduleId);
    }

    /**
     * Calcula primeira execução do agendamento
     */
    private LocalDateTime calculateFirstExecution(CreateCheckinScheduleRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = now.toLocalDate().atTime(request.getTimeOfDay());

        // Se já passou do horário hoje, agendar para amanhã
        if (candidate.isBefore(now)) {
            candidate = candidate.plusDays(1);
        }

        return candidate;
    }
}

