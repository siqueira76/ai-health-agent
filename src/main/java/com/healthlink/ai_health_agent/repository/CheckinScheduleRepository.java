package com.healthlink.ai_health_agent.repository;

import com.healthlink.ai_health_agent.domain.entity.CheckinSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para gerenciar agendamentos de check-ins proativos.
 * Todas as queries respeitam isolamento multi-tenant.
 */
@Repository
public interface CheckinScheduleRepository extends JpaRepository<CheckinSchedule, UUID> {

    /**
     * Busca agendamentos prontos para execução
     * COM ISOLAMENTO MULTI-TENANT
     *
     * Critérios:
     * - is_active = true
     * - next_execution_at <= now
     * - account ativo (TRIAL ou ACTIVE)
     * - patient ativo
     */
    @Query("""
        SELECT cs FROM CheckinSchedule cs
        JOIN FETCH cs.account a
        JOIN FETCH cs.patient p
        WHERE cs.isActive = true
        AND cs.nextExecutionAt <= :now
        AND (a.status = 'ACTIVE' OR a.status = 'TRIAL')
        AND p.isActive = true
        ORDER BY cs.nextExecutionAt ASC
        """)
    List<CheckinSchedule> findSchedulesReadyForExecution(@Param("now") LocalDateTime now);

    /**
     * Busca agendamentos de um tenant específico
     */
    @Query("""
        SELECT cs FROM CheckinSchedule cs
        JOIN FETCH cs.account a
        JOIN FETCH cs.patient p
        WHERE cs.account.id = :tenantId
        ORDER BY cs.nextExecutionAt ASC
        """)
    List<CheckinSchedule> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Busca agendamentos de um paciente específico
     * COM ISOLAMENTO MULTI-TENANT
     */
    @Query("""
        SELECT cs FROM CheckinSchedule cs 
        WHERE cs.patient.id = :patientId 
        AND cs.account.id = :tenantId
        ORDER BY cs.timeOfDay ASC
        """)
    List<CheckinSchedule> findByPatientAndTenant(
        @Param("patientId") UUID patientId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Busca um agendamento específico com validação de tenant
     */
    @Query("""
        SELECT cs FROM CheckinSchedule cs
        WHERE cs.id = :scheduleId
        AND cs.account.id = :tenantId
        """)
    Optional<CheckinSchedule> findByIdAndTenant(
        @Param("scheduleId") UUID scheduleId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Conta mensagens enviadas hoje por um tenant
     * Usado para rate limiting no nível de tenant
     */
    @Query("""
        SELECT COUNT(ce) FROM CheckinExecution ce
        WHERE ce.account.id = :tenantId
        AND CAST(ce.executedAt AS date) = :date
        AND ce.status = 'SUCCESS'
        """)
    long countMessagesSentTodayByTenant(
        @Param("tenantId") UUID tenantId,
        @Param("date") LocalDate date
    );

    /**
     * Busca agendamentos ativos de um tenant
     */
    @Query("""
        SELECT cs FROM CheckinSchedule cs
        WHERE cs.account.id = :tenantId
        AND cs.isActive = true
        ORDER BY cs.nextExecutionAt ASC
        """)
    List<CheckinSchedule> findActiveByTenant(@Param("tenantId") UUID tenantId);

    /**
     * Conta agendamentos ativos de um paciente
     */
    @Query("""
        SELECT COUNT(cs) FROM CheckinSchedule cs
        WHERE cs.patient.id = :patientId
        AND cs.account.id = :tenantId
        AND cs.isActive = true
        """)
    long countActiveByPatientAndTenant(
        @Param("patientId") UUID patientId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Busca agendamentos que precisam de reset diário
     * (para resetar contador de mensagens)
     */
    @Query("""
        SELECT cs FROM CheckinSchedule cs
        WHERE cs.isActive = true
        AND cs.lastResetDate < :today
        """)
    List<CheckinSchedule> findSchedulesNeedingReset(@Param("today") LocalDate today);
}

