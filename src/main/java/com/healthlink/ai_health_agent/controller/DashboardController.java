package com.healthlink.ai_health_agent.controller;

import com.healthlink.ai_health_agent.domain.entity.Alert;
import com.healthlink.ai_health_agent.dto.ConversationSummaryDTO;
import com.healthlink.ai_health_agent.dto.PatientStatsDTO;
import com.healthlink.ai_health_agent.service.AlertService;
import com.healthlink.ai_health_agent.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST para Dashboard de Psicólogos
 * Fornece endpoints para visualização de estatísticas e alertas
 */
@RestController
@RequestMapping("/api/dashboard")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "APIs para visualização de estatísticas, alertas e monitoramento de pacientes")
public class DashboardController {

    private final AnalyticsService analyticsService;
    private final AlertService alertService;

    /**
     * GET /api/dashboard/patients?tenantId=xxx
     * Lista todos os pacientes com estatísticas resumidas
     */
    @Operation(
            summary = "Listar todos os pacientes",
            description = "Retorna estatísticas resumidas de todos os pacientes do tenant, incluindo métricas de saúde, alertas ativos e tendências"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pacientes retornada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PatientStatsDTO.class))),
            @ApiResponse(responseCode = "400", description = "TenantId inválido", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content)
    })
    @GetMapping("/patients")
    public ResponseEntity<List<PatientStatsDTO>> getAllPatientsStats(
            @Parameter(description = "UUID do tenant (Account)", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam UUID tenantId) {

        log.info("📊 Dashboard: Buscando estatísticas de todos os pacientes - Tenant: {}", tenantId);

        List<PatientStatsDTO> stats = analyticsService.getAllPatientsStats(tenantId);

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/dashboard/patients/{patientId}?tenantId=xxx
     * Busca estatísticas detalhadas de um paciente específico
     */
    @Operation(
            summary = "Estatísticas de um paciente",
            description = "Retorna estatísticas detalhadas de um paciente específico, incluindo métricas de saúde, alertas, tendências e score de engajamento"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping("/patients/{patientId}")
    public ResponseEntity<PatientStatsDTO> getPatientStats(
            @Parameter(description = "UUID do paciente", required = true)
            @PathVariable UUID patientId,
            @Parameter(description = "UUID do tenant (Account)", required = true)
            @RequestParam UUID tenantId) {

        log.info("📊 Dashboard: Buscando estatísticas do paciente {} - Tenant: {}", patientId, tenantId);

        PatientStatsDTO stats = analyticsService.calculatePatientStats(tenantId, patientId);

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/dashboard/patients/{patientId}/conversations?tenantId=xxx&startDate=xxx&endDate=xxx
     * Busca resumo de conversas de um paciente
     */
    @GetMapping("/patients/{patientId}/conversations")
    public ResponseEntity<ConversationSummaryDTO> getConversationSummary(
            @PathVariable UUID patientId,
            @RequestParam UUID tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("💬 Dashboard: Buscando conversas do paciente {} - Tenant: {}", patientId, tenantId);
        
        // Padrão: últimos 30 dias
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        ConversationSummaryDTO summary = analyticsService.getConversationSummary(
                tenantId, patientId, startDate, endDate);
        
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/dashboard/alerts?tenantId=xxx
     * Lista todos os alertas ativos do tenant
     */
    @Operation(
            summary = "Listar alertas ativos",
            description = "Retorna todos os alertas não reconhecidos do tenant, ordenados por severidade (CRITICAL > HIGH > MEDIUM > LOW)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de alertas retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping("/alerts")
    public ResponseEntity<List<Alert>> getAllActiveAlerts(
            @Parameter(description = "UUID do tenant (Account)", required = true)
            @RequestParam UUID tenantId) {

        log.info("🚨 Dashboard: Buscando alertas ativos - Tenant: {}", tenantId);

        List<Alert> alerts = alertService.getAllActiveAlerts(tenantId);

        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/dashboard/alerts/critical?tenantId=xxx
     * Lista alertas críticos
     */
    @GetMapping("/alerts/critical")
    public ResponseEntity<List<Alert>> getCriticalAlerts(
            @RequestParam UUID tenantId) {
        
        log.info("🚨 Dashboard: Buscando alertas críticos - Tenant: {}", tenantId);
        
        List<Alert> alerts = alertService.getCriticalAlerts(tenantId);
        
        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/dashboard/patients/{patientId}/alerts?tenantId=xxx
     * Lista alertas ativos de um paciente específico
     */
    @GetMapping("/patients/{patientId}/alerts")
    public ResponseEntity<List<Alert>> getPatientAlerts(
            @PathVariable UUID patientId,
            @RequestParam UUID tenantId) {
        
        log.info("🚨 Dashboard: Buscando alertas do paciente {} - Tenant: {}", patientId, tenantId);
        
        List<Alert> alerts = alertService.getActiveAlerts(tenantId, patientId);
        
        return ResponseEntity.ok(alerts);
    }

    /**
     * POST /api/dashboard/alerts/{alertId}/acknowledge?tenantId=xxx
     * Reconhece um alerta
     */
    @Operation(
            summary = "Reconhecer alerta",
            description = "Marca um alerta como reconhecido/visualizado pelo profissional. Registra quem reconheceu e quando."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerta reconhecido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Alerta não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<Map<String, String>> acknowledgeAlert(
            @Parameter(description = "UUID do alerta", required = true)
            @PathVariable UUID alertId,
            @Parameter(description = "UUID do tenant (Account)", required = true)
            @RequestParam UUID tenantId,
            @Parameter(description = "Corpo da requisição com 'acknowledgedBy' (nome do profissional)")
            @RequestBody(required = false) Map<String, String> body) {

        String acknowledgedBy = body != null ? body.get("acknowledgedBy") : "system";

        log.info("✅ Dashboard: Reconhecendo alerta {} - Por: {}", alertId, acknowledgedBy);

        alertService.acknowledgeAlert(tenantId, alertId, acknowledgedBy);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Alerta reconhecido com sucesso",
                "alertId", alertId.toString()
        ));
    }
}

