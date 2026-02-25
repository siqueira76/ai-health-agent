package com.healthlink.ai_health_agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para enviar mensagens via Evolution API
 * 
 * Endpoint: POST /message/sendText/{instance}
 * 
 * Body:
 * {
 *   "number": "5511999999999",
 *   "text": "Olá! Como posso ajudar?",
 *   "delay": 1000
 * }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvolutionApiSendMessageDTO {
    
    /**
     * Número do destinatário (sem @s.whatsapp.net)
     * Exemplo: "5511999999999"
     */
    private String number;
    
    /**
     * Texto da mensagem a ser enviada
     */
    private String text;
    
    /**
     * Delay em milissegundos antes de enviar (opcional)
     * Útil para simular digitação humana
     * Padrão: 1000ms (1 segundo)
     */
    private Integer delay;
    
    /**
     * Construtor sem delay (usa padrão de 1 segundo)
     */
    public EvolutionApiSendMessageDTO(String number, String text) {
        this.number = number;
        this.text = text;
        this.delay = 1000;
    }
}

