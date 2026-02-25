package com.healthlink.ai_health_agent.repository;

import com.healthlink.ai_health_agent.domain.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para Alerts com queries multi-tenant
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    /**
     * Busca alertas ativos (não reconhecidos) de um paciente
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.patient.id = :patientId
        AND a.account.id = :tenantId
        AND a.acknowledged = false
        ORDER BY a.createdAt DESC
        """)
    List<Alert> findActiveAlertsByPatient(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId
    );

    /**
     * Busca todos os alertas ativos de um tenant
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.account.id = :tenantId
        AND a.acknowledged = false
        ORDER BY a.severity DESC, a.createdAt DESC
        """)
    List<Alert> findActiveAlertsByTenant(@Param("tenantId") UUID tenantId);

    /**
     * Busca alertas por severidade
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.account.id = :tenantId
        AND a.severity = :severity
        AND a.acknowledged = false
        ORDER BY a.createdAt DESC
        """)
    List<Alert> findBySeverity(
            @Param("tenantId") UUID tenantId,
            @Param("severity") Alert.Severity severity
    );

    /**
     * Busca alertas críticos não reconhecidos
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.account.id = :tenantId
        AND a.severity = 'CRITICAL'
        AND a.acknowledged = false
        ORDER BY a.createdAt DESC
        """)
    List<Alert> findCriticalAlerts(@Param("tenantId") UUID tenantId);

    /**
     * Conta alertas ativos por paciente
     */
    @Query("""
        SELECT COUNT(a) FROM Alert a
        WHERE a.patient.id = :patientId
        AND a.account.id = :tenantId
        AND a.acknowledged = false
        """)
    Long countActiveAlertsByPatient(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId
    );

    /**
     * Busca alertas de um período
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.account.id = :tenantId
        AND a.createdAt BETWEEN :startDate AND :endDate
        ORDER BY a.createdAt DESC
        """)
    List<Alert> findByPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Busca alertas por tipo
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.patient.id = :patientId
        AND a.account.id = :tenantId
        AND a.alertType = :alertType
        AND a.acknowledged = false
        ORDER BY a.createdAt DESC
        """)
    List<Alert> findByTypeAndPatient(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId,
            @Param("alertType") Alert.AlertType alertType
    );

    /**
     * Verifica se já existe alerta similar recente (últimas 24h)
     * Evita duplicação de alertas
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM Alert a
        WHERE a.patient.id = :patientId
        AND a.account.id = :tenantId
        AND a.alertType = :alertType
        AND a.createdAt > :since
        """)
    boolean existsRecentAlert(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId,
            @Param("alertType") Alert.AlertType alertType,
            @Param("since") LocalDateTime since
    );

    /**
     * Busca histórico completo de alertas de um paciente
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.patient.id = :patientId
        AND a.account.id = :tenantId
        ORDER BY a.createdAt DESC
        """)
    List<Alert> findAllByPatient(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId
    );

    /**
     * Estatísticas de alertas por tipo
     */
    @Query("""
        SELECT a.alertType, COUNT(a)
        FROM Alert a
        WHERE a.account.id = :tenantId
        AND a.createdAt > :since
        GROUP BY a.alertType
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> getAlertStatsByType(
            @Param("tenantId") UUID tenantId,
            @Param("since") LocalDateTime since
    );
}

