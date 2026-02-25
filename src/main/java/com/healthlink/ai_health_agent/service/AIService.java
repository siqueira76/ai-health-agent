package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.domain.entity.Account;
import com.healthlink.ai_health_agent.domain.entity.ChatMessage;
import com.healthlink.ai_health_agent.domain.entity.Patient;
import com.healthlink.ai_health_agent.repository.AccountRepository;
import com.healthlink.ai_health_agent.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service principal de IA com suporte a Multi-Tenancy e Chat History
 * Injeta o prompt customizado do tenant no System Message
 * Mantém contexto das últimas conversas
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIService {

    private final ChatModel chatModel;
    private final PromptService promptService;
    private final PatientRepository patientRepository;
    private final AccountRepository accountRepository;
    private final ChatHistoryService chatHistoryService;

    /**
     * Processa uma mensagem do paciente com contexto multi-tenant
     * 
     * @param whatsappNumber Número do WhatsApp do paciente
     * @param userMessage Mensagem enviada pelo paciente
     * @return Resposta da IA
     */
    public String processMessage(String whatsappNumber, String userMessage) {
        log.info("Processando mensagem do WhatsApp: {}", whatsappNumber);

        // 1. Identificar o paciente e o tenant
        Patient patient = patientRepository.findByWhatsappNumber(whatsappNumber)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado: " + whatsappNumber));

        UUID tenantId = patient.getTenantId();
        log.debug("Tenant identificado: {}", tenantId);

        // 2. Buscar a conta (tenant) para obter o prompt customizado
        Account account = accountRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Account não encontrada: " + tenantId));

        // 3. Construir o System Message dinâmico
        String systemPrompt = promptService.buildSystemMessageWithContext(
                account, 
                patient.getName(), 
                patient.getDiagnosis()
        );

        log.debug("System Prompt construído para tenant {}: {} caracteres", 
                  tenantId, systemPrompt.length());

        // 4. Criar o Prompt com System Message + User Message
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userMessage));

        // 5. Chamar a IA
        Prompt prompt = new Prompt(messages);
        ChatResponse response = chatModel.call(prompt);
        String aiResponse = response.getResult().getOutput().getText();
        log.info("Resposta da IA gerada para {}: {} caracteres", whatsappNumber, aiResponse.length());

        // 6. Atualizar última interação do paciente
        patient.updateLastInteraction();
        patientRepository.save(patient);

        return aiResponse;
    }

    /**
     * Processa mensagem com tenant já identificado (mais eficiente)
     * COM FUNCTION CALLING E CHAT HISTORY
     *
     * @param tenantId ID do tenant
     * @param patientId ID do paciente
     * @param userMessage Mensagem do usuário
     * @param whatsappMessageId ID da mensagem do WhatsApp (para idempotência)
     * @return Resposta da IA
     */
    public String processMessageWithTenant(UUID tenantId, UUID patientId, String userMessage, String whatsappMessageId) {
        log.info("🤖 Processando mensagem com tenant pré-identificado: {}", tenantId);

        // 1. Buscar paciente com validação de tenant
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // Validação de segurança: garantir que o paciente pertence ao tenant
        if (!patient.getTenantId().equals(tenantId)) {
            throw new SecurityException("Acesso negado: paciente não pertence ao tenant");
        }

        // 2. Buscar account
        Account account = accountRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Account não encontrada"));

        // 3. Salvar mensagem do usuário no histórico
        chatHistoryService.saveUserMessage(account, patient, userMessage, whatsappMessageId);

        // 4. Buscar histórico de conversas (últimas 10 mensagens)
        List<ChatMessage> recentMessages = chatHistoryService.getRecentMessages(tenantId, patientId);
        log.info("📖 Histórico carregado: {} mensagens", recentMessages.size());

        // 5. Construir System Message
        String systemPrompt = promptService.buildSystemMessageWithContext(
                account,
                patient.getName(),
                patient.getDiagnosis()
        );

        // 6. Construir lista de mensagens com contexto
        List<Message> messages = new ArrayList<>();

        // System message sempre primeiro
        messages.add(new SystemMessage(systemPrompt));

        // Adicionar histórico (se houver)
        if (!recentMessages.isEmpty()) {
            messages.addAll(chatHistoryService.toSpringAiMessages(recentMessages));
            log.debug("📝 Contexto: {} mensagens históricas adicionadas", recentMessages.size());
        }

        // Adicionar mensagem atual do usuário
        messages.add(new UserMessage(userMessage));

        log.debug("📊 Total de mensagens no contexto: {}", messages.size());
        log.debug("System Prompt: {} caracteres", systemPrompt.length());

        // 7. Chamar IA COM HISTÓRICO
        // TODO: Function calling será adicionado em versão futura do Spring AI
        Prompt prompt = new Prompt(messages);
        ChatResponse response = chatModel.call(prompt);
        String aiResponse = response.getResult().getOutput().getText();

        log.info("✅ Resposta da IA gerada: {} caracteres", aiResponse.length());

        // 8. Salvar resposta da IA no histórico
        chatHistoryService.saveAssistantMessage(account, patient, aiResponse);

        // 9. Atualizar última interação
        patient.updateLastInteraction();
        patientRepository.save(patient);

        return aiResponse;
    }

    /**
     * Sobrecarga para compatibilidade (sem whatsappMessageId)
     */
    public String processMessageWithTenant(UUID tenantId, UUID patientId, String userMessage) {
        return processMessageWithTenant(tenantId, patientId, userMessage, null);
    }

    /**
     * Preview do prompt que será usado para um tenant específico
     * Útil para psicólogos testarem suas customizações
     * 
     * @param tenantId ID do tenant
     * @return Prompt que será usado
     */
    public String previewSystemPrompt(UUID tenantId) {
        Account account = accountRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Account não encontrada"));

        return promptService.buildSystemMessage(account);
    }

    /**
     * Atualiza o prompt customizado de um tenant
     * 
     * @param tenantId ID do tenant
     * @param customPrompt Novo prompt customizado
     */
    public void updateCustomPrompt(UUID tenantId, String customPrompt) {
        log.info("Atualizando prompt customizado para tenant: {}", tenantId);

        // Validar o prompt
        if (!promptService.validateCustomPrompt(customPrompt)) {
            throw new IllegalArgumentException("Prompt customizado inválido ou inseguro");
        }

        Account account = accountRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Account não encontrada"));

        account.setCustomPrompt(customPrompt);
        accountRepository.save(account);

        log.info("Prompt customizado atualizado com sucesso para tenant: {}", tenantId);
    }

    /**
     * Gera mensagem proativa usando IA
     * Usado pelo sistema de check-ins automáticos
     *
     * @param tenantId ID do tenant
     * @param patientId ID do paciente
     * @param systemPrompt Prompt do sistema (custom_prompt + contexto proativo)
     * @param recentHistory Histórico recente de mensagens
     * @return Mensagem proativa gerada pela IA
     */
    public String generateProactiveMessage(
            UUID tenantId,
            UUID patientId,
            String systemPrompt,
            List<Message> recentHistory) {

        log.info("Gerando mensagem proativa - Tenant: {}, Patient: {}", tenantId, patientId);

        // Construir lista de mensagens
        List<Message> messages = new ArrayList<>();

        // 1. System Message com prompt customizado
        messages.add(new SystemMessage(systemPrompt));

        // 2. Histórico recente (se houver)
        if (recentHistory != null && !recentHistory.isEmpty()) {
            messages.addAll(recentHistory);
        }

        // 3. Mensagem do usuário solicitando check-in
        messages.add(new UserMessage(
            "Inicie uma conversa proativa com o paciente perguntando como ele está se sentindo hoje."
        ));

        // Chamar IA
        Prompt prompt = new Prompt(messages);
        ChatResponse response = chatModel.call(prompt);
        String aiMessage = response.getResult().getOutput().getText();

        log.info("Mensagem proativa gerada com sucesso");
        log.debug("Mensagem: {}", aiMessage);

        return aiMessage;
    }
}

