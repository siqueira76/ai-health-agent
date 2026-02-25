package com.healthlink.ai_health_agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração do Swagger/OpenAPI
 * Documentação interativa da API
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Health Agent API")
                        .version("1.0.0")
                        .description("""
                                # AI Health Agent - API REST
                                
                                Sistema multi-tenant de monitoramento de saúde via WhatsApp com Inteligência Artificial.
                                
                                ## Funcionalidades
                                
                                - 🤖 **IA Conversacional** - GPT-4o-mini com memória de contexto
                                - 📊 **Analytics** - Estatísticas, tendências e insights
                                - 🚨 **Alertas Automáticos** - Detecção de crises e padrões críticos
                                - 💬 **Chat History** - Histórico completo de conversas
                                - 🔐 **Multi-Tenancy** - Isolamento completo de dados por tenant
                                - 📈 **Dashboard** - Visualização de dados para profissionais
                                
                                ## Modelos de Negócio
                                
                                - **B2C (Fibromialgia)**: Monitoramento direto de pacientes
                                - **B2B (Psicólogos)**: Plataforma para profissionais gerenciarem múltiplos pacientes
                                
                                ## Autenticação
                                
                                Todas as requisições requerem o parâmetro `tenantId` (UUID do Account).
                                O webhook requer header `X-Webhook-Key` para autenticação.
                                
                                ## Tecnologias
                                
                                - Spring Boot 4.0.2
                                - Spring AI 1.0.0-M5
                                - PostgreSQL
                                - Evolution API (WhatsApp)
                                - OpenAI GPT-4o-mini
                                """)
                        .contact(new Contact()
                                .name("HealthLink")
                                .email("contato@healthlink.com")
                                .url("https://healthlink.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor Local"),
                        new Server()
                                .url("https://api.healthlink.com")
                                .description("Servidor de Produção")))
                .tags(List.of(
                        new Tag()
                                .name("Dashboard")
                                .description("Endpoints para visualização de estatísticas e alertas"),
                        new Tag()
                                .name("Webhook")
                                .description("Endpoints para receber mensagens da Evolution API"),
                        new Tag()
                                .name("Patients")
                                .description("Gerenciamento de pacientes")
                ));
    }
}

