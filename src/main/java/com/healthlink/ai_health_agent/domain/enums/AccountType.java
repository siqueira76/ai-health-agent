package com.healthlink.ai_health_agent.domain.enums;

/**
 * Tipo de conta no sistema multi-tenant
 * B2C: Paciente direto (Fibromialgia)
 * B2B: Psicólogo que gerencia múltiplos pacientes
 */
public enum AccountType {
    /**
     * Conta B2C - Paciente individual (ex: Fibromialgia)
     */
    B2C,
    
    /**
     * Conta B2B - Psicólogo que gerencia pacientes
     */
    B2B
}

