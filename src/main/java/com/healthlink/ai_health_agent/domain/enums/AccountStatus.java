package com.healthlink.ai_health_agent.domain.enums;

/**
 * Status da conta no sistema
 */
public enum AccountStatus {
    /**
     * Conta ativa e funcional
     */
    ACTIVE,
    
    /**
     * Conta suspensa (inadimplência ou violação de termos)
     */
    SUSPENDED,
    
    /**
     * Conta cancelada pelo usuário
     */
    CANCELLED,
    
    /**
     * Conta em período de trial/teste
     */
    TRIAL
}

