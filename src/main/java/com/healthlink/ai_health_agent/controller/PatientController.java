package com.healthlink.ai_health_agent.controller;

import com.healthlink.ai_health_agent.domain.entity.Patient;
import com.healthlink.ai_health_agent.service.PatientService;
import com.healthlink.ai_health_agent.service.PatientService.SlotLimitExceededException;
import com.healthlink.ai_health_agent.service.PatientService.SlotUsageStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller para gerenciamento de pacientes
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService patientService;

    /**
     * Cria um novo paciente
     * 
     * POST /api/patients?tenantId={tenantId}
     */
    @PostMapping
    public ResponseEntity<?> createPatient(
            @RequestBody Patient patient,
            @RequestParam UUID tenantId) {
        
        try {
            Patient createdPatient = patientService.createPatient(patient, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPatient);
            
        } catch (SlotLimitExceededException e) {
            log.warn("Limite de slots atingido para tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "SLOT_LIMIT_EXCEEDED",
                    "message", e.getMessage()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "VALIDATION_ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Lista todos os pacientes de um tenant
     * 
     * GET /api/patients?tenantId={tenantId}
     */
    @GetMapping
    public ResponseEntity<List<Patient>> listPatients(@RequestParam UUID tenantId) {
        List<Patient> patients = patientService.findAllByTenant(tenantId);
        return ResponseEntity.ok(patients);
    }

    /**
     * Lista pacientes ativos de um tenant
     * 
     * GET /api/patients/active?tenantId={tenantId}
     */
    @GetMapping("/active")
    public ResponseEntity<List<Patient>> listActivePatients(@RequestParam UUID tenantId) {
        List<Patient> patients = patientService.findActivePatients(tenantId);
        return ResponseEntity.ok(patients);
    }

    /**
     * Busca paciente por WhatsApp
     * 
     * GET /api/patients/whatsapp/{whatsappNumber}?tenantId={tenantId}
     */
    @GetMapping("/whatsapp/{whatsappNumber}")
    public ResponseEntity<Patient> findByWhatsapp(
            @PathVariable String whatsappNumber,
            @RequestParam UUID tenantId) {
        
        Patient patient = patientService.findByWhatsappNumber(whatsappNumber, tenantId);
        return ResponseEntity.ok(patient);
    }

    /**
     * Retorna estatísticas de uso de slots
     * 
     * GET /api/patients/slots/stats?tenantId={tenantId}
     */
    @GetMapping("/slots/stats")
    public ResponseEntity<SlotUsageStats> getSlotStats(@RequestParam UUID tenantId) {
        SlotUsageStats stats = patientService.getSlotUsageStats(tenantId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Desativa um paciente
     * 
     * PUT /api/patients/{patientId}/deactivate?tenantId={tenantId}
     */
    @PutMapping("/{patientId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivatePatient(
            @PathVariable UUID patientId,
            @RequestParam UUID tenantId) {
        
        patientService.deactivatePatient(patientId, tenantId);
        return ResponseEntity.ok(Map.of(
                "message", "Paciente desativado com sucesso",
                "patientId", patientId.toString()
        ));
    }

    /**
     * Reativa um paciente
     * 
     * PUT /api/patients/{patientId}/reactivate?tenantId={tenantId}
     */
    @PutMapping("/{patientId}/reactivate")
    public ResponseEntity<?> reactivatePatient(
            @PathVariable UUID patientId,
            @RequestParam UUID tenantId) {
        
        try {
            patientService.reactivatePatient(patientId, tenantId);
            return ResponseEntity.ok(Map.of(
                    "message", "Paciente reativado com sucesso",
                    "patientId", patientId.toString()
            ));
            
        } catch (SlotLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "SLOT_LIMIT_EXCEEDED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Exception handler para erros de segurança
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "ACCESS_DENIED",
                "message", e.getMessage()
        ));
    }

    /**
     * Exception handler para recursos não encontrados
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleNotFoundException(RuntimeException e) {
        if (e.getMessage().contains("não encontrad")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", e.getMessage()
            ));
        }
        throw e;
    }
}

