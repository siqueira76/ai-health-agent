package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.domain.entity.Account;
import com.healthlink.ai_health_agent.domain.entity.ChatMessage;
import com.healthlink.ai_health_agent.domain.entity.Patient;
import com.healthlink.ai_health_agent.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service para gerenciar histórico de conversas
 * Armazena mensagens e fornece contexto para a IA
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * Número de mensagens a manter no contexto (últimas N mensagens)
     */
    private static final int CONTEXT_WINDOW_SIZE = 10;

    /**
     * Salva mensagem do usuário
     * 
     * @param account Account (tenant)
     * @param patient Paciente
     * @param content Conteúdo da mensagem
     * @param whatsappMessageId ID da mensagem do WhatsApp (para idempotência)
     * @return ChatMessage salva
     */
    @Transactional
    public ChatMessage saveUserMessage(Account account, Patient patient, String content, String whatsappMessageId) {
        log.debug("💾 Salvando mensagem do usuário - Patient: {}, Length: {}", patient.getId(), content.length());

        // Verificar se já existe (idempotência)
        if (whatsappMessageId != null) {
            var existing = chatMessageRepository.findByWhatsappMessageId(whatsappMessageId, account.getId());
            if (existing.isPresent()) {
                log.warn("⚠️ Mensagem duplicada detectada: {}", whatsappMessageId);
                return existing.get();
            }
        }

        ChatMessage message = ChatMessage.userMessage(account, patient, content, whatsappMessageId);
        ChatMessage saved = chatMessageRepository.save(message);

        log.info("✅ Mensagem do usuário salva - ID: {}", saved.getId());
        return saved;
    }

    /**
     * Salva mensagem do assistente (IA)
     * 
     * @param account Account (tenant)
     * @param patient Paciente
     * @param content Conteúdo da resposta da IA
     * @return ChatMessage salva
     */
    @Transactional
    public ChatMessage saveAssistantMessage(Account account, Patient patient, String content) {
        log.debug("💾 Salvando mensagem do assistente - Patient: {}, Length: {}", patient.getId(), content.length());

        ChatMessage message = ChatMessage.assistantMessage(account, patient, content);
        ChatMessage saved = chatMessageRepository.save(message);

        log.info("✅ Mensagem do assistente salva - ID: {}", saved.getId());
        return saved;
    }

    /**
     * Busca últimas N mensagens para contexto da IA
     * Retorna em ordem cronológica (mais antiga primeiro)
     * 
     * @param tenantId ID do tenant
     * @param patientId ID do paciente
     * @param limit Número de mensagens (padrão: CONTEXT_WINDOW_SIZE)
     * @return Lista de mensagens em ordem cronológica
     */
    public List<ChatMessage> getRecentMessages(UUID tenantId, UUID patientId, int limit) {
        log.debug("📖 Buscando últimas {} mensagens - Tenant: {}, Patient: {}", limit, tenantId, patientId);

        List<ChatMessage> messages = chatMessageRepository.findLastNMessages(
                patientId,
                tenantId,
                PageRequest.of(0, limit)
        );

        // Inverter para ordem cronológica (mais antiga primeiro)
        Collections.reverse(messages);

        log.info("📖 {} mensagens recuperadas para contexto", messages.size());
        return messages;
    }

    /**
     * Busca últimas mensagens com tamanho padrão
     */
    public List<ChatMessage> getRecentMessages(UUID tenantId, UUID patientId) {
        return getRecentMessages(tenantId, patientId, CONTEXT_WINDOW_SIZE);
    }

    /**
     * Converte ChatMessages para formato Spring AI Message
     * Usado para construir o contexto da conversa
     * 
     * @param chatMessages Lista de ChatMessage
     * @return Lista de Message (Spring AI)
     */
    public List<Message> toSpringAiMessages(List<ChatMessage> chatMessages) {
        List<Message> messages = new ArrayList<>();

        for (ChatMessage chatMessage : chatMessages) {
            if (chatMessage.isUserMessage()) {
                messages.add(new UserMessage(chatMessage.getContent()));
            } else if (chatMessage.isAssistantMessage()) {
                messages.add(new AssistantMessage(chatMessage.getContent()));
            }
        }

        log.debug("🔄 Convertidas {} ChatMessages para Spring AI Messages", messages.size());
        return messages;
    }

    /**
     * Busca mensagens de hoje
     */
    public List<ChatMessage> getTodayMessages(UUID tenantId, UUID patientId) {
        return chatMessageRepository.findTodayMessages(patientId, tenantId);
    }

    /**
     * Conta total de mensagens de um paciente
     */
    public Long countMessages(UUID tenantId, UUID patientId) {
        return chatMessageRepository.countByPatient(patientId, tenantId);
    }

    /**
     * Exporta todas as mensagens de um paciente
     * Útil para relatórios e análises
     */
    public List<ChatMessage> exportAllMessages(UUID tenantId, UUID patientId) {
        log.info("📤 Exportando todas as mensagens - Tenant: {}, Patient: {}", tenantId, patientId);
        return chatMessageRepository.findAllByPatient(patientId, tenantId);
    }

    /**
     * Carrega mensagens recentes e converte para formato Spring AI
     * Usado em ProactiveCheckinService
     *
     * @param tenantId ID do tenant
     * @param patientId ID do paciente
     * @param limit Número de mensagens
     * @return Lista de Message (Spring AI) em ordem cronológica
     */
    public List<Message> loadRecentMessages(UUID tenantId, UUID patientId, int limit) {
        log.debug("📖 Carregando últimas {} mensagens para Spring AI - Tenant: {}, Patient: {}",
                  limit, tenantId, patientId);

        List<ChatMessage> chatMessages = getRecentMessages(tenantId, patientId, limit);
        List<Message> messages = toSpringAiMessages(chatMessages);

        log.info("📖 {} mensagens Spring AI carregadas", messages.size());
        return messages;
    }
}

