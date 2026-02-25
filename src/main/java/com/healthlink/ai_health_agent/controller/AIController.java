package com.healthlink.ai_health_agent.controller;

import com.healthlink.ai_health_agent.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller para endpoints de IA
 * Demonstra o uso do AIService com prompts dinâmicos
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIService aiService;

    /**
     * Endpoint para processar mensagem (simula webhook do WhatsApp)
     * 
     * POST /api/ai/message
     * Body: { "whatsappNumber": "5511999999999", "message": "Olá, como você está?" }
     */
    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> processMessage(
            @RequestBody Map<String, String> request) {
        
        String whatsappNumber = request.get("whatsappNumber");
        String message = request.get("message");

        log.info("Recebida mensagem de {}: {}", whatsappNumber, message);

        String response = aiService.processMessage(whatsappNumber, message);

        return ResponseEntity.ok(Map.of(
                "whatsappNumber", whatsappNumber,
                "response", response
        ));
    }

    /**
     * Preview do prompt customizado de um tenant
     * 
     * GET /api/ai/prompt/preview/{tenantId}
     */
    @GetMapping("/prompt/preview/{tenantId}")
    public ResponseEntity<Map<String, String>> previewPrompt(
            @PathVariable UUID tenantId) {
        
        String prompt = aiService.previewSystemPrompt(tenantId);

        return ResponseEntity.ok(Map.of(
                "tenantId", tenantId.toString(),
                "systemPrompt", prompt
        ));
    }

    /**
     * Atualiza o prompt customizado de um tenant
     * 
     * PUT /api/ai/prompt/{tenantId}
     * Body: { "customPrompt": "Você é um assistente..." }
     */
    @PutMapping("/prompt/{tenantId}")
    public ResponseEntity<Map<String, String>> updatePrompt(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, String> request) {
        
        String customPrompt = request.get("customPrompt");

        aiService.updateCustomPrompt(tenantId, customPrompt);

        return ResponseEntity.ok(Map.of(
                "message", "Prompt customizado atualizado com sucesso",
                "tenantId", tenantId.toString()
        ));
    }
}

