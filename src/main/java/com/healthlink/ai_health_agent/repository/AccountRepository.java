package com.healthlink.ai_health_agent.repository;

import com.healthlink.ai_health_agent.domain.entity.Account;
import com.healthlink.ai_health_agent.domain.enums.AccountStatus;
import com.healthlink.ai_health_agent.domain.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para Account (Tenant)
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Busca conta por CPF
     */
    Optional<Account> findByCpf(String cpf);

    /**
     * Busca conta por email
     */
    Optional<Account> findByEmail(String email);

    /**
     * Verifica se existe conta com o CPF informado
     */
    boolean existsByCpf(String cpf);

    /**
     * Verifica se existe conta com o email informado
     */
    boolean existsByEmail(String email);

    /**
     * Lista todas as contas por tipo
     */
    List<Account> findByType(AccountType type);

    /**
     * Lista todas as contas por status
     */
    List<Account> findByStatus(AccountStatus status);

    /**
     * Lista contas B2B ativas
     */
    @Query("SELECT a FROM Account a WHERE a.type = 'B2B' AND a.status IN ('ACTIVE', 'TRIAL')")
    List<Account> findActiveB2BAccounts();

    /**
     * Lista contas B2C ativas
     */
    @Query("SELECT a FROM Account a WHERE a.type = 'B2C' AND a.status IN ('ACTIVE', 'TRIAL')")
    List<Account> findActiveB2CAccounts();

    /**
     * Busca contas que atingiram o limite de slots
     */
    @Query("SELECT a FROM Account a WHERE a.type = 'B2B' AND a.limitSlots IS NOT NULL AND SIZE(a.patients) >= a.limitSlots")
    List<Account> findAccountsAtSlotLimit();

    /**
     * Conta total de contas por tipo
     */
    Long countByType(AccountType type);

    /**
     * Conta total de contas por status
     */
    Long countByStatus(AccountStatus status);
}

