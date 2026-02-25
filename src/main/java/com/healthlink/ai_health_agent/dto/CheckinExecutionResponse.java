package com.healthlink.ai_health_agent.dto;

import com.healthlink.ai_health_agent.domain.entity.CheckinExecution;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response com dados de uma execução de check-in
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados de uma execução de check-in")
public class CheckinExecutionResponse {

    @Schema(description = "ID da execução")
    private UUID id;

    @Schema(description = "ID do agendamento")
    private UUID scheduleId;

    @Schema(description = "ID do paciente")
    private UUID patientId;

    @Schema(description = "Nome do paciente")
    private String patientName;

    @Schema(description = "Data/hora da execução")
    private LocalDateTime executedAt;

    @Schema(description = "Status da execução")
    private String status;

    @Schema(description = "Motivo da falha (se houver)")
    private String failureReason;

    @Schema(description = "Mensagem enviada")
    private String messageSent;

    @Schema(description = "ID da mensagem no WhatsApp")
    private String messageId;

    @Schema(description = "Paciente respondeu")
    private Boolean patientResponded;

    @Schema(description = "Data/hora da resposta")
    private LocalDateTime responseReceivedAt;

    @Schema(description = "Duração da execução (ms)")
    private Integer executionDurationMs;

    /**
     * Converte entidade para DTO
     */
    public static CheckinExecutionResponse fromEntity(CheckinExecution execution) {
        return CheckinExecutionResponse.builder()
                .id(execution.getId())
                .scheduleId(execution.getSchedule().getId())
                .patientId(execution.getPatient().getId())
                .patientName(execution.getPatient().getName())
                .executedAt(execution.getExecutedAt())
                .status(execution.getStatus())
                .failureReason(execution.getFailureReason())
                .messageSent(execution.getMessageSent())
                .messageId(execution.getMessageId())
                .patientResponded(execution.getPatientResponded())
                .responseReceivedAt(execution.getResponseReceivedAt())
                .executionDurationMs(execution.getExecutionDurationMs())
                .build();
    }
}

