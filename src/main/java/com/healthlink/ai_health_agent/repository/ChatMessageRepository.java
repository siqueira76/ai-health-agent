package com.healthlink.ai_health_agent.repository;

import com.healthlink.ai_health_agent.domain.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para ChatMessage com queries multi-tenant
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Busca últimas N mensagens de um paciente (ordenadas por timestamp DESC)
     * Usado para construir contexto da IA
     * 
     * @param patientId ID do paciente
     * @param tenantId ID do tenant (segurança)
     * @param pageable Paginação (ex: PageRequest.of(0, 10))
     * @return Lista de mensagens mais recentes
     */
    @Query("""
            SELECT cm FROM ChatMessage cm
            WHERE cm.patient.id = :patientId
            AND cm.account.id = :tenantId
            ORDER BY cm.timestamp DESC
            """)
    List<ChatMessage> findLastNMessages(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId,
            Pageable pageable
    );

    /**
     * Busca mensagens de um período específico
     * 
     * @param patientId ID do paciente
     * @param tenantId ID do tenant
     * @param start Data/hora inicial
     * @param end Data/hora final
     * @return Lista de mensagens no período
     */
    @Query("""
            SELECT cm FROM ChatMessage cm
            WHERE cm.patient.id = :patientId
            AND cm.account.id = :tenantId
            AND cm.timestamp BETWEEN :start AND :end
            ORDER BY cm.timestamp ASC
            """)
    List<ChatMessage> findByPatientAndPeriod(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Busca mensagens de hoje
     *
     * @param patientId ID do paciente
     * @param tenantId ID do tenant
     * @return Lista de mensagens de hoje
     */
    @Query("""
            SELECT cm FROM ChatMessage cm
            WHERE cm.patient.id = :patientId
            AND cm.account.id = :tenantId
            AND CAST(cm.timestamp AS date) = CURRENT_DATE
            ORDER BY cm.timestamp ASC
            """)
    List<ChatMessage> findTodayMessages(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId
    );

    /**
     * Conta total de mensagens de um paciente
     * 
     * @param patientId ID do paciente
     * @param tenantId ID do tenant
     * @return Total de mensagens
     */
    @Query("""
            SELECT COUNT(cm) FROM ChatMessage cm
            WHERE cm.patient.id = :patientId
            AND cm.account.id = :tenantId
            """)
    Long countByPatient(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId
    );

    /**
     * Busca mensagem pelo ID do WhatsApp (para evitar duplicatas)
     * 
     * @param whatsappMessageId ID da mensagem do WhatsApp
     * @param tenantId ID do tenant
     * @return Optional com a mensagem se existir
     */
    @Query("""
            SELECT cm FROM ChatMessage cm
            WHERE cm.whatsappMessageId = :whatsappMessageId
            AND cm.account.id = :tenantId
            """)
    Optional<ChatMessage> findByWhatsappMessageId(
            @Param("whatsappMessageId") String whatsappMessageId,
            @Param("tenantId") UUID tenantId
    );

    /**
     * Deleta mensagens antigas (limpeza de dados)
     * 
     * @param before Data limite
     * @param tenantId ID do tenant
     * @return Número de mensagens deletadas
     */
    @Query("""
            DELETE FROM ChatMessage cm
            WHERE cm.timestamp < :before
            AND cm.account.id = :tenantId
            """)
    int deleteOldMessages(
            @Param("before") LocalDateTime before,
            @Param("tenantId") UUID tenantId
    );

    /**
     * Busca todas as mensagens de um paciente (para exportação)
     * 
     * @param patientId ID do paciente
     * @param tenantId ID do tenant
     * @return Lista completa de mensagens
     */
    @Query("""
            SELECT cm FROM ChatMessage cm
            WHERE cm.patient.id = :patientId
            AND cm.account.id = :tenantId
            ORDER BY cm.timestamp ASC
            """)
    List<ChatMessage> findAllByPatient(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId
    );
}

