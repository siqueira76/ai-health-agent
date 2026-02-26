# 5.4 Extração de Dados de Saúde

## 📊 Extração Estruturada de Dados

Como transformar conversas naturais em dados estruturados para análise.

---

## 🎯 Objetivo

Converter isto:
```
"Oi, hoje acordei com dor de cabeça nível 8, não consegui dormir 
direito e estou me sentindo muito ansioso. Tomei dipirona às 9h."
```

Em isto:
```json
{
  "painLevel": 8,
  "painType": "dor de cabeça",
  "sleepQuality": "ruim",
  "mood": "ansioso",
  "medicationsTaken": ["dipirona 9h"],
  "timestamp": "2024-01-15T09:30:00Z"
}
```

---

## 🔧 Implementação com Function Calling

### **1. DTO de Extração:**

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthDataRequest {
    
    @JsonProperty("pain_level")
    @JsonPropertyDescription("Nível de dor de 0 a 10. Null se não mencionado.")
    private Integer painLevel;
    
    @JsonProperty("pain_location")
    @JsonPropertyDescription("Localização da dor: cabeça, costas, articulações, etc")
    private String painLocation;
    
    @JsonProperty("pain_type")
    @JsonPropertyDescription("Tipo de dor: pulsátil, aguda, constante, pontada")
    private String painType;
    
    @JsonProperty("mood")
    @JsonPropertyDescription("Estado emocional: feliz, triste, ansioso, irritado, calmo")
    private String mood;
    
    @JsonProperty("sleep_quality")
    @JsonPropertyDescription("Qualidade do sono: ótimo, bom, regular, ruim, péssimo")
    private String sleepQuality;
    
    @JsonProperty("sleep_hours")
    @JsonPropertyDescription("Horas de sono. Null se não mencionado.")
    private Double sleepHours;
    
    @JsonProperty("medications_taken")
    @JsonPropertyDescription("Lista de medicamentos com horário. Ex: ['dipirona 9h', 'ibuprofeno 14h']")
    private List<String> medicationsTaken;
    
    @JsonProperty("energy_level")
    @JsonPropertyDescription("Nível de energia: alto, médio, baixo")
    private String energyLevel;
    
    @JsonProperty("stress_level")
    @JsonPropertyDescription("Nível de estresse: baixo, médio, alto")
    private String stressLevel;
    
    @JsonProperty("appetite")
    @JsonPropertyDescription("Apetite: normal, aumentado, diminuído, sem apetite")
    private String appetite;
    
    @JsonProperty("physical_activity")
    @JsonPropertyDescription("Atividade física realizada. Ex: 'caminhada 30min'")
    private String physicalActivity;
    
    @JsonProperty("triggers")
    @JsonPropertyDescription("Gatilhos identificados: estresse, alimentos, clima, etc")
    private List<String> triggers;
    
    @JsonProperty("notes")
    @JsonPropertyDescription("Observações adicionais importantes")
    private String notes;
}
```

---

## 💾 Salvando no Banco

### **Function de Extração:**

```java
@Slf4j
@Component
@Description("Extrai e salva dados de saúde estruturados da conversa")
public class ExtractHealthDataFunction implements Function<HealthDataRequest, String> {
    
    private final HealthLogRepository healthLogRepository;
    private final AlertService alertService;
    
