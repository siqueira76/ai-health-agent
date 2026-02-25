# 3.2 Modelo de Dados

## 📊 Entidades JPA

Todas as entidades seguem o padrão JPA com Lombok para reduzir boilerplate.

---

## 🏢 Account (Tenant)

```java
@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType; // B2C, B2B
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status; // TRIAL, ACTIVE, SUSPENDED, CANCELLED
    
    @Column(name = "custom_prompt", columnDefinition = "TEXT")
    private String customPrompt;
    
    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Patient> patients;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return status == AccountStatus.ACTIVE || status == AccountStatus.TRIAL;
    }
}
```

---

## 👤 Patient

```java
@Entity
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "whatsapp_number", unique = true, nullable = false)
    private String whatsappNumber;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    private List<HealthLog> healthLogs;
    
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    private List<ChatMessage> chatMessages;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

---

## 📋 HealthLog

```java
@Entity
@Table(name = "health_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "pain_level")
    private Integer painLevel; // 0-10
    
    private String mood;
    
    @Column(name = "sleep_quality")
    private String sleepQuality;
    
    @Column(name = "medications_taken", columnDefinition = "TEXT")
    private String medicationsTaken; // JSON array
    
    @Column(name = "energy_level")
    private String energyLevel;
    
    @Column(name = "stress_level")
    private String stressLevel;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

---

## 💬 ChatMessage

```java
@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "message_text", columnDefinition = "TEXT", nullable = false)
    private String messageText;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageSender sender; // PATIENT, AI
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "context_window")
    private Integer contextWindow; // 1-10 (últimas 10 mensagens)
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
```

---

## 🚨 Alert

```java
@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType; // CRISIS, TREND, MEDICATION
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

---

## 🎯 Próximos Passos

1. 🔄 [Migrations](03-migrations.md)
2. 🔗 [Relacionamentos](04-relationships.md)
3. 🏗️ [Arquitetura em Camadas](../04-architecture/01-layered-architecture.md)

---

[⬅️ Anterior: Estrutura do Banco](01-database-structure.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Migrations](03-migrations.md)

