package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.domain.entity.Account;
import com.healthlink.ai_health_agent.domain.enums.AccountType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsável por gerenciar os prompts do sistema
 * Injeta o prompt customizado do tenant no System Message
 */
@Service
@Slf4j
public class PromptService {

    /**
     * Prompt padrão para contas B2C (Fibromialgia)
     */
    private static final String DEFAULT_B2C_PROMPT = """
            Você é um assistente terapêutico especializado em fibromialgia e dor crônica.

            Seu papel é:
            - Acompanhar diariamente o paciente com empatia e acolhimento
            - Fazer perguntas sobre níveis de dor, humor e qualidade do sono
            - Lembrar sobre medicações e exercícios prescritos
            - Oferecer suporte emocional e técnicas de relaxamento
            - Identificar padrões e gatilhos de crises

            IMPORTANTE - REGISTRO DE DADOS:
            - SEMPRE que o paciente mencionar dor, humor, sono ou medicação, use a função recordDailyHealthStats
            - Exemplos:
              * "Estou com dor 8" → recordDailyHealthStats(painLevel=8)
              * "Não dormi bem" → recordDailyHealthStats(sleepQuality="ruim")
              * "Esqueci de tomar o remédio" → recordDailyHealthStats(medicationTaken=false)
              * "Estou ansioso" → recordDailyHealthStats(mood="ansioso")
            - Chame a função ANTES de responder
            - Confirme de forma natural: "Entendi, registrei que..."

            Tom de voz: Empático, acolhedor, profissional mas próximo.
            Linguagem: Simples e acessível, evitando termos técnicos complexos.

            IMPORTANTE:
            - Nunca substitua orientação médica
            - Em casos de emergência, oriente a procurar atendimento imediato
            - Mantenha o histórico do paciente em mente para personalizar o acompanhamento
            """;

    /**
     * Prompt padrão para contas B2B (Psicólogos)
     */
    private static final String DEFAULT_B2B_PROMPT = """
            Você é um assistente terapêutico configurável para apoiar o trabalho de psicólogos.

            Seu papel é:
            - Fazer check-ins regulares com os pacientes
            - Coletar informações sobre humor, ansiedade e bem-estar
            - Aplicar escalas e questionários quando solicitado
            - Manter registro estruturado das interações
            - Identificar sinais de alerta para o profissional

            IMPORTANTE - REGISTRO DE DADOS:
            - SEMPRE use a função recordDailyHealthStats para salvar dados mencionados pelo paciente
            - Registre: humor, níveis de ansiedade/estresse, qualidade do sono, energia
            - Chame a função ANTES de responder ao paciente
            
            Tom de voz: Profissional, empático e neutro.
            Linguagem: Clara e respeitosa.
            
            IMPORTANTE:
            - Você apoia, mas não substitui o psicólogo
            - Siga as diretrizes personalizadas do profissional responsável
            - Em situações de risco, alerte imediatamente o profissional
            """;

    /**
     * Constrói o System Message dinâmico baseado no tenant
     * 
     * @param account Conta do tenant (B2C ou B2B)
     * @return System Message personalizado
     */
    public String buildSystemMessage(Account account) {
        log.debug("Construindo System Message para Account ID: {} (Tipo: {})", 
                  account.getId(), account.getType());

        // Se o tenant tem prompt customizado, usa ele
        if (account.getCustomPrompt() != null && !account.getCustomPrompt().isBlank()) {
            log.info("Usando prompt customizado para Account ID: {}", account.getId());
            return account.getCustomPrompt();
        }

        // Caso contrário, usa o prompt padrão baseado no tipo de conta
        String defaultPrompt = account.getType() == AccountType.B2C 
                ? DEFAULT_B2C_PROMPT 
                : DEFAULT_B2B_PROMPT;

        log.debug("Usando prompt padrão {} para Account ID: {}", 
                  account.getType(), account.getId());

        return defaultPrompt;
    }

    /**
     * Constrói o System Message com contexto adicional do paciente
     * 
     * @param account Conta do tenant
     * @param patientName Nome do paciente
     * @param diagnosis Diagnóstico do paciente (opcional)
     * @return System Message personalizado com contexto
     */
    public String buildSystemMessageWithContext(Account account, String patientName, String diagnosis) {
        String basePrompt = buildSystemMessage(account);

        // Adiciona contexto do paciente ao prompt
        StringBuilder contextualPrompt = new StringBuilder(basePrompt);
        contextualPrompt.append("\n\n--- CONTEXTO DO PACIENTE ---\n");
        contextualPrompt.append("Nome: ").append(patientName).append("\n");

        if (diagnosis != null && !diagnosis.isBlank()) {
            contextualPrompt.append("Diagnóstico: ").append(diagnosis).append("\n");
        }

        contextualPrompt.append("\nPersonalize suas respostas considerando este contexto.");

        return contextualPrompt.toString();
    }

    /**
     * Retorna o prompt padrão B2C (útil para preview/documentação)
     */
    public String getDefaultB2CPrompt() {
        return DEFAULT_B2C_PROMPT;
    }

    /**
     * Retorna o prompt padrão B2B (útil para preview/documentação)
     */
    public String getDefaultB2BPrompt() {
        return DEFAULT_B2B_PROMPT;
    }

    /**
     * Valida se um prompt customizado está dentro dos limites aceitáveis
     * 
     * @param customPrompt Prompt a ser validado
     * @return true se válido, false caso contrário
     */
    public boolean validateCustomPrompt(String customPrompt) {
        if (customPrompt == null || customPrompt.isBlank()) {
            return false;
        }

        // Limite de 5000 caracteres para o prompt
        if (customPrompt.length() > 5000) {
            log.warn("Prompt customizado excede 5000 caracteres: {}", customPrompt.length());
            return false;
        }

        // Verifica se contém palavras-chave essenciais de segurança
        String lowerPrompt = customPrompt.toLowerCase();
        boolean hasSafetyGuidelines = lowerPrompt.contains("nunca substitua") 
                || lowerPrompt.contains("não substitui")
                || lowerPrompt.contains("orientação médica")
                || lowerPrompt.contains("orientação profissional");

        if (!hasSafetyGuidelines) {
            log.warn("Prompt customizado não contém diretrizes de segurança adequadas");
            return false;
        }

        return true;
    }
}

