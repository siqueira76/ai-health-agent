package com.healthlink.ai_health_agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * DTO para receber webhooks da Evolution API
 * 
 * Formato do JSON recebido:
 * {
 *   "event": "messages.upsert",
 *   "instance": "instance-name",
 *   "data": {
 *     "key": {
 *       "remoteJid": "5511999999999@s.whatsapp.net",
 *       "fromMe": false,
 *       "id": "3EB0XXXXX"
 *     },
 *     "message": {
 *       "conversation": "Estou com dor 8 hoje"
 *     },
 *     "messageTimestamp": 1708387200,
 *     "pushName": "João Silva"
 *   }
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvolutionApiWebhookDTO {
    
    private String event;
    private String instance;
    private WebhookData data;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookData {
        private Key key;
        private Message message;
        private Long messageTimestamp;
        private String pushName;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Key {
            private String remoteJid;  // "5511999999999@s.whatsapp.net"
            private Boolean fromMe;
            private String id;
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Message {
            private String conversation;  // Texto da mensagem
            private ExtendedTextMessage extendedTextMessage;  // Mensagens com contexto
            
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class ExtendedTextMessage {
                private String text;
            }
        }
    }
    
    /**
     * Extrai o número do WhatsApp (sem @s.whatsapp.net)
     * 
     * @return Número limpo (ex: "5511999999999")
     */
    public String getWhatsappNumber() {
        if (data == null || data.getKey() == null) {
            return null;
        }
        String remoteJid = data.getKey().getRemoteJid();
        if (remoteJid == null) {
            return null;
        }
        return remoteJid.replace("@s.whatsapp.net", "").replace("@c.us", "");
    }
    
    /**
     * Extrai o texto da mensagem
     * Suporta mensagens simples e extendedTextMessage
     * 
     * @return Texto da mensagem
     */
    public String getMessageText() {
        if (data == null || data.getMessage() == null) {
            return null;
        }
        
        // Tentar conversation primeiro
        String conversation = data.getMessage().getConversation();
        if (conversation != null && !conversation.isBlank()) {
            return conversation;
        }
        
        // Tentar extendedTextMessage
        var extended = data.getMessage().getExtendedTextMessage();
        if (extended != null && extended.getText() != null) {
            return extended.getText();
        }
        
        return null;
    }
    
    /**
     * Verifica se a mensagem é do usuário (não é nossa resposta)
     * 
     * @return true se a mensagem é do usuário
     */
    public boolean isFromUser() {
        return data != null && 
               data.getKey() != null && 
               Boolean.FALSE.equals(data.getKey().getFromMe());
    }
    
    /**
     * Obtém o ID único da mensagem (para idempotência)
     * 
     * @return ID da mensagem
     */
    public String getMessageId() {
        if (data == null || data.getKey() == null) {
            return null;
        }
        return data.getKey().getId();
    }
    
    /**
     * Obtém o nome do remetente
     * 
     * @return Nome do remetente
     */
    public String getSenderName() {
        return data != null ? data.getPushName() : null;
    }
}

