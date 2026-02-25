package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.domain.entity.Account;
import com.healthlink.ai_health_agent.domain.entity.Patient;
import com.healthlink.ai_health_agent.domain.enums.AccountStatus;
import com.healthlink.ai_health_agent.domain.enums.AccountType;
import com.healthlink.ai_health_agent.repository.AccountRepository;
import com.healthlink.ai_health_agent.repository.PatientRepository;
import com.healthlink.ai_health_agent.service.PatientService.SlotLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para PatientService
 * Foco na validação de limit_slots
 */
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private PatientService patientService;

    private Account b2bAccount;
    private Account b2cAccount;
    private Patient patient;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();

        // Conta B2B com limite de 5 slots
        b2bAccount = Account.builder()
                .id(tenantId)
                .cpf("12345678900")
                .name("Dr. João Silva")
                .email("joao@example.com")
                .type(AccountType.B2B)
                .status(AccountStatus.ACTIVE)
                .limitSlots(5)
                .build();

        // Conta B2C sem limite
        b2cAccount = Account.builder()
                .id(UUID.randomUUID())
                .cpf("98765432100")
                .name("Maria Santos")
                .email("maria@example.com")
                .type(AccountType.B2C)
                .status(AccountStatus.ACTIVE)
                .limitSlots(null)
                .build();

        // Paciente de exemplo
        patient = Patient.builder()
                .whatsappNumber("5511999999999")
                .name("Paciente Teste")
                .email("paciente@example.com")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve criar paciente quando há slots disponíveis")
    void shouldCreatePatientWhenSlotsAvailable() {
        // Arrange
        when(accountRepository.findById(tenantId)).thenReturn(Optional.of(b2bAccount));
        when(patientRepository.countActivePatientsByTenantId(tenantId)).thenReturn(3L); // 3/5 slots usados
        when(patientRepository.existsByWhatsappNumberAndTenantId(any(), any())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        // Act
        Patient result = patientService.createPatient(patient, tenantId);

        // Assert
        assertNotNull(result);
        verify(patientRepository).save(patient);
        verify(patientRepository).countActivePatientsByTenantId(tenantId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando limite de slots for atingido")
    void shouldThrowExceptionWhenSlotLimitReached() {
        // Arrange
        when(accountRepository.findById(tenantId)).thenReturn(Optional.of(b2bAccount));
        when(patientRepository.countActivePatientsByTenantId(tenantId)).thenReturn(5L); // 5/5 slots usados

        // Act & Assert
        SlotLimitExceededException exception = assertThrows(
                SlotLimitExceededException.class,
                () -> patientService.createPatient(patient, tenantId)
        );

        assertTrue(exception.getMessage().contains("Limite de pacientes atingido"));
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar paciente em conta B2C sem validar limite")
    void shouldCreatePatientInB2CAccountWithoutLimit() {
        // Arrange
        UUID b2cTenantId = b2cAccount.getId();
        when(accountRepository.findById(b2cTenantId)).thenReturn(Optional.of(b2cAccount));
        when(patientRepository.existsByWhatsappNumberAndTenantId(any(), any())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        // Act
        Patient result = patientService.createPatient(patient, b2cTenantId);

        // Assert
        assertNotNull(result);
        verify(patientRepository).save(patient);
        // Não deve verificar limite para B2C
        verify(patientRepository, never()).countActivePatientsByTenantId(any());
    }

    @Test
    @DisplayName("Deve criar paciente em conta B2B sem limite configurado")
    void shouldCreatePatientInB2BAccountWithoutConfiguredLimit() {
        // Arrange
        b2bAccount.setLimitSlots(null); // Sem limite
        when(accountRepository.findById(tenantId)).thenReturn(Optional.of(b2bAccount));
        when(patientRepository.existsByWhatsappNumberAndTenantId(any(), any())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        // Act
        Patient result = patientService.createPatient(patient, tenantId);

        // Assert
        assertNotNull(result);
        verify(patientRepository).save(patient);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar reativar paciente quando limite atingido")
    void shouldThrowExceptionWhenReactivatingPatientAtLimit() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        patient.setId(patientId);
        patient.setAccount(b2bAccount);
        patient.setIsActive(false);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(accountRepository.findById(tenantId)).thenReturn(Optional.of(b2bAccount));
        when(patientRepository.countActivePatientsByTenantId(tenantId)).thenReturn(5L); // Limite atingido

        // Act & Assert
        assertThrows(
                SlotLimitExceededException.class,
                () -> patientService.reactivatePatient(patientId, tenantId)
        );

        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar estatísticas corretas de uso de slots")
    void shouldReturnCorrectSlotUsageStats() {
        // Arrange
        when(accountRepository.findById(tenantId)).thenReturn(Optional.of(b2bAccount));
        when(patientRepository.countActivePatientsByTenantId(tenantId)).thenReturn(3L);
        when(patientRepository.countByTenantId(tenantId)).thenReturn(4L);

        // Act
        PatientService.SlotUsageStats stats = patientService.getSlotUsageStats(tenantId);

        // Assert
        assertEquals(3L, stats.activePatients());
        assertEquals(4L, stats.totalPatients());
        assertEquals(5, stats.limit());
        assertEquals(2L, stats.available());
        assertFalse(stats.isAtLimit());
        assertEquals(60.0, stats.usagePercentage(), 0.01);
    }
}