    @Override
    public String apply(HealthDataRequest request) {
        try {
            // 1. Obter paciente do contexto
            Patient patient = PatientContext.getCurrentPatient();
            if (patient == null) {
                log.error("Patient context not set");
                return "Erro: contexto do paciente não definido";
            }
            
            // 2. Criar HealthLog
            HealthLog healthLog = HealthLog.builder()
                .account(patient.getAccount())
                .patient(patient)
                .painLevel(request.getPainLevel())
                .painLocation(request.getPainLocation())
                .painType(request.getPainType())
                .mood(request.getMood())
                .sleepQuality(request.getSleepQuality())
                .sleepHours(request.getSleepHours())
                .medicationsTaken(joinList(request.getMedicationsTaken()))
                .energyLevel(request.getEnergyLevel())
                .stressLevel(request.getStressLevel())
                .appetite(request.getAppetite())
                .physicalActivity(request.getPhysicalActivity())
                .triggers(joinList(request.getTriggers()))
                .notes(request.getNotes())
                .build();
            
            // 3. Salvar
            HealthLog saved = healthLogRepository.save(healthLog);
            log.info("HealthLog saved: {} for patient: {}", saved.getId(), patient.getId());
            
            // 4. Avaliar alertas
            alertService.evaluateAlerts(patient, saved);
            
            // 5. Retornar confirmação
            return buildConfirmationMessage(request);
            
        } catch (Exception e) {
            log.error("Error extracting health data", e);
            return "Erro ao salvar dados de saúde";
        }
    }
    
    private String joinList(List<String> list) {
        return list != null && !list.isEmpty() 
            ? String.join(", ", list) 
            : null;
    }
    
    private String buildConfirmationMessage(HealthDataRequest request) {
        StringBuilder msg = new StringBuilder("Dados registrados:\n");
        
        if (request.getPainLevel() != null) {
            msg.append("- Dor nível ").append(request.getPainLevel());
            if (request.getPainLocation() != null) {
                msg.append(" (").append(request.getPainLocation()).append(")");
            }
            msg.append("\n");
        }
        
        if (request.getMood() != null) {
            msg.append("- Humor: ").append(request.getMood()).append("\n");
        }
        
        if (request.getSleepQuality() != null) {
            msg.append("- Sono: ").append(request.getSleepQuality()).append("\n");
        }
        
        if (request.getMedicationsTaken() != null && !request.getMedicationsTaken().isEmpty()) {
            msg.append("- Medicamentos: ").append(String.join(", ", request.getMedicationsTaken())).append("\n");
        }
        
        return msg.toString();
    }
}
```

---

## 🎯 Validação de Dados

### **Validação no DTO:**

```java
@Data
@Builder
public class HealthDataRequest {
    
    @JsonProperty("pain_level")
    @Min(0)
    @Max(10)
    private Integer painLevel;
    
    @JsonProperty("sleep_hours")
    @Min(0)
    @Max(24)
    private Double sleepHours;
    
    @JsonProperty("mood")
    @Pattern(regexp = "feliz|triste|ansioso|irritado|calmo|neutro")
    private String mood;
}
```

### **Validação no Service:**

```java
@Override
public String apply(HealthDataRequest request) {
    // Validar dados
    if (request.getPainLevel() != null && (request.getPainLevel() < 0 || request.getPainLevel() > 10)) {
        return "Erro: nível de dor deve estar entre 0 e 10";
    }
    
    if (request.getSleepHours() != null && request.getSleepHours() > 24) {
        return "Erro: horas de sono não podem exceder 24h";
    }
    
    // Continuar processamento...
}
```

---

## 📈 Análise de Dados Extraídos

### **Tendências de Dor:**

```java
@Service
public class HealthAnalyticsService {
    
    public PainTrendDTO analyzePainTrend(UUID patientId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        List<HealthLog> logs = healthLogRepository
            .findByPatientAndCreatedAtAfter(patientId, startDate);
        
        double avgPain = logs.stream()
            .filter(log -> log.getPainLevel() != null)
            .mapToInt(HealthLog::getPainLevel)
            .average()
            .orElse(0);
        
        int highPainDays = (int) logs.stream()
            .filter(log -> log.getPainLevel() != null && log.getPainLevel() >= 7)
            .count();
        
        return PainTrendDTO.builder()
            .averagePain(avgPain)
            .highPainDays(highPainDays)
            .totalDays(days)
            .trend(calculateTrend(logs))
            .build();
    }
    
