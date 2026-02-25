package com.healthlink.ai_health_agent.controller;

import com.healthlink.ai_health_agent.dto.EvolutionApiWebhookDTO;
import com.healthlink.ai_health_agent.repository.PatientRepository;
import com.healthlink.ai_health_agent.security.TenantContext;
import com.healthlink.ai_health_agent.security.TenantContextHolder;
import com.healthlink.ai_health_agent.service.AIService;
import com.healthlink.ai_health_agent.service.EvolutionApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller para receber webhooks da Evolution API
 *
 * Este é o "porteiro" da aplicação - ponto de entrada para mensagens do WhatsApp
 *
 * Fluxo de Segurança:
 * 1. Validar autenticação (X-Webhook-Key)
 * 2. Identificar tenant via whatsappNumber (Projeção Leve)
 * 3. Estabelecer contexto de segurança (TenantContextHolder)
 * 4. Processar com IA (AIService)
 * 5. Enviar resposta (EvolutionApiService)
 * 6. Limpar contexto
 */
@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhook", description = "Endpoint para receber mensagens da Evolution API (WhatsApp)")
public class WhatsappWebhookController {

    private final PatientRepository patientRepository;
    private final AIService aiService;
    private final EvolutionApiService evolutionApiService;
    
    @Value("${evolution.api.webhook-key:default-secret}")
    private String webhookKey;

