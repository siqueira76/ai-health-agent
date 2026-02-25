package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.domain.entity.Account;
import com.healthlink.ai_health_agent.domain.entity.CheckinSchedule;
import com.healthlink.ai_health_agent.domain.enums.AccountType;
import com.healthlink.ai_health_agent.repository.CheckinScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Service responsável por gerenciar rate limiting de mensagens proativas.
 * Implementa controle em 3 níveis:
 * 1. Por Paciente (max_messages_per_day)
 * 2. Por Tenant (baseado no tipo de conta)
 * 3. Global (opcional, para controle de custos)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitService {

    private final CheckinScheduleRepository scheduleRepository;

    // Limites por tipo de conta
    private static final int B2B_DAILY_LIMIT = 100;
    private static final int B2C_DAILY_LIMIT = 50;
    private static final int GLOBAL_HOURLY_LIMIT = 1000; // Futuro

    /**
     * Verifica se pode enviar check-in
     * Aplica rate limiting em múltiplos níveis
     */
    public boolean canSendCheckin(CheckinSchedule schedule) {
        // Nível 1: Verificar limite do paciente
        if (!checkPatientLimit(schedule)) {
            log.warn("Rate limit atingido - Paciente: {} ({})", 
                     schedule.getPatient().getName(), 
                     schedule.getPatient().getId());
            return false;
        }

        // Nível 2: Verificar limite do tenant
        if (!checkTenantLimit(schedule.getAccount())) {
            log.warn("Rate limit atingido - Tenant: {} ({})", 
                     schedule.getAccount().getName(), 
                     schedule.getAccount().getId());
            return false;
        }

        // Nível 3: Verificar limite global (opcional)
        // if (!checkGlobalLimit()) {
        //     log.warn("Rate limit global atingido");
        //     return false;
        // }

        return true;
    }

    /**
     * Verifica limite de mensagens do paciente
     */
    private boolean checkPatientLimit(CheckinSchedule schedule) {
        // Reset contador diário
        if (!schedule.getLastResetDate().equals(LocalDate.now())) {
            schedule.setMessagesSentToday(0);
            schedule.setLastResetDate(LocalDate.now());
            scheduleRepository.save(schedule);
        }

        int sent = schedule.getMessagesSentToday();
        int limit = schedule.getMaxMessagesPerDay();

        log.debug("Paciente {} - Mensagens enviadas hoje: {}/{}", 
                  schedule.getPatient().getName(), sent, limit);

        return sent < limit;
    }

    /**
     * Verifica limite de mensagens do tenant
     */
    private boolean checkTenantLimit(Account account) {
        // Contar mensagens enviadas hoje pelo tenant
        long count = scheduleRepository.countMessagesSentTodayByTenant(
            account.getId(),
            LocalDate.now()
        );

        // Determinar limite baseado no tipo de conta
        int limit = account.getType() == AccountType.B2B
                    ? B2B_DAILY_LIMIT
                    : B2C_DAILY_LIMIT;

        log.debug("Tenant {} - Mensagens enviadas hoje: {}/{}", 
                  account.getName(), count, limit);

        return count < limit;
    }

    /**
     * Verifica limite global (futuro)
     * Pode ser implementado com Redis para controle em tempo real
     */
    private boolean checkGlobalLimit() {
        // TODO: Implementar com Redis ou cache distribuído
        // Para controle de custos em tempo real
        return true;
    }

    /**
     * Reseta contadores diários de todos os schedules
     * Pode ser chamado por um job separado à meia-noite
     */
    public void resetDailyCounters() {
        LocalDate today = LocalDate.now();
        var schedules = scheduleRepository.findSchedulesNeedingReset(today);

        log.info("Resetando contadores diários de {} schedules", schedules.size());

        for (var schedule : schedules) {
            schedule.setMessagesSentToday(0);
            schedule.setLastResetDate(today);
        }

        scheduleRepository.saveAll(schedules);
        log.info("Contadores resetados com sucesso");
    }

    /**
     * Obtém estatísticas de uso de um tenant
     */
    public RateLimitStats getTenantStats(Account account) {
        long messagesUsed = scheduleRepository.countMessagesSentTodayByTenant(
            account.getId(),
            LocalDate.now()
        );

        int limit = account.getType() == AccountType.B2B
                    ? B2B_DAILY_LIMIT
                    : B2C_DAILY_LIMIT;

        return new RateLimitStats(
            messagesUsed,
            limit,
            limit - messagesUsed,
            (double) messagesUsed / limit * 100
        );
    }

    /**
     * DTO para estatísticas de rate limiting
     */
    public record RateLimitStats(
        long messagesUsed,
        int dailyLimit,
        long remaining,
        double usagePercentage
    ) {}
}

