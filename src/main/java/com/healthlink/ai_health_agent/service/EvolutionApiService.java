package com.healthlink.ai_health_agent.service;

import com.healthlink.ai_health_agent.dto.EvolutionApiSendMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service para integração com Evolution API
 * Responsável por enviar mensagens via WhatsApp
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EvolutionApiService {

    @Value("${evolution.api.url}")
    private String evolutionApiUrl;

    @Value("${evolution.api.key}")
    private String evolutionApiKey;

    @Value("${evolution.api.instance}")
    private String instanceName;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Envia mensagem de texto via Evolution API
     * 
     * @param whatsappNumber Número do destinatário (5511999999999)
     * @param message Texto da mensagem
     */
    public void sendMessage(String whatsappNumber, String message) {
        try {
            String url = String.format("%s/message/sendText/%s", evolutionApiUrl, instanceName);

            log.debug("Enviando mensagem para {} via Evolution API: {}", whatsappNumber, url);

            EvolutionApiSendMessageDTO payload = new EvolutionApiSendMessageDTO(
                    whatsappNumber,
                    message,
                    1000  // Delay de 1 segundo para parecer mais humano
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", evolutionApiKey);

            HttpEntity<EvolutionApiSendMessageDTO> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Mensagem enviada com sucesso para {}: {} caracteres", 
                         whatsappNumber, message.length());
            } else {
                log.error("❌ Erro ao enviar mensagem: Status {}", response.getStatusCode());
                throw new RuntimeException("Evolution API retornou status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Erro ao enviar mensagem via Evolution API para {}: {}", 
                      whatsappNumber, e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar mensagem via WhatsApp", e);
        }
    }

    /**
     * Envia mensagem com delay customizado
     * 
     * @param whatsappNumber Número do destinatário
     * @param message Texto da mensagem
     * @param delayMs Delay em milissegundos
     */
    public void sendMessageWithDelay(String whatsappNumber, String message, int delayMs) {
        try {
            String url = String.format("%s/message/sendText/%s", evolutionApiUrl, instanceName);

            EvolutionApiSendMessageDTO payload = new EvolutionApiSendMessageDTO(
                    whatsappNumber,
                    message,
                    delayMs
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", evolutionApiKey);

            HttpEntity<EvolutionApiSendMessageDTO> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Mensagem enviada com delay de {}ms para {}", delayMs, whatsappNumber);
            } else {
                log.error("❌ Erro ao enviar mensagem: Status {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Erro ao enviar mensagem com delay: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar mensagem via WhatsApp", e);
        }
    }

    /**
     * Verifica se a Evolution API está acessível
     * 
     * @return true se a API está online
     */
    public boolean isApiAvailable() {
        try {
            String url = evolutionApiUrl + "/instance/fetchInstances";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", evolutionApiKey);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            log.error("Evolution API não está acessível: {}", e.getMessage());
            return false;
        }
    }
}

