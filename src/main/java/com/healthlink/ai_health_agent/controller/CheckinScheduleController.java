package com.healthlink.ai_health_agent.controller;

import com.healthlink.ai_health_agent.domain.entity.CheckinExecution;
import com.healthlink.ai_health_agent.domain.entity.CheckinSchedule;
import com.healthlink.ai_health_agent.dto.*;
import com.healthlink.ai_health_agent.repository.CheckinExecutionRepository;
import com.healthlink.ai_health_agent.security.TenantContextHolder;
import com.healthlink.ai_health_agent.service.CheckinScheduleService;
import com.healthlink.ai_health_agent.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller para gerenciar agendamentos de check-ins proativos
 */
@RestController
@RequestMapping("/api/checkin-schedules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Check-in Proativo", description = "APIs para gerenciar mensagens proativas automáticas")
public class CheckinScheduleController {

    private final CheckinScheduleService scheduleService;
    private final CheckinExecutionRepository executionRepository;
    private final RateLimitService rateLimitService;

    @PostMapping
    @Operation(summary = "Criar agendamento de check-in",
               description = "Cria um novo agendamento de mensagens proativas para um paciente")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Agendamento criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    })
    public ResponseEntity<CheckinScheduleResponse> createSchedule(
            @Valid @RequestBody CreateCheckinScheduleRequest request) {

        UUID tenantId = TenantContextHolder.getTenantId();
        CheckinSchedule schedule = scheduleService.createSchedule(tenantId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CheckinScheduleResponse.fromEntity(schedule));
    }

    @GetMapping
    @Operation(summary = "Listar agendamentos",
               description = "Lista todos os agendamentos de check-in do tenant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    public ResponseEntity<List<CheckinScheduleResponse>> listSchedules() {
        UUID tenantId = TenantContextHolder.getTenantId();
        List<CheckinSchedule> schedules = scheduleService.listSchedules(tenantId);

        List<CheckinScheduleResponse> response = schedules.stream()
                .map(CheckinScheduleResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{scheduleId}")
    @Operation(summary = "Buscar agendamento por ID",
               description = "Retorna detalhes de um agendamento específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Agendamento encontrado"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado")
    })
    public ResponseEntity<CheckinScheduleResponse> getSchedule(
            @Parameter(description = "ID do agendamento")
            @PathVariable UUID scheduleId) {

        UUID tenantId = TenantContextHolder.getTenantId();
        CheckinSchedule schedule = scheduleService.getSchedule(tenantId, scheduleId);

        return ResponseEntity.ok(CheckinScheduleResponse.fromEntity(schedule));
    }

    @PutMapping("/{scheduleId}")
    @Operation(summary = "Atualizar agendamento",
               description = "Atualiza configurações de um agendamento existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Agendamento atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado")
    })
    public ResponseEntity<CheckinScheduleResponse> updateSchedule(
            @Parameter(description = "ID do agendamento")
            @PathVariable UUID scheduleId,
            @Valid @RequestBody UpdateCheckinScheduleRequest request) {

        UUID tenantId = TenantContextHolder.getTenantId();
        CheckinSchedule schedule = scheduleService.updateSchedule(tenantId, scheduleId, request);

        return ResponseEntity.ok(CheckinScheduleResponse.fromEntity(schedule));
    }

    @PutMapping("/{scheduleId}/toggle")
    @Operation(summary = "Ativar/Desativar agendamento",
               description = "Alterna o status ativo/inativo de um agendamento")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status alterado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado")
    })
    public ResponseEntity<CheckinScheduleResponse> toggleSchedule(
            @Parameter(description = "ID do agendamento")
            @PathVariable UUID scheduleId) {

        UUID tenantId = TenantContextHolder.getTenantId();
        CheckinSchedule schedule = scheduleService.getSchedule(tenantId, scheduleId);

        // Inverter status
        UpdateCheckinScheduleRequest request = new UpdateCheckinScheduleRequest();
        request.setIsActive(!schedule.getIsActive());

        CheckinSchedule updated = scheduleService.updateSchedule(tenantId, scheduleId, request);

        return ResponseEntity.ok(CheckinScheduleResponse.fromEntity(updated));
    }

    @DeleteMapping("/{scheduleId}")
    @Operation(summary = "Deletar agendamento",
               description = "Remove um agendamento de check-in")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Agendamento deletado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado")
    })
    public ResponseEntity<Void> deleteSchedule(
            @Parameter(description = "ID do agendamento")
            @PathVariable UUID scheduleId) {

        UUID tenantId = TenantContextHolder.getTenantId();
        scheduleService.deleteSchedule(tenantId, scheduleId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{scheduleId}/executions")
    @Operation(summary = "Histórico de execuções",
               description = "Lista todas as execuções de um agendamento")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado")
    })
    public ResponseEntity<List<CheckinExecutionResponse>> getExecutions(
            @Parameter(description = "ID do agendamento")
            @PathVariable UUID scheduleId) {

        UUID tenantId = TenantContextHolder.getTenantId();

        // Validar que schedule pertence ao tenant
        scheduleService.getSchedule(tenantId, scheduleId);

        List<CheckinExecution> executions = executionRepository.findByScheduleId(scheduleId);

        List<CheckinExecutionResponse> response = executions.stream()
                .map(CheckinExecutionResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/rate-limit")
    @Operation(summary = "Estatísticas de rate limiting",
               description = "Retorna estatísticas de uso de mensagens do tenant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso")
    })
    public ResponseEntity<RateLimitService.RateLimitStats> getRateLimitStats() {
        UUID tenantId = TenantContextHolder.getTenantId();

        // Buscar account
        var account = scheduleService.listSchedules(tenantId).stream()
                .findFirst()
                .map(CheckinSchedule::getAccount)
                .orElseThrow(() -> new RuntimeException("Tenant não encontrado"));

        RateLimitService.RateLimitStats stats = rateLimitService.getTenantStats(account);

        return ResponseEntity.ok(stats);
    }
}
