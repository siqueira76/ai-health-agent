package com.healthlink.ai_health_agent.repository;

import com.healthlink.ai_health_agent.domain.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para Patient com suporte a Multi-Tenancy
 * IMPORTANTE: Todos os métodos devem filtrar por tenantId (account_id)
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    /**
     * Busca paciente por número do WhatsApp e tenantId
     * MÉTODO PRINCIPAL para garantir isolamento multi-tenant
     */
    @Query("SELECT p FROM Patient p WHERE p.whatsappNumber = :whatsappNumber AND p.account.id = :tenantId")
    Optional<Patient> findByWhatsappNumberAndTenantId(
        @Param("whatsappNumber") String whatsappNumber,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Busca paciente apenas por WhatsApp (sem filtro de tenant)
     * USO LEGÍTIMO: Identificação inicial do tenant na primeira interação
     * Este é o ponto de entrada do sistema quando recebe uma mensagem do WhatsApp
     */
    Optional<Patient> findByWhatsappNumber(String whatsappNumber);

    /**
     * Projeção leve para identificação rápida do tenant
     * Retorna apenas os dados essenciais para estabelecer o contexto de segurança
     * Evita carregar toda a entidade Patient na identificação inicial
     */
    @Query("SELECT p.id as id, p.whatsappNumber as whatsappNumber, p.account.id as tenantId, p.name as name FROM Patient p WHERE p.whatsappNumber = :whatsappNumber")
    Optional<PatientTenantProjection> findTenantContextByWhatsappNumber(@Param("whatsappNumber") String whatsappNumber);

    /**
     * Interface de projeção para identificação do tenant
     * Usado no fluxo inicial de autenticação
     */
    interface PatientTenantProjection {
        UUID getId();
        String getWhatsappNumber();
        UUID getTenantId();
        String getName();
    }

    /**
     * Lista todos os pacientes de um tenant específico
     */
    @Query("SELECT p FROM Patient p WHERE p.account.id = :tenantId ORDER BY p.name ASC")
    List<Patient> findAllByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Lista pacientes ativos de um tenant
     */
    @Query("SELECT p FROM Patient p WHERE p.account.id = :tenantId AND p.isActive = true ORDER BY p.name ASC")
    List<Patient> findActivePatientsByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Lista pacientes inativos de um tenant
     */
    @Query("SELECT p FROM Patient p WHERE p.account.id = :tenantId AND p.isActive = false ORDER BY p.name ASC")
    List<Patient> findInactivePatientsByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Conta total de pacientes de um tenant
     */
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.account.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Conta pacientes ativos de um tenant
     */
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.account.id = :tenantId AND p.isActive = true")
    Long countActivePatientsByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Busca pacientes por diagnóstico dentro de um tenant
     */
    @Query("SELECT p FROM Patient p WHERE p.account.id = :tenantId AND LOWER(p.diagnosis) LIKE LOWER(CONCAT('%', :diagnosis, '%'))")
    List<Patient> findByDiagnosisAndTenantId(
        @Param("diagnosis") String diagnosis,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Busca pacientes que não interagiram desde uma data específica
     * Útil para identificar pacientes inativos
     */
    @Query("SELECT p FROM Patient p WHERE p.account.id = :tenantId AND p.lastInteractionAt < :since")
    List<Patient> findInactiveSince(
        @Param("tenantId") UUID tenantId,
        @Param("since") LocalDateTime since
    );

    /**
     * Busca pacientes por nome (busca parcial) dentro de um tenant
     */
    @Query("SELECT p FROM Patient p WHERE p.account.id = :tenantId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Patient> searchByNameAndTenantId(
        @Param("name") String name,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Verifica se existe um paciente com o WhatsApp informado no tenant
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Patient p WHERE p.whatsappNumber = :whatsappNumber AND p.account.id = :tenantId")
    boolean existsByWhatsappNumberAndTenantId(
        @Param("whatsappNumber") String whatsappNumber,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Busca pacientes criados em um período específico
     */
    @Query("SELECT p FROM Patient p WHERE p.account.id = :tenantId AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Patient> findByCreatedAtBetweenAndTenantId(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Busca os N pacientes mais recentes de um tenant
     */
    @Query("SELECT p FROM Patient p WHERE p.account.id = :tenantId ORDER BY p.createdAt DESC LIMIT :limit")
    List<Patient> findRecentPatientsByTenantId(
        @Param("tenantId") UUID tenantId,
        @Param("limit") int limit
    );

    /**
     * Busca pacientes com última interação mais recente
     */
    @Query("SELECT p FROM Patient p WHERE p.account.id = :tenantId AND p.lastInteractionAt IS NOT NULL ORDER BY p.lastInteractionAt DESC LIMIT :limit")
    List<Patient> findMostRecentInteractionsByTenantId(
        @Param("tenantId") UUID tenantId,
        @Param("limit") int limit
    );

    /**
     * Busca todos os pacientes de um account (alias para findAllByTenantId)
     * Usado em alguns services que preferem o nome "findByAccountId"
     */
    @Query("SELECT p FROM Patient p WHERE p.account.id = :accountId ORDER BY p.name ASC")
    List<Patient> findByAccountId(@Param("accountId") UUID accountId);
}

