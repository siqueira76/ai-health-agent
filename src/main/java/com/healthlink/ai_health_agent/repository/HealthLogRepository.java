package com.healthlink.ai_health_agent.repository;

import com.healthlink.ai_health_agent.domain.entity.HealthLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para HealthLog com isolamento multi-tenant
 */
@Repository
public interface HealthLogRepository extends JpaRepository<HealthLog, UUID> {

    /**
     * Buscar logs de um paciente específico (com tenant)
     * SEMPRE usar este método para garantir isolamento
     */
    @Query("SELECT h FROM HealthLog h WHERE h.patient.id = :patientId AND h.account.id = :tenantId ORDER BY h.timestamp DESC")
    List<HealthLog> findByPatientAndTenant(
            @Param("patientId") UUID patientId, 
            @Param("tenantId") UUID tenantId
    );

    /**
     * Buscar últimos N logs de um paciente
     * Útil para contexto da IA
     */
    @Query("SELECT h FROM HealthLog h WHERE h.patient.id = :patientId AND h.account.id = :tenantId ORDER BY h.timestamp DESC")
    List<HealthLog> findLastNLogs(
            @Param("patientId") UUID patientId, 
            @Param("tenantId") UUID tenantId, 
            Pageable pageable
    );

    /**
     * Contar logs de um tenant
     */
    @Query("SELECT COUNT(h) FROM HealthLog h WHERE h.account.id = :tenantId")
    Long countByTenant(@Param("tenantId") UUID tenantId);

    /**
     * Contar logs de um paciente
     */
    @Query("SELECT COUNT(h) FROM HealthLog h WHERE h.patient.id = :patientId AND h.account.id = :tenantId")
    Long countByPatientAndTenant(
            @Param("patientId") UUID patientId, 
            @Param("tenantId") UUID tenantId
    );

    /**
     * Buscar logs por período
     */
    @Query("SELECT h FROM HealthLog h WHERE h.patient.id = :patientId AND h.account.id = :tenantId AND h.timestamp BETWEEN :start AND :end ORDER BY h.timestamp DESC")
    List<HealthLog> findByPatientAndPeriod(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Buscar logs com nível de dor acima de um threshold
     */
    @Query("SELECT h FROM HealthLog h WHERE h.patient.id = :patientId AND h.account.id = :tenantId AND h.painLevel >= :threshold ORDER BY h.timestamp DESC")
    List<HealthLog> findHighPainLogs(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId,
            @Param("threshold") Integer threshold
    );

    /**
     * Buscar logs onde medicação não foi tomada
     */
    @Query("SELECT h FROM HealthLog h WHERE h.patient.id = :patientId AND h.account.id = :tenantId AND h.medicationTaken = false ORDER BY h.timestamp DESC")
    List<HealthLog> findMissedMedicationLogs(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId
    );

    /**
     * Buscar último log de um paciente
     */
    @Query("SELECT h FROM HealthLog h WHERE h.patient.id = :patientId AND h.account.id = :tenantId ORDER BY h.timestamp DESC")
    List<HealthLog> findLastLogList(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId,
            Pageable pageable
    );

    default HealthLog findLastLog(UUID patientId, UUID tenantId) {
        List<HealthLog> logs = findLastLogList(patientId, tenantId, Pageable.ofSize(1));
        return logs.isEmpty() ? null : logs.get(0);
    }

    /**
     * Calcular média de dor em um período
     */
    @Query("SELECT AVG(h.painLevel) FROM HealthLog h WHERE h.patient.id = :patientId AND h.account.id = :tenantId AND h.painLevel IS NOT NULL AND h.timestamp BETWEEN :start AND :end")
    Double calculateAveragePain(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Buscar todos os logs de um tenant (para dashboard do psicólogo)
     */
    @Query("SELECT h FROM HealthLog h WHERE h.account.id = :tenantId ORDER BY h.timestamp DESC")
    List<HealthLog> findAllByTenant(@Param("tenantId") UUID tenantId);

    /**
     * Buscar logs de hoje de um paciente
     */
    @Query("SELECT h FROM HealthLog h WHERE h.patient.id = :patientId AND h.account.id = :tenantId AND CAST(h.timestamp AS date) = CURRENT_DATE ORDER BY h.timestamp DESC")
    List<HealthLog> findTodayLogs(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId
    );
}

