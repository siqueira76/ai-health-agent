# 3.4 Relacionamentos

## 🔗 Diagrama de Relacionamentos

```
accounts (1) ──────< (N) patients
    │                      │
    │                      ├──────< (N) health_logs
    │                      ├──────< (N) chat_messages
    │                      ├──────< (N) alerts
    │                      └──────< (N) checkin_schedules
    │                                     │
    │                                     └──────< (N) checkin_executions
    │
    ├──────< (N) health_logs
    ├──────< (N) chat_messages
    ├──────< (N) alerts
    └──────< (N) checkin_schedules
```

---

## 📊 Relacionamentos Detalhados

### **1. Account → Patients (1:N)**

**Descrição:** Um tenant (account) pode ter múltiplos pacientes.

**SQL:**
```sql
ALTER TABLE patients 
  ADD CONSTRAINT fk_patients_account 
  FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE;
```

**JPA:**
```java
// Account.java
@OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
private List<Patient> patients;

// Patient.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "account_id", nullable = false)
private Account account;
```

**Comportamento:**
- ✅ Deletar account → deleta todos os patients
- ✅ Lazy loading (não carrega patients automaticamente)

---

### **2. Patient → HealthLogs (1:N)**

**Descrição:** Um paciente pode ter múltiplos registros de saúde.

**SQL:**
```sql
ALTER TABLE health_logs 
  ADD CONSTRAINT fk_health_logs_patient 
  FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE;
```

**JPA:**
```java
// Patient.java
@OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
private List<HealthLog> healthLogs;

// HealthLog.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "patient_id", nullable = false)
private Patient patient;
```

---

### **3. Patient → ChatMessages (1:N)**

**Descrição:** Um paciente pode ter múltiplas mensagens de chat.

**SQL:**
```sql
ALTER TABLE chat_messages 
  ADD CONSTRAINT fk_chat_messages_patient 
  FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE;
```

**JPA:**
```java
// Patient.java
@OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
private List<ChatMessage> chatMessages;

// ChatMessage.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "patient_id", nullable = false)
private Patient patient;
```

---

### **4. Patient → Alerts (1:N)**

**Descrição:** Um paciente pode ter múltiplos alertas.

**SQL:**
```sql
ALTER TABLE alerts 
  ADD CONSTRAINT fk_alerts_patient 
  FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE;
```

**JPA:**
```java
// Patient.java
@OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
private List<Alert> alerts;

// Alert.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "patient_id", nullable = false)
private Patient patient;
```

---

### **5. CheckinSchedule → CheckinExecutions (1:N)**

**Descrição:** Um agendamento pode ter múltiplas execuções.

**SQL:**
```sql
ALTER TABLE checkin_executions 
  ADD CONSTRAINT fk_checkin_executions_schedule 
  FOREIGN KEY (schedule_id) REFERENCES checkin_schedules(id) ON DELETE CASCADE;
```

**JPA:**
```java
// CheckinSchedule.java
@OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL)
private List<CheckinExecution> executions;

// CheckinExecution.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "schedule_id", nullable = false)
private CheckinSchedule schedule;
```

---

## 🔐 Multi-Tenancy (Isolamento)

Todas as tabelas principais têm `account_id` para garantir isolamento:

```sql
-- Exemplo: Buscar health logs de um paciente
SELECT * FROM health_logs 
WHERE patient_id = ? 
  AND account_id = ?; -- Isolamento de tenant
```

**Implementação:**
```java
@Repository
public interface HealthLogRepository extends JpaRepository<HealthLog, UUID> {
    
    @Query("SELECT h FROM HealthLog h WHERE h.patient.id = :patientId AND h.account.id = :tenantId")
    List<HealthLog> findByPatientAndTenant(
        @Param("patientId") UUID patientId, 
        @Param("tenantId") UUID tenantId
    );
}
```

---

## 📈 Índices para Performance

### **Índices de Foreign Keys**

```sql
-- Pacientes por account
CREATE INDEX idx_patients_account ON patients(account_id);

-- Health logs por paciente
CREATE INDEX idx_health_logs_patient ON health_logs(patient_id, created_at DESC);

-- Chat messages por paciente
CREATE INDEX idx_chat_messages_patient ON chat_messages(patient_id, timestamp DESC);

-- Alertas por paciente
CREATE INDEX idx_alerts_patient ON alerts(patient_id, created_at DESC);

-- Alertas não lidos por account
CREATE INDEX idx_alerts_unread ON alerts(account_id, is_read) WHERE is_read = false;
```

---

## 🔄 Cascade Behaviors

### **ON DELETE CASCADE**

Quando um registro pai é deletado, todos os filhos são deletados automaticamente:

```sql
-- Deletar account → deleta patients, health_logs, chat_messages, alerts
DELETE FROM accounts WHERE id = '123...';

-- Deletar patient → deleta health_logs, chat_messages, alerts
DELETE FROM patients WHERE id = '456...';
```

### **JPA Cascade Types**

```java
@OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
private List<Patient> patients;
```

**Tipos:**
- `CascadeType.ALL` - Todas as operações
- `CascadeType.PERSIST` - Apenas insert
- `CascadeType.MERGE` - Apenas update
- `CascadeType.REMOVE` - Apenas delete
- `CascadeType.REFRESH` - Recarregar do banco

---

## 🎯 Queries Comuns

### **Buscar pacientes de um account**

```java
@Query("SELECT p FROM Patient p WHERE p.account.id = :accountId AND p.isActive = true")
List<Patient> findActivePatientsByAccount(@Param("accountId") UUID accountId);
```

### **Buscar últimos health logs de um paciente**

```java
@Query("SELECT h FROM HealthLog h WHERE h.patient.id = :patientId ORDER BY h.createdAt DESC")
List<HealthLog> findRecentByPatient(@Param("patientId") UUID patientId, Pageable pageable);
```

### **Buscar alertas não lidos de um account**

```java
@Query("SELECT a FROM Alert a WHERE a.account.id = :accountId AND a.isRead = false ORDER BY a.createdAt DESC")
List<Alert> findUnreadByAccount(@Param("accountId") UUID accountId);
```

---

## 🎯 Próximos Passos

1. 🏗️ [Arquitetura em Camadas](../04-architecture/01-layered-architecture.md)
2. 🔐 [Multi-Tenancy](../04-architecture/02-multi-tenancy.md)
3. 🤖 [Spring AI Setup](../05-ai/01-spring-ai-setup.md)

---

[⬅️ Anterior: Migrations](03-migrations.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Arquitetura em Camadas](../04-architecture/01-layered-architecture.md)