    /**
     * Endpoint que recebe mensagens da Evolution API
     *
     * POST /webhook/whatsapp
     * Header: X-Webhook-Key: {secret}
     * Body: EvolutionApiWebhookDTO
     */
    @Operation(
            summary = "Receber mensagem do WhatsApp",
            description = """
                    Endpoint webhook para receber mensagens da Evolution API.

                    **Fluxo de Processamento:**
                    1. Valida autenticação (X-Webhook-Key)
                    2. Identifica tenant pelo número do WhatsApp
                    3. Processa mensagem com IA (GPT-4o-mini)
                    4. Salva histórico de conversa
                    5. Executa Function Calling se necessário
                    6. Envia resposta via Evolution API

                    **Segurança:**
                    - Requer header `X-Webhook-Key` configurado no Evolution API
                    - Isolamento multi-tenant automático
                    - Idempotência via messageId
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mensagem processada com sucesso"),
            @ApiResponse(responseCode = "401", description = "API Key inválida"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro no processamento")
    })
    @PostMapping
    public ResponseEntity<?> receiveMessage(
            @Parameter(description = "Chave de autenticação do webhook", required = true, example = "your-secret-key")
            @RequestHeader(value = "X-Webhook-Key", required = false) String apiKey,
            @Parameter(description = "Payload do webhook da Evolution API", required = true)
            @RequestBody EvolutionApiWebhookDTO webhook) {
        
        try {
            log.info("📨 Webhook recebido - Event: {}, Instance: {}", 
                     webhook.getEvent(), webhook.getInstance());

            // ========================================
            // PASSO 1: VALIDAÇÃO DE AUTENTICAÇÃO
            // ========================================
            if (!webhookKey.equals(apiKey)) {
                log.warn("⚠️ Tentativa de acesso não autorizado ao webhook");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "UNAUTHORIZED", "message", "Invalid API Key"));
            }

            // ========================================
            // PASSO 2: FILTRAR MENSAGENS
            // ========================================
            // Ignorar mensagens que enviamos (fromMe=true)
            if (!webhook.isFromUser()) {
                log.debug("⏭️ Mensagem ignorada (fromMe=true)");
                return ResponseEntity.ok(Map.of("status", "ignored", "reason", "fromMe"));
            }

            // ========================================
            // PASSO 3: EXTRAIR DADOS ESSENCIAIS
            // ========================================
            String whatsappNumber = webhook.getWhatsappNumber();
            String messageText = webhook.getMessageText();
            String messageId = webhook.getMessageId();
            
            if (whatsappNumber == null || messageText == null || messageText.isBlank()) {
                log.warn("⚠️ Webhook inválido - número ou mensagem vazia");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "INVALID_DATA", "message", "Missing whatsapp number or message"));
            }

            log.info("📱 Mensagem recebida de {}: \"{}\" (ID: {})", 
                     whatsappNumber, messageText, messageId);

            // ========================================
            // PASSO 4: IDENTIFICAÇÃO DE TENANT
            // ========================================
            // Usa projeção leve para performance
            var projection = patientRepository
                    .findTenantContextByWhatsappNumber(whatsappNumber)
                    .orElseThrow(() -> new PatientNotFoundException(
                            "Paciente não cadastrado: " + whatsappNumber));

            UUID tenantId = projection.getTenantId();
            UUID patientId = projection.getId();
            String patientName = projection.getName();

            log.info("🔐 Tenant identificado: {} | Paciente: {} ({})", 
                     tenantId, patientName, patientId);

            // ========================================
            // PASSO 5: ESTABELECER CONTEXTO DE SEGURANÇA
            // ========================================
            TenantContext context = new TenantContext(tenantId, patientName, whatsappNumber, patientId);
            TenantContextHolder.setContext(context);

            log.debug("✅ Contexto de segurança estabelecido: {}", context);

            // ========================================
            // PASSO 6: PROCESSAR COM IA (COM HISTÓRICO)
            // ========================================
            String aiResponse = aiService.processMessageWithTenant(
                    tenantId,
                    patientId,
                    messageText,
                    messageId
            );

            log.info("🤖 Resposta da IA gerada: {} caracteres", aiResponse.length());

            // ========================================
            // PASSO 7: ENVIAR RESPOSTA VIA EVOLUTION API
            // ========================================
            evolutionApiService.sendMessage(whatsappNumber, aiResponse);

            log.info("✅ Fluxo completo executado com sucesso para {}", whatsappNumber);

            // ========================================
            // PASSO 8: LIMPAR CONTEXTO
            // ========================================
            TenantContextHolder.clear();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "whatsappNumber", whatsappNumber,
                    "tenantId", tenantId.toString(),
                    "patientId", patientId.toString(),
                    "messageId", messageId,
                    "responseLength", aiResponse.length()
            ));

        } catch (PatientNotFoundException e) {
            log.error("❌ Paciente não encontrado: {}", e.getMessage());
            
            // Enviar mensagem de boas-vindas/cadastro
            String welcomeMessage = """
                    Olá! 👋
                    
                    Você ainda não está cadastrado no sistema AI Health Agent.
                    
                    Para começar a usar nosso assistente terapêutico, entre em contato com seu profissional de saúde para realizar o cadastro.
                    """;
            
            try {
                evolutionApiService.sendMessage(webhook.getWhatsappNumber(), welcomeMessage);
            } catch (Exception ex) {
                log.error("Erro ao enviar mensagem de boas-vindas", ex);
            }
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "PATIENT_NOT_FOUND", 
                            "message", e.getMessage(),
                            "whatsappNumber", webhook.getWhatsappNumber()
                    ));

        } catch (Exception e) {
            log.error("❌ Erro ao processar webhook", e);
            
            // Enviar mensagem de erro ao usuário
            try {
                String errorMessage = """
                        Desculpe, ocorreu um erro ao processar sua mensagem. 😔
                        
                        Por favor, tente novamente em alguns instantes.
                        
                        Se o problema persistir, entre em contato com seu profissional de saúde.
                        """;
                
                evolutionApiService.sendMessage(webhook.getWhatsappNumber(), errorMessage);
            } catch (Exception ex) {
                log.error("Erro ao enviar mensagem de erro", ex);
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "PROCESSING_ERROR", 
                            "message", e.getMessage()
                    ));
        } finally {
            // Garantir que o contexto seja limpo mesmo em caso de erro
            TenantContextHolder.clear();
        }
    }

    /**
     * Exception customizada para paciente não encontrado
     */
    public static class PatientNotFoundException extends RuntimeException {
        public PatientNotFoundException(String message) {
            super(message);
        }
    }
}

