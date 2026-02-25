package com.healthlink.ai_health_agent.dto;

import com.healthlink.ai_health_agent.domain.entity.CheckinSchedule.ScheduleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Request para criar um novo agendamento de check-in proativo
 */
@Data
@Schema(description = "Request para criar agendamento de check-in proativo")
public class CreateCheckinScheduleRequest {

    @NotNull(message = "Patient ID é obrigatório")
    @Schema(description = "ID do paciente", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID patientId;

    @NotNull(message = "Tipo de agendamento é obrigatório")
    @Schema(description = "Tipo de agendamento", example = "DAILY")
    private ScheduleType scheduleType;

    @NotNull(message = "Horário é obrigatório")
    @Schema(description = "Horário de execução", example = "09:00:00")
    private LocalTime timeOfDay;

    @Schema(description = "Dias da semana (1=Segunda, 7=Domingo)", example = "[1,2,3,4,5]")
    private int[] daysOfWeek;

    @Schema(description = "Timezone", example = "America/Sao_Paulo")
    private String timezone = "America/Sao_Paulo";

    @Schema(description = "Mensagem customizada (se não usar IA)", example = "Olá! Como você está se sentindo hoje?")
    private String customMessage;

    @Schema(description = "Usar IA para gerar mensagens", example = "true")
    private Boolean useAiGeneration = true;

    @Min(1)
    @Max(10)
    @Schema(description = "Máximo de mensagens por dia", example = "3")
    private Integer maxMessagesPerDay = 3;

    @Schema(description = "Ativar agendamento imediatamente", example = "true")
    private Boolean isActive = true;
}

