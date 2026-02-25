package com.healthlink.ai_health_agent.dto;

import com.healthlink.ai_health_agent.domain.entity.CheckinSchedule.ScheduleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalTime;

/**
 * Request para atualizar um agendamento de check-in
 */
@Data
@Schema(description = "Request para atualizar agendamento de check-in")
public class UpdateCheckinScheduleRequest {

    @Schema(description = "Tipo de agendamento", example = "DAILY")
    private ScheduleType scheduleType;

    @Schema(description = "Horário de execução", example = "09:00:00")
    private LocalTime timeOfDay;

    @Schema(description = "Dias da semana (1=Segunda, 7=Domingo)", example = "[1,2,3,4,5]")
    private int[] daysOfWeek;

    @Schema(description = "Timezone", example = "America/Sao_Paulo")
    private String timezone;

    @Schema(description = "Mensagem customizada", example = "Olá! Como você está?")
    private String customMessage;

    @Schema(description = "Usar IA para gerar mensagens", example = "true")
    private Boolean useAiGeneration;

    @Min(1)
    @Max(10)
    @Schema(description = "Máximo de mensagens por dia", example = "3")
    private Integer maxMessagesPerDay;

    @Schema(description = "Ativar/desativar agendamento", example = "true")
    private Boolean isActive;
}