    private String calculateTrend(List<HealthLog> logs) {
        if (logs.size() < 2) return "INSUFFICIENT_DATA";
        
        // Comparar primeira metade com segunda metade
        int midpoint = logs.size() / 2;
        double firstHalf = logs.subList(0, midpoint).stream()
            .filter(log -> log.getPainLevel() != null)
            .mapToInt(HealthLog::getPainLevel)
            .average()
            .orElse(0);
        
        double secondHalf = logs.subList(midpoint, logs.size()).stream()
            .filter(log -> log.getPainLevel() != null)
            .mapToInt(HealthLog::getPainLevel)
            .average()
            .orElse(0);
        
        if (secondHalf > firstHalf + 1) return "WORSENING";
        if (secondHalf < firstHalf - 1) return "IMPROVING";
        return "STABLE";
    }
}
```

---

## 🔔 Geração de Alertas

### **Alertas Automáticos:**

```java
@Service
public class AlertService {
    
    public void evaluateAlerts(Patient patient, HealthLog log) {
        // 1. Dor alta
        if (log.getPainLevel() != null && log.getPainLevel() >= 8) {
            createAlert(patient, AlertType.HIGH_PAIN, AlertSeverity.HIGH,
                "Dor intensa detectada: nível " + log.getPainLevel());
        }
        
        // 2. Sono ruim por 3+ dias
        long badSleepDays = healthLogRepository
            .countRecentBadSleep(patient.getId(), 3);
        
        if (badSleepDays >= 3) {
            createAlert(patient, AlertType.POOR_SLEEP, AlertSeverity.MEDIUM,
                "Sono ruim por " + badSleepDays + " dias consecutivos");
        }
        
        // 3. Humor negativo persistente
        if ("triste".equals(log.getMood()) || "ansioso".equals(log.getMood())) {
            long negativeMoodDays = healthLogRepository
                .countRecentNegativeMood(patient.getId(), 5);
            
            if (negativeMoodDays >= 5) {
                createAlert(patient, AlertType.MENTAL_HEALTH, AlertSeverity.HIGH,
                    "Humor negativo por " + negativeMoodDays + " dias");
            }
        }
        
        // 4. Sem medicação quando deveria tomar
        if (patient.getHasRegularMedication() && 
            (log.getMedicationsTaken() == null || log.getMedicationsTaken().isEmpty())) {
            createAlert(patient, AlertType.MEDICATION_MISSED, AlertSeverity.LOW,
                "Possível esquecimento de medicação");
        }
    }
    
    private void createAlert(Patient patient, AlertType type, 
                            AlertSeverity severity, String message) {
        Alert alert = Alert.builder()
            .account(patient.getAccount())
            .patient(patient)
            .alertType(type)
            .severity(severity)
            .message(message)
            .isResolved(false)
            .build();
        
        alertRepository.save(alert);
        
        // Notificar profissional de saúde (se B2B)
        if (severity == AlertSeverity.HIGH) {
            notificationService.notifyHealthProfessional(patient, alert);
        }
    }
}
```

---

## 📊 Relatórios

### **Relatório Semanal:**

```java
@Service
public class ReportService {
    
    public WeeklyReportDTO generateWeeklyReport(UUID patientId) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        
        List<HealthLog> logs = healthLogRepository
            .findByPatientAndCreatedAtAfter(patientId, weekAgo);
        
        return WeeklyReportDTO.builder()
            .patientId(patientId)
            .period("Últimos 7 dias")
            .totalEntries(logs.size())
            .averagePain(calculateAverage(logs, HealthLog::getPainLevel))
            .mostCommonMood(findMostCommon(logs, HealthLog::getMood))
            .sleepQualityDistribution(calculateDistribution(logs, HealthLog::getSleepQuality))
            .medicationAdherence(calculateMedicationAdherence(logs))
            .alerts(alertRepository.findRecentByPatient(patientId, weekAgo))
            .build();
    }
}
```

---

## 🎯 Próximos Passos

1. 💬 [WhatsApp Integration](../07-whatsapp/01-evolution-api-setup.md)
2. 🔔 [Check-ins Proativos](../08-checkins/01-proactive-checkins.md)
3. 📊 [Analytics](../09-analytics/01-health-analytics.md)

---

[⬅️ Anterior: Prompts](03-prompts.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: WhatsApp Integration](../07-whatsapp/01-evolution-api-setup.md)

