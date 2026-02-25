package com.healthlink.ai_health_agent.config;

import com.healthlink.ai_health_agent.dto.HealthStatsRequest;
import com.healthlink.ai_health_agent.security.TenantContextHolder;
import com.healthlink.ai_health_agent.service.HealthLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * Configuração de Function Calling para Spring AI
 * 
 * Define as funções que a IA pode chamar durante a conversa
 * para executar ações específicas (salvar dados, buscar informações, etc)
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class FunctionCallingConfig {

    private final HealthLogService healthLogService;

    /**
     * Função para registrar dados de saúde diários
     * 
     * A IA chama esta função quando identifica informações de saúde na conversa
     * 
     * Exemplo de uso pela IA:
     * Paciente: "Estou com dor 8 hoje, não dormi bem"
     * IA identifica: painLevel=8, sleepQuality="ruim"
     * IA chama: recordDailyHealthStats(painLevel=8, sleepQuality="ruim")
     * IA responde: "Entendi, registrei sua dor nível 8 e que você não dormiu bem..."
     */
    @Bean
    @Description("Records daily health statistics including pain level, mood, sleep quality, and medication adherence. " +
                 "Use this function when the patient mentions any health-related information such as pain levels (0-10), " +
                 "mood (bem/ansioso/triste/irritado/deprimido), sleep quality (ótimo/bom/regular/ruim/péssimo), " +
                 "or whether they took their medication. Always call this function to save important health data.")
    public Function<HealthStatsRequest, String> recordDailyHealthStats() {
        return request -> {
            try {
                log.info("🔧 Function Calling: recordDailyHealthStats");
                log.debug("Request: {}", request.getSummary());

                // Obter contexto do tenant da thread atual
                var context = TenantContextHolder.getContext();
                
                if (context == null || !context.isValid()) {
                    log.error("❌ Contexto de tenant não encontrado ou inválido");
                    return "Erro: contexto de segurança não estabelecido.";
                }

                log.debug("Contexto obtido - Tenant: {}, Patient: {}", 
                          context.getTenantId(), context.getPatientId());

                // Chamar o service para salvar os dados
                String result = healthLogService.recordHealthStats(
                        context.getTenantId(),
                        context.getPatientId(),
                        request
                );

                log.info("✅ Function Calling executado com sucesso");
                return result;

            } catch (SecurityException e) {
                log.error("🚨 Erro de segurança no Function Calling: {}", e.getMessage());
                return "Erro de segurança ao registrar dados.";
                
            } catch (Exception e) {
                log.error("❌ Erro ao executar Function Calling: {}", e.getMessage(), e);
                return "Erro ao registrar dados de saúde. Por favor, tente novamente.";
            }
        };
    }

    /**
     * Função para buscar histórico de dor (exemplo de função de consulta)
     * 
     * A IA pode chamar esta função para obter contexto sobre a evolução do paciente
     */
    @Bean
    @Description("Retrieves the patient's pain history for the last 7 days. " +
                 "Use this function when you need to understand the patient's pain trends or " +
                 "when the patient asks about their pain evolution.")
    public Function<Void, String> getPainHistory() {
        return unused -> {
            try {
                log.info("🔧 Function Calling: getPainHistory");

                var context = TenantContextHolder.getContext();
                
                if (context == null || !context.isValid()) {
                    return "Não foi possível acessar seu histórico no momento.";
                }

                // Buscar últimos 7 registros
                var recentLogs = healthLogService.getRecentLogs(
                        context.getTenantId(),
                        context.getPatientId(),
                        7
                );

                if (recentLogs.isEmpty()) {
                    return "Ainda não há registros de dor no seu histórico.";
                }

                // Construir resumo
                StringBuilder summary = new StringBuilder("Seus últimos registros de dor:\n");
                
                recentLogs.forEach(log -> {
                    if (log.getPainLevel() != null) {
                        summary.append("- ")
                               .append(log.getTimestamp().toLocalDate())
                               .append(": dor nível ")
                               .append(log.getPainLevel())
                               .append("/10\n");
                    }
                });

                log.info("✅ Histórico de dor recuperado: {} registros", recentLogs.size());
                return summary.toString();

            } catch (Exception e) {
                log.error("❌ Erro ao buscar histórico de dor: {}", e.getMessage(), e);
                return "Erro ao buscar histórico de dor.";
            }
        };
    }

    /**
     * Função para verificar se a medicação foi tomada hoje
     */
    @Bean
    @Description("Checks if the patient has taken their medication today. " +
                 "Use this function when you need to remind the patient about medication or " +
                 "when the patient asks if they already took their medication today.")
    public Function<Void, String> checkMedicationToday() {
        return unused -> {
            try {
                log.info("🔧 Function Calling: checkMedicationToday");

                var context = TenantContextHolder.getContext();
                
                if (context == null || !context.isValid()) {
                    return "Não foi possível verificar a medicação no momento.";
                }

                var todayLogs = healthLogService.getTodayLogs(
                        context.getTenantId(),
                        context.getPatientId()
                );

                // Verificar se há registro de medicação tomada hoje
                boolean tookMedication = todayLogs.stream()
                        .anyMatch(log -> Boolean.TRUE.equals(log.getMedicationTaken()));

                if (tookMedication) {
                    return "Sim, você já registrou que tomou sua medicação hoje.";
                } else {
                    return "Não há registro de medicação tomada hoje. Você já tomou?";
                }

            } catch (Exception e) {
                log.error("❌ Erro ao verificar medicação: {}", e.getMessage(), e);
                return "Erro ao verificar medicação.";
            }
        };
    }
}

