package com.healthlink.ai_health_agent.repository;

import com.healthlink.ai_health_agent.domain.entity.CheckinExecution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para gerenciar execuções de check-ins proativos.
 * Armazena histórico de todas as tentativas de envio.
 */
@Repository
public interface CheckinExecutionRepository extends JpaRepository<CheckinExecution, UUID> {

    /**
     * Busca execuções de um schedule específico
     */
    @Query("""
        SELECT ce FROM CheckinExecution ce
        WHERE ce.schedule.id = :scheduleId
        ORDER BY ce.executedAt DESC
        """)
    List<CheckinExecution> findByScheduleId(@Param("scheduleId") UUID scheduleId);

    /**
     * Busca execuções de um paciente
     * COM ISOLAMENTO MULTI-TENANT
     */
    @Query("""
        SELECT ce FROM CheckinExecution ce
        WHERE ce.patient.id = :patientId
        AND ce.account.id = :tenantId
        ORDER BY ce.executedAt DESC
        """)
    List<CheckinExecution> findByPatientAndTenant(
        @Param("patientId") UUID patientId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Busca execuções de um tenant
     */
    @Query("""
        SELECT ce FROM CheckinExecution ce
        WHERE ce.account.id = :tenantId
        ORDER BY ce.executedAt DESC
        """)
    List<CheckinExecution> findByTenant(@Param("tenantId") UUID tenantId);

    /**
     * Busca execuções com falha desde uma data
     */
    @Query("""
        SELECT ce FROM CheckinExecution ce
        WHERE ce.status = 'FAILED'
        AND ce.executedAt >= :since
        ORDER BY ce.executedAt DESC
        """)
    List<CheckinExecution> findFailedExecutionsSince(@Param("since") LocalDateTime since);

    /**
     * Busca execuções bem-sucedidas de um paciente
     */
    @Query("""
        SELECT ce FROM CheckinExecution ce
        WHERE ce.patient.id = :patientId
        AND ce.account.id = :tenantId
        AND ce.status = 'SUCCESS'
        ORDER BY ce.executedAt DESC
        """)
    List<CheckinExecution> findSuccessfulByPatientAndTenant(
        @Param("patientId") UUID patientId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Conta execuções por status em um período
     */
    @Query("""
        SELECT ce.status, COUNT(ce) FROM CheckinExecution ce
        WHERE ce.account.id = :tenantId
        AND ce.executedAt >= :since
        GROUP BY ce.status
        """)
    List<Object[]> countByStatusSince(
        @Param("tenantId") UUID tenantId,
        @Param("since") LocalDateTime since
    );

    /**
     * Busca últimas N execuções de um schedule
     */
    @Query("""
        SELECT ce FROM CheckinExecution ce
        WHERE ce.schedule.id = :scheduleId
        ORDER BY ce.executedAt DESC
        """)
    List<CheckinExecution> findLastNBySchedule(
        @Param("scheduleId") UUID scheduleId,
        Pageable pageable
    );

    /**
     * Verifica se paciente respondeu à última mensagem proativa
     */
    @Query("""
        SELECT ce FROM CheckinExecution ce
        WHERE ce.patient.id = :patientId
        AND ce.account.id = :tenantId
        AND ce.status = 'SUCCESS'
        ORDER BY ce.executedAt DESC
        """)
    List<CheckinExecution> findLastExecutionByPatientList(
        @Param("patientId") UUID patientId,
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );

    default CheckinExecution findLastExecutionByPatient(UUID patientId, UUID tenantId) {
        List<CheckinExecution> executions = findLastExecutionByPatientList(patientId, tenantId, Pageable.ofSize(1));
        return executions.isEmpty() ? null : executions.get(0);
    }
}

