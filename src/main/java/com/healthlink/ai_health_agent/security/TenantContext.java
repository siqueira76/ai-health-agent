package com.healthlink.ai_health_agent.security;

import com.healthlink.ai_health_agent.domain.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Contexto de Tenant estabelecido após identificação inicial
 * Usado para armazenar informações do tenant durante a sessão/request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantContext {

    /**
     * ID do tenant (account_id)
     */
    private UUID tenantId;

    /**
     * Tipo de conta (B2B ou B2C)
     */
    private AccountType accountType;

    /**
     * Prompt customizado do tenant
     */
    private String customPrompt;

    /**
     * Nome do paciente (para personalização de mensagens)
     */
    private String patientName;

    /**
     * Número do WhatsApp (para referência)
     */
    private String whatsappNumber;

    /**
     * ID do paciente
     */
    private UUID patientId;

    /**
     * Construtor simplificado para identificação inicial
     */
    public TenantContext(UUID tenantId, String patientName) {
        this.tenantId = tenantId;
        this.patientName = patientName;
    }

    /**
     * Construtor para contexto completo (usado em ProactiveCheckinService)
     */
    public TenantContext(UUID tenantId, AccountType accountType, String customPrompt) {
        this.tenantId = tenantId;
        this.accountType = accountType;
        this.customPrompt = customPrompt;
    }

    /**
     * Construtor para contexto completo com paciente (usado em WhatsappWebhookController)
     */
    public TenantContext(UUID tenantId, String patientName, String whatsappNumber, UUID patientId) {
        this.tenantId = tenantId;
        this.patientName = patientName;
        this.whatsappNumber = whatsappNumber;
        this.patientId = patientId;
    }

    /**
     * Verifica se o contexto está válido
     */
    public boolean isValid() {
        return tenantId != null;
    }
}

