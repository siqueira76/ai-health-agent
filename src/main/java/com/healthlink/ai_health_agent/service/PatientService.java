package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.domain.entity.Account;
import com.healthlink.ai_health_agent.domain.entity.Patient;
import com.healthlink.ai_health_agent.repository.AccountRepository;
import com.healthlink.ai_health_agent.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service para gerenciamento de pacientes com validação de limit_slots
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final AccountRepository accountRepository;

    /**
     * Cria um novo paciente com validação de limit_slots
     * 
     * @param patient Paciente a ser criado
     * @param tenantId ID do tenant (account)
     * @return Paciente criado
     * @throws SlotLimitExceededException se o limite de slots foi atingido
     */
    @Transactional
    public Patient createPatient(Patient patient, UUID tenantId) {
        log.info("Criando paciente para tenant: {}", tenantId);

        // 1. Buscar a conta (tenant)
        Account account = accountRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Account não encontrada: " + tenantId));

        // 2. VALIDAÇÃO DE LIMIT_SLOTS
        validateSlotLimit(account);

        // 3. Associar o paciente à conta
        patient.setAccount(account);

        // 4. Validar se o WhatsApp já existe no tenant
        if (patientRepository.existsByWhatsappNumberAndTenantId(patient.getWhatsappNumber(), tenantId)) {
            throw new IllegalArgumentException("Paciente com este WhatsApp já existe neste tenant");
        }

        // 5. Salvar o paciente
        Patient savedPatient = patientRepository.save(patient);
        log.info("Paciente criado com sucesso: {} (Tenant: {})", savedPatient.getId(), tenantId);

        return savedPatient;
    }

    /**
     * Valida se a conta ainda tem slots disponíveis
     * 
     * @param account Conta a ser validada
     * @throws SlotLimitExceededException se o limite foi atingido
     */
    private void validateSlotLimit(Account account) {
        // Contas B2C não têm limite
        if (account.isB2C()) {
            log.debug("Conta B2C não tem limite de slots");
            return;
        }

        // Se limitSlots é null ou 0, não há limite
        if (account.getLimitSlots() == null || account.getLimitSlots() == 0) {
            log.debug("Conta B2B sem limite de slots configurado");
            return;
        }

        // Contar pacientes ativos do tenant
        Long currentPatientCount = patientRepository.countActivePatientsByTenantId(account.getId());

        log.debug("Tenant {} - Pacientes ativos: {} / Limite: {}", 
                  account.getId(), currentPatientCount, account.getLimitSlots());

        // Validar se atingiu o limite
        if (currentPatientCount >= account.getLimitSlots()) {
            log.warn("Limite de slots atingido para tenant {}: {} / {}", 
                     account.getId(), currentPatientCount, account.getLimitSlots());
            
            throw new SlotLimitExceededException(
                String.format("Limite de pacientes atingido: %d/%d. Faça upgrade do seu plano para adicionar mais pacientes.",
                              currentPatientCount, account.getLimitSlots())
            );
        }
    }

    /**
     * Busca paciente por WhatsApp com validação de tenant
     * 
     * @param whatsappNumber Número do WhatsApp
     * @param tenantId ID do tenant
     * @return Paciente encontrado
     */
    public Patient findByWhatsappNumber(String whatsappNumber, UUID tenantId) {
        return patientRepository.findByWhatsappNumberAndTenantId(whatsappNumber, tenantId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
    }

    /**
     * Lista todos os pacientes de um tenant
     * 
     * @param tenantId ID do tenant
     * @return Lista de pacientes
     */
    public List<Patient> findAllByTenant(UUID tenantId) {
        return patientRepository.findAllByTenantId(tenantId);
    }

    /**
     * Lista pacientes ativos de um tenant
     * 
     * @param tenantId ID do tenant
     * @return Lista de pacientes ativos
     */
    public List<Patient> findActivePatients(UUID tenantId) {
        return patientRepository.findActivePatientsByTenantId(tenantId);
    }

    /**
     * Retorna estatísticas de uso de slots
     * 
     * @param tenantId ID do tenant
     * @return Estatísticas de slots
     */
    public SlotUsageStats getSlotUsageStats(UUID tenantId) {
        Account account = accountRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Account não encontrada"));

        Long activePatients = patientRepository.countActivePatientsByTenantId(tenantId);
        Long totalPatients = patientRepository.countByTenantId(tenantId);
        Integer limit = account.getLimitSlots();

        // Se não há limite, retorna null
        if (limit == null || limit == 0) {
            return new SlotUsageStats(activePatients, totalPatients, null, null, false);
        }

        long available = limit - activePatients;
        double usagePercentage = (activePatients.doubleValue() / limit) * 100;
        boolean isAtLimit = activePatients >= limit;

        return new SlotUsageStats(activePatients, totalPatients, limit, available, isAtLimit, usagePercentage);
    }

    /**
     * Desativa um paciente (libera slot)
     * 
     * @param patientId ID do paciente
     * @param tenantId ID do tenant
     */
    @Transactional
    public void deactivatePatient(UUID patientId, UUID tenantId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // Validação de segurança
        if (!patient.getTenantId().equals(tenantId)) {
            throw new SecurityException("Acesso negado: paciente não pertence ao tenant");
        }

        patient.deactivate();
        patientRepository.save(patient);

        log.info("Paciente {} desativado (Tenant: {})", patientId, tenantId);
    }

    /**
     * Reativa um paciente (consome slot)
     * 
     * @param patientId ID do paciente
     * @param tenantId ID do tenant
     */
    @Transactional
    public void reactivatePatient(UUID patientId, UUID tenantId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // Validação de segurança
        if (!patient.getTenantId().equals(tenantId)) {
            throw new SecurityException("Acesso negado: paciente não pertence ao tenant");
        }

        // Validar limite antes de reativar
        Account account = accountRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Account não encontrada"));

        validateSlotLimit(account);

        patient.activate();
        patientRepository.save(patient);

        log.info("Paciente {} reativado (Tenant: {})", patientId, tenantId);
    }

    /**
     * Exception customizada para limite de slots
     */
    public static class SlotLimitExceededException extends RuntimeException {
        public SlotLimitExceededException(String message) {
            super(message);
        }
    }

    /**
     * Record para estatísticas de uso de slots
     */
    public record SlotUsageStats(
            Long activePatients,
            Long totalPatients,
            Integer limit,
            Long available,
            boolean isAtLimit,
            Double usagePercentage
    ) {
        public SlotUsageStats(Long activePatients, Long totalPatients, Integer limit, Long available, boolean isAtLimit) {
            this(activePatients, totalPatients, limit, available, isAtLimit, null);
        }
    }
}

