package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.domain.entity.CheckinExecution;
import com.healthlink.ai_health_agent.domain.entity.CheckinSchedule;
import com.healthlink.ai_health_agent.repository.CheckinExecutionRepository;
import com.healthlink.ai_health_agent.repository.CheckinScheduleRepository;
import com.healthlink.ai_health_agent.security.TenantContext;
import com.healthlink.ai_health_agent.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.ai.chat.messages.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service responsável por executar check-ins proativos agendados.
 * Usa ShedLock para garantir execução única em ambientes com múltiplas instâncias.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProactiveCheckinService {

    private final CheckinScheduleRepository scheduleRepository;
    private final CheckinExecutionRepository executionRepository;
    private final AIService aiService;
    private final EvolutionApiService evolutionApiService;
    private final ChatHistoryService chatHistoryService;
    private final RateLimitService rateLimitService;

    /**
     * Executa check-ins agendados
     * Roda a cada 1 minuto, mas com lock distribuído (ShedLock)
     */
    @Scheduled(cron = "0 * * * * *") // A cada minuto
    @SchedulerLock(
        name = "proactiveCheckinJob",
        lockAtMostFor = "50s",
        lockAtLeastFor = "10s"
    )
    @Transactional
    public void executeScheduledCheckins() {
        log.info("🤖 Iniciando execução de check-ins proativos");

        LocalDateTime now = LocalDateTime.now();

        // Buscar agendamentos prontos para execução
        List<CheckinSchedule> schedules = scheduleRepository
                .findSchedulesReadyForExecution(now);

        log.info("📊 Encontrados {} check-ins para executar", schedules.size());

        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;

        for (CheckinSchedule schedule : schedules) {
            try {
                long startTime = System.currentTimeMillis();
                ExecutionResult result = executeCheckin(schedule);
                long duration = System.currentTimeMillis() - startTime;

                switch (result) {
                    case SUCCESS -> successCount++;
                    case FAILED -> failedCount++;
                    case SKIPPED -> skippedCount++;
                }

                log.debug("Check-in executado em {}ms - Status: {}", duration, result);

            } catch (Exception e) {
                log.error("❌ Erro ao executar check-in: {}", schedule.getId(), e);
                recordFailedExecution(schedule, e.getMessage(), 0);
                failedCount++;
            }
        }

        log.info("✅ Check-ins executados - Success: {}, Failed: {}, Skipped: {}",
                 successCount, failedCount, skippedCount);
    }

    /**
     * Executa um check-in individual
     */
    private ExecutionResult executeCheckin(CheckinSchedule schedule) {
        log.info("🚀 Executando check-in - Schedule: {}, Patient: {}",
                 schedule.getId(), schedule.getPatient().getName());

        // PASSO 1: Verificar rate limiting
        if (!rateLimitService.canSendCheckin(schedule)) {
            log.warn("⏭️ Check-in pulado - Rate limit atingido");
            recordSkippedExecution(schedule, "Rate limit exceeded");
            return ExecutionResult.SKIPPED;
        }

        // PASSO 2: Estabelecer contexto de tenant
        TenantContext context = new TenantContext(
                schedule.getAccount().getId(),
                schedule.getAccount().getType(),
                schedule.getAccount().getCustomPrompt()
        );
        TenantContextHolder.setContext(context);

        try {
            long startTime = System.currentTimeMillis();

            // PASSO 3: Gerar mensagem proativa
            String message = generateProactiveMessage(schedule);

            // PASSO 4: Enviar via Evolution API
            sendProactiveMessage(schedule, message);

            long duration = System.currentTimeMillis() - startTime;

            // PASSO 5: Registrar execução bem-sucedida
            String messageId = "PROACTIVE_" + System.currentTimeMillis(); // ID gerado localmente
            recordSuccessfulExecution(schedule, message, messageId, (int) duration);

            // PASSO 6: Atualizar próxima execução
            updateNextExecution(schedule);

            log.info("✅ Check-in enviado com sucesso - MessageId: {}", messageId);
            return ExecutionResult.SUCCESS;

        } catch (Exception e) {
            log.error("❌ Erro ao processar check-in", e);
            long duration = System.currentTimeMillis() - System.currentTimeMillis();
            recordFailedExecution(schedule, e.getMessage(), (int) duration);
            return ExecutionResult.FAILED;

        } finally {
            // SEMPRE limpar contexto
            TenantContextHolder.clear();
        }
    }

    /**
     * Gera mensagem proativa usando IA ou mensagem customizada
     */
    private String generateProactiveMessage(CheckinSchedule schedule) {
        if (!schedule.getUseAiGeneration() && schedule.getCustomMessage() != null) {
            return schedule.getCustomMessage();
        }

        // Buscar histórico recente (últimas 5 mensagens)
        List<Message> recentHistory = chatHistoryService.loadRecentMessages(
                schedule.getAccount().getId(),
                schedule.getPatient().getId(),
                5
        );

        // Criar prompt para IA
        String systemPrompt = buildProactiveSystemPrompt(schedule);

        // Gerar mensagem com IA
        return aiService.generateProactiveMessage(
                schedule.getAccount().getId(),
                schedule.getPatient().getId(),
                systemPrompt,
                recentHistory
        );
    }

    /**
     * Constrói prompt do sistema para mensagem proativa
     */
    private String buildProactiveSystemPrompt(CheckinSchedule schedule) {
        String basePrompt = schedule.getAccount().getCustomPrompt();

        return basePrompt + """


                CONTEXTO ADICIONAL - MENSAGEM PROATIVA:
                Você está iniciando uma conversa proativa com o paciente.
                Seja empático, breve e direto.
                Pergunte como o paciente está se sentindo hoje.
                Mencione o histórico recente se relevante.

                Exemplo: "Bom dia! Como você está se sentindo hoje?
                Vi que ontem você mencionou dor nível 7. Melhorou?"
                """;
    }

    /**
     * Envia mensagem proativa via Evolution API
     */
    private void sendProactiveMessage(CheckinSchedule schedule, String message) {
        evolutionApiService.sendMessage(
                schedule.getPatient().getWhatsappNumber(),
                message
        );
    }

    /**
     * Registra execução bem-sucedida
     */
    private void recordSuccessfulExecution(
            CheckinSchedule schedule,
            String message,
            String messageId,
            int durationMs) {

        CheckinExecution execution = CheckinExecution.builder()
                .schedule(schedule)
                .account(schedule.getAccount())
                .patient(schedule.getPatient())
                .status("SUCCESS")
                .messageSent(message)
                .messageId(messageId)
                .executionDurationMs(durationMs)
                .build();

        executionRepository.save(execution);

        // Incrementar contador de mensagens
        schedule.setMessagesSentToday(schedule.getMessagesSentToday() + 1);
        schedule.setLastExecutionAt(LocalDateTime.now());
        scheduleRepository.save(schedule);
    }

    /**
     * Registra execução com falha
     */
    private void recordFailedExecution(CheckinSchedule schedule, String reason, int durationMs) {
        CheckinExecution execution = CheckinExecution.builder()
                .schedule(schedule)
                .account(schedule.getAccount())
                .patient(schedule.getPatient())
                .status("FAILED")
                .failureReason(reason)
                .executionDurationMs(durationMs)
                .build();

        executionRepository.save(execution);
    }

    /**
     * Registra execução pulada (rate limit)
     */
    private void recordSkippedExecution(CheckinSchedule schedule, String reason) {
        CheckinExecution execution = CheckinExecution.builder()
                .schedule(schedule)
                .account(schedule.getAccount())
                .patient(schedule.getPatient())
                .status("SKIPPED")
                .failureReason(reason)
                .build();

        executionRepository.save(execution);
    }

    /**
     * Atualiza próxima execução do schedule
     */
    private void updateNextExecution(CheckinSchedule schedule) {
        LocalDateTime next = calculateNextExecution(schedule);
        schedule.setNextExecutionAt(next);
        scheduleRepository.save(schedule);

        log.debug("Próxima execução agendada para: {}", next);
    }

    /**
     * Calcula próxima execução baseado no tipo de schedule
     */
    private LocalDateTime calculateNextExecution(CheckinSchedule schedule) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime timeOfDay = schedule.getTimeOfDay();

        switch (schedule.getScheduleType()) {
            case DAILY:
                return now.plusDays(1).with(timeOfDay);

            case WEEKLY:
                return findNextWeeklyExecution(now, timeOfDay, schedule.getDaysOfWeek());

            default:
                return now.plusDays(1).with(timeOfDay);
        }
    }

    /**
     * Encontra próxima execução para schedule semanal
     */
    private LocalDateTime findNextWeeklyExecution(
            LocalDateTime now,
            LocalTime timeOfDay,
            int[] daysOfWeek) {

        if (daysOfWeek == null || daysOfWeek.length == 0) {
            return now.plusDays(1).with(timeOfDay);
        }

        LocalDateTime candidate = now.plusDays(1).with(timeOfDay);

        for (int i = 0; i < 7; i++) {
            int dayValue = candidate.getDayOfWeek().getValue();

            for (int allowedDay : daysOfWeek) {
                if (dayValue == allowedDay) {
                    return candidate;
                }
            }

            candidate = candidate.plusDays(1);
        }

        return now.plusDays(1).with(timeOfDay);
    }

    /**
     * Enum para resultado de execução
     */
    private enum ExecutionResult {
        SUCCESS, FAILED, SKIPPED
    }
}
