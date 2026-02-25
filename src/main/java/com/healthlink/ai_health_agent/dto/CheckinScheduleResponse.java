package com.healthlink.ai_health_agent.dto;

import com.healthlink.ai_health_agent.domain.entity.CheckinSchedule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Response com dados de um agendamento de check-in
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados de um agendamento de check-in")
public class CheckinScheduleResponse {

    @Schema(description = "ID do agendamento")
    private UUID id;

    @Schema(description = "ID do paciente")
    private UUID patientId;

    @Schema(description = "Nome do paciente")
    private String patientName;

    @Schema(description = "Tipo de agendamento")
    private String scheduleType;

    @Schema(description = "Horário de execução")
    private LocalTime timeOfDay;

    @Schema(description = "Dias da semana")
    private int[] daysOfWeek;

    @Schema(description = "Timezone")
    private String timezone;

    @Schema(description = "Mensagem customizada")
    private String customMessage;

    @Schema(description = "Usa IA para gerar mensagens")
    private Boolean useAiGeneration;

    @Schema(description = "Máximo de mensagens por dia")
    private Integer maxMessagesPerDay;

    @Schema(description = "Mensagens enviadas hoje")
    private Integer messagesSentToday;

    @Schema(description = "Agendamento ativo")
    private Boolean isActive;

    @Schema(description = "Última execução")
    private LocalDateTime lastExecutionAt;

    @Schema(description = "Próxima execução")
    private LocalDateTime nextExecutionAt;

    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    /**
     * Converte entidade para DTO
     */
    public static CheckinScheduleResponse fromEntity(CheckinSchedule schedule) {
        return CheckinScheduleResponse.builder()
                .id(schedule.getId())
                .patientId(schedule.getPatient().getId())
                .patientName(schedule.getPatient().getName())
                .scheduleType(schedule.getScheduleType().name())
                .timeOfDay(schedule.getTimeOfDay())
                .daysOfWeek(schedule.getDaysOfWeek())
                .timezone(schedule.getTimezone())
                .customMessage(schedule.getCustomMessage())
                .useAiGeneration(schedule.getUseAiGeneration())
                .maxMessagesPerDay(schedule.getMaxMessagesPerDay())
                .messagesSentToday(schedule.getMessagesSentToday())
                .isActive(schedule.getIsActive())
                .lastExecutionAt(schedule.getLastExecutionAt())
                .nextExecutionAt(schedule.getNextExecutionAt())
                .createdAt(schedule.getCreatedAt())
                .build();
    }
}

