package com.healthlink.ai_health_agent.security;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe holder para TenantContext
 * 
 * Armazena o contexto do tenant na thread atual usando ThreadLocal
 * Garante isolamento multi-tenant durante o processamento de requests
 * 
 * IMPORTANTE:
 * - Sempre chamar setContext() no início do processamento
 * - Sempre chamar clear() no finally para evitar memory leaks
 * 
 * Exemplo de uso:
 * <pre>
 * try {
 *     TenantContext context = new TenantContext(tenantId, patientName, whatsappNumber, patientId);
 *     TenantContextHolder.setContext(context);
 *     
 *     // Processar request com contexto estabelecido
 *     service.processWithTenant();
 *     
 * } finally {
 *     TenantContextHolder.clear(); // SEMPRE limpar!
 * }
 * </pre>
 */
@Slf4j
public class TenantContextHolder {
    
    /**
     * ThreadLocal para armazenar o contexto do tenant
     * Cada thread tem sua própria cópia, garantindo isolamento
     */
    private static final ThreadLocal<TenantContext> contextHolder = new ThreadLocal<>();
    
    /**
     * Define o contexto do tenant para a thread atual
     * 
     * @param context Contexto do tenant a ser estabelecido
     * @throws IllegalArgumentException se o contexto for nulo ou inválido
     */
    public static void setContext(TenantContext context) {
        if (context == null) {
            throw new IllegalArgumentException("TenantContext não pode ser nulo");
        }
        
        if (!context.isValid()) {
            throw new IllegalArgumentException("TenantContext inválido: tenantId é obrigatório");
        }
        
        log.debug("🔐 Estabelecendo contexto de tenant: {}", context.getTenantId());
        contextHolder.set(context);
    }
    
    /**
     * Obtém o contexto do tenant da thread atual
     * 
     * @return TenantContext ou null se não houver contexto estabelecido
     */
    public static TenantContext getContext() {
        return contextHolder.get();
    }
    
    /**
     * Limpa o contexto do tenant da thread atual
     * 
     * IMPORTANTE: Sempre chamar este método em um bloco finally
     * para evitar memory leaks e vazamento de contexto entre requests
     */
    public static void clear() {
        TenantContext context = contextHolder.get();
        if (context != null) {
            log.debug("🧹 Limpando contexto de tenant: {}", context.getTenantId());
        }
        contextHolder.remove();
    }
    
    /**
     * Verifica se há um contexto válido estabelecido
     * 
     * @return true se há contexto válido, false caso contrário
     */
    public static boolean hasContext() {
        TenantContext context = contextHolder.get();
        return context != null && context.isValid();
    }
    
    /**
     * Obtém o tenantId do contexto atual
     * 
     * @return UUID do tenant ou null se não houver contexto
     */
    public static java.util.UUID getTenantId() {
        TenantContext context = getContext();
        return context != null ? context.getTenantId() : null;
    }
    
    /**
     * Obtém o patientId do contexto atual
     * 
     * @return UUID do paciente ou null se não houver contexto
     */
    public static java.util.UUID getPatientId() {
        TenantContext context = getContext();
        return context != null ? context.getPatientId() : null;
    }
    
    /**
     * Obtém o nome do paciente do contexto atual
     * 
     * @return Nome do paciente ou null se não houver contexto
     */
    public static String getPatientName() {
        TenantContext context = getContext();
        return context != null ? context.getPatientName() : null;
    }
    
    /**
     * Obtém o número do WhatsApp do contexto atual
     * 
     * @return Número do WhatsApp ou null se não houver contexto
     */
    public static String getWhatsappNumber() {
        TenantContext context = getContext();
        return context != null ? context.getWhatsappNumber() : null;
    }
}

