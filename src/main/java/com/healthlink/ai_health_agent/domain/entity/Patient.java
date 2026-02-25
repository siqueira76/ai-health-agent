package com.healthlink.ai_health_agent.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade Patient - Representa um paciente no sistema
 * Isolado por tenantId (account_id) para garantir multi-tenancy
 * Chave primária lógica: whatsappNumber (único por tenant)
 */
@Entity
@Table(name = "patients", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_patient_whatsapp", columnNames = {"whatsapp_number"})
    },
    indexes = {
        @Index(name = "idx_patient_account", columnList = "account_id"),
        @Index(name = "idx_patient_whatsapp", columnList = "whatsapp_number"),
        @Index(name = "idx_patient_active", columnList = "is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * TENANT ID - Referência à conta (Account) que gerencia este paciente
     * Este campo é fundamental para o isolamento multi-tenant
     */
    @NotNull(message = "Account é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_patient_account"))
    private Account account;

    /**
     * Número do WhatsApp (chave primária lógica)
     * Formato: 5511999999999 (código do país + DDD + número)
     */
    @NotBlank(message = "Número do WhatsApp é obrigatório")
    @Pattern(regexp = "^\\d{12,15}$", message = "Número do WhatsApp inválido")
    @Column(name = "whatsapp_number", unique = true, nullable = false, length = 15)
    private String whatsappNumber;

    /**
     * Nome do paciente
     */
    @NotBlank(message = "Nome é obrigatório")
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Email do paciente (opcional)
     */
    @Column(name = "email")
    private String email;

    /**
     * Data de nascimento (opcional, mas útil para contexto terapêutico)
     */
    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    /**
     * Diagnóstico principal (ex: "Fibromialgia", "Ansiedade", "Depressão")
     */
    @Column(name = "diagnosis")
    private String diagnosis;

    /**
     * Notas adicionais sobre o paciente (histórico, observações)
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Indica se o paciente está ativo no sistema
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Última interação do paciente com o sistema
     */
    @Column(name = "last_interaction_at")
    private LocalDateTime lastInteractionAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Atualiza o timestamp da última interação
     */
    public void updateLastInteraction() {
        this.lastInteractionAt = LocalDateTime.now();
    }

    /**
     * Desativa o paciente
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Ativa o paciente
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Retorna o ID do tenant (account) para facilitar queries
     */
    public UUID getTenantId() {
        return account != null ? account.getId() : null;
    }

    /**
     * Formata o número do WhatsApp para exibição
     * Ex: 5511999999999 -> +55 (11) 99999-9999
     */
    public String getFormattedWhatsappNumber() {
        if (whatsappNumber == null || whatsappNumber.length() < 12) {
            return whatsappNumber;
        }
        
        String countryCode = whatsappNumber.substring(0, 2);
        String areaCode = whatsappNumber.substring(2, 4);
        String firstPart = whatsappNumber.substring(4, 9);
        String secondPart = whatsappNumber.substring(9);
        
        return String.format("+%s (%s) %s-%s", countryCode, areaCode, firstPart, secondPart);
    }
}

