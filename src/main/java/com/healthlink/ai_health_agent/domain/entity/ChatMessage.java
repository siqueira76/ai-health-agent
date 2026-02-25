package com.healthlink.ai_health_agent.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade para armazenar histórico de mensagens do chat
 * Permite que a IA tenha contexto das conversas anteriores
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_patient_timestamp", columnList = "patient_id, timestamp DESC"),
        @Index(name = "idx_chat_account_timestamp", columnList = "account_id, timestamp DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant (isolamento multi-tenant)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /**
     * Paciente dono da conversa
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Timestamp da mensagem
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * Papel do remetente: USER ou ASSISTANT
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    /**
     * Conteúdo da mensagem
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * ID da mensagem do WhatsApp (para idempotência)
     */
    @Column(name = "whatsapp_message_id", length = 100)
    private String whatsappMessageId;

    /**
     * Metadados adicionais (JSON)
     * Ex: tokens usados, tempo de resposta, etc
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Enum para papel da mensagem
     */
    public enum MessageRole {
        USER,       // Mensagem do paciente
        ASSISTANT   // Resposta da IA
    }

    /**
     * Helper para criar mensagem do usuário
     */
    public static ChatMessage userMessage(Account account, Patient patient, String content, String whatsappMessageId) {
        return ChatMessage.builder()
                .account(account)
                .patient(patient)
                .timestamp(LocalDateTime.now())
                .role(MessageRole.USER)
                .content(content)
                .whatsappMessageId(whatsappMessageId)
                .build();
    }

    /**
     * Helper para criar mensagem do assistente
     */
    public static ChatMessage assistantMessage(Account account, Patient patient, String content) {
        return ChatMessage.builder()
                .account(account)
                .patient(patient)
                .timestamp(LocalDateTime.now())
                .role(MessageRole.ASSISTANT)
                .content(content)
                .build();
    }

    /**
     * Retorna resumo da mensagem para log
     */
    public String getSummary() {
        String preview = content.length() > 50 
                ? content.substring(0, 50) + "..." 
                : content;
        return String.format("[%s] %s", role, preview);
    }

    /**
     * Verifica se a mensagem é do usuário
     */
    public boolean isUserMessage() {
        return role == MessageRole.USER;
    }

    /**
     * Verifica se a mensagem é do assistente
     */
    public boolean isAssistantMessage() {
        return role == MessageRole.ASSISTANT;
    }
}

