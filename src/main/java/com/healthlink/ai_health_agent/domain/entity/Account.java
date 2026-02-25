package com.healthlink.ai_health_agent.domain.entity;

import com.healthlink.ai_health_agent.domain.enums.AccountStatus;
import com.healthlink.ai_health_agent.domain.enums.AccountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade Account - Representa o Tenant no sistema multi-tenant
 * Pode ser uma conta B2C (paciente direto) ou B2B (psicólogo)
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_cpf", columnList = "cpf"),
    @Index(name = "idx_account_email", columnList = "email"),
    @Index(name = "idx_account_type", columnList = "type"),
    @Index(name = "idx_account_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * CPF do titular da conta (B2C: paciente, B2B: psicólogo)
     */
    @NotBlank(message = "CPF é obrigatório")
    @Column(name = "cpf", unique = true, nullable = false, length = 11)
    private String cpf;

    /**
     * Nome completo do titular
     */
    @NotBlank(message = "Nome é obrigatório")
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Email do titular
     */
    @Email(message = "Email inválido")
    @NotBlank(message = "Email é obrigatório")
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    /**
     * Tipo de conta: B2C ou B2B
     */
    @NotNull(message = "Tipo de conta é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private AccountType type;

    /**
     * Status da conta
     */
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.TRIAL;

    /**
     * Prompt customizado para a IA (usado principalmente em contas B2B)
     * Permite que psicólogos personalizem a abordagem terapêutica
     */
    @Column(name = "custom_prompt", columnDefinition = "TEXT")
    private String customPrompt;

    /**
     * Limite de slots/pacientes (apenas para contas B2B)
     * NULL ou 0 = ilimitado
     */
    @Column(name = "limit_slots")
    private Integer limitSlots;

    /**
     * Pacientes vinculados a esta conta (tenant)
     */
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Patient> patients = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Verifica se a conta está ativa
     */
    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE || this.status == AccountStatus.TRIAL;
    }

    /**
     * Verifica se a conta é do tipo B2B (psicólogo)
     */
    public boolean isB2B() {
        return this.type == AccountType.B2B;
    }

    /**
     * Verifica se a conta é do tipo B2C (paciente direto)
     */
    public boolean isB2C() {
        return this.type == AccountType.B2C;
    }

    /**
     * Verifica se a conta atingiu o limite de slots (apenas B2B)
     */
    public boolean hasReachedSlotLimit() {
        if (!isB2B() || limitSlots == null || limitSlots == 0) {
            return false;
        }
        return patients.size() >= limitSlots;
    }

    /**
     * Adiciona um paciente à conta
     */
    public void addPatient(Patient patient) {
        patients.add(patient);
        patient.setAccount(this);
    }

    /**
     * Remove um paciente da conta
     */
    public void removePatient(Patient patient) {
        patients.remove(patient);
        patient.setAccount(null);
    }
}

