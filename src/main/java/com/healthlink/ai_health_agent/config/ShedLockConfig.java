package com.healthlink.ai_health_agent.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * Configuração do ShedLock para lock distribuído em jobs agendados.
 * 
 * ShedLock garante que jobs @Scheduled sejam executados apenas uma vez,
 * mesmo em ambientes com múltiplas instâncias da aplicação (Railway, Docker, Kubernetes).
 * 
 * Funcionamento:
 * - Antes de executar um job, tenta adquirir um lock no banco de dados
 * - Se conseguir o lock, executa o job
 * - Se não conseguir (outra instância já está executando), pula a execução
 * - Lock é liberado automaticamente após o tempo configurado
 * 
 * Tabela necessária: shedlock (criada na migration V5)
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

    /**
     * Configura o provedor de lock usando JDBC (PostgreSQL)
     * 
     * @param dataSource DataSource do Spring Boot
     * @return LockProvider configurado
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime() // Usa timestamp do banco de dados (mais confiável)
                .build()
        );
    }
}

