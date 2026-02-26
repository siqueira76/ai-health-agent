# 9.1 Health Analytics

## 📊 Análise de Dados de Saúde

Transforme dados coletados em insights acionáveis para pacientes e profissionais de saúde.

---

## 🎯 Tipos de Análises

### **1. Tendências de Dor**
- Média de dor ao longo do tempo
- Picos de dor (dias com dor >= 7)
- Padrões (piora em determinados dias/horários)

### **2. Qualidade do Sono**
- Distribuição (ótimo/bom/regular/ruim)
- Correlação entre sono e dor
- Tendência ao longo do tempo

### **3. Estado Emocional**
- Humor predominante
- Dias com humor negativo
- Correlação entre humor e dor

### **4. Adesão a Medicamentos**
- Taxa de adesão (%)
- Medicamentos mais usados
- Esquecimentos

### **5. Gatilhos**
- Gatilhos mais frequentes
- Correlação gatilho → sintoma

---

## 📈 Implementação

### **HealthAnalyticsService.java:**

```java
@Service
@Slf4j
public class HealthAnalyticsService {
    
    private final HealthLogRepository healthLogRepository;
    
    // ========================================
    // ANÁLISE DE DOR
    // ========================================
    
    public PainAnalysisDTO analyzePain(UUID patientId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        List<HealthLog> logs = healthLogRepository
            .findByPatientAndCreatedAtAfter(patientId, since);
        
        List<Integer> painLevels = logs.stream()
            .filter(log -> log.getPainLevel() != null)
            .map(HealthLog::getPainLevel)
            .toList();
        
        if (painLevels.isEmpty()) {
            return PainAnalysisDTO.builder()
                .averagePain(0.0)
                .trend("NO_DATA")
                .build();
        }
        
        double avgPain = painLevels.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0);
        
        int maxPain = painLevels.stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);
        
        int minPain = painLevels.stream()
            .mapToInt(Integer::intValue)
            .min()
            .orElse(0);
        
        long highPainDays = painLevels.stream()
            .filter(pain -> pain >= 7)
            .count();
        
        String trend = calculatePainTrend(logs);
        
        return PainAnalysisDTO.builder()
            .averagePain(avgPain)
            .maxPain(maxPain)
            .minPain(minPain)
            .highPainDays((int) highPainDays)
            .totalDays(days)
            .trend(trend)
            .painByDay(groupPainByDay(logs))
            .build();
    }
    
    private String calculatePainTrend(List<HealthLog> logs) {
        if (logs.size() < 4) return "INSUFFICIENT_DATA";
        
        // Dividir em duas metades
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
        
        if (secondHalf > firstHalf + 1.5) return "WORSENING";
        if (secondHalf < firstHalf - 1.5) return "IMPROVING";
        return "STABLE";
    }
    
    private Map<LocalDate, Double> groupPainByDay(List<HealthLog> logs) {
        return logs.stream()
            .filter(log -> log.getPainLevel() != null)
            .collect(Collectors.groupingBy(
                log -> log.getCreatedAt().toLocalDate(),
                Collectors.averagingInt(HealthLog::getPainLevel)
            ));
    }
    
    // ========================================
    // ANÁLISE DE SONO
    // ========================================
    
    public SleepAnalysisDTO analyzeSleep(UUID patientId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        List<HealthLog> logs = healthLogRepository
            .findByPatientAndCreatedAtAfter(patientId, since);
        
        Map<String, Long> distribution = logs.stream()
            .filter(log -> log.getSleepQuality() != null)
            .collect(Collectors.groupingBy(
                HealthLog::getSleepQuality,
                Collectors.counting()
            ));
        
        long badSleepDays = distribution.getOrDefault("ruim", 0L) + 
                           distribution.getOrDefault("péssimo", 0L);
        
        return SleepAnalysisDTO.builder()
            .distribution(distribution)
            .badSleepDays((int) badSleepDays)
            .totalDays(days)
            .build();
    }
    
    // ========================================
    // ANÁLISE DE HUMOR
    // ========================================
    
    public MoodAnalysisDTO analyzeMood(UUID patientId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        List<HealthLog> logs = healthLogRepository
            .findByPatientAndCreatedAtAfter(patientId, since);
        
        Map<String, Long> distribution = logs.stream()
            .filter(log -> log.getMood() != null)
            .collect(Collectors.groupingBy(
                HealthLog::getMood,
                Collectors.counting()
            ));
        
        String predominantMood = distribution.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");
        
        long negativeMoodDays = logs.stream()
            .filter(log -> isNegativeMood(log.getMood()))
            .count();
        
        return MoodAnalysisDTO.builder()
            .distribution(distribution)
            .predominantMood(predominantMood)
            .negativeMoodDays((int) negativeMoodDays)
            .totalDays(days)
            .build();
    }
    
    private boolean isNegativeMood(String mood) {
        if (mood == null) return false;
        return mood.equalsIgnoreCase("triste") || 
               mood.equalsIgnoreCase("ansioso") ||
               mood.equalsIgnoreCase("irritado");
    }
    
    // ========================================
    // CORRELAÇÕES
    // ========================================
    
    public CorrelationDTO analyzeSleepPainCorrelation(UUID patientId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        List<HealthLog> logs = healthLogRepository
            .findByPatientAndCreatedAtAfter(patientId, since);
        
        // Agrupar por dia
        Map<LocalDate, HealthLog> logsByDay = logs.stream()
            .collect(Collectors.toMap(
                log -> log.getCreatedAt().toLocalDate(),
                log -> log,
                (log1, log2) -> log1 // Se houver múltiplos, pegar o primeiro
            ));
        
        // Calcular correlação: sono ruim → dor alta no dia seguinte
        int badSleepFollowedByPain = 0;
        int totalBadSleep = 0;
        
        List<LocalDate> sortedDates = logsByDay.keySet().stream()
            .sorted()
            .toList();
        
        for (int i = 0; i < sortedDates.size() - 1; i++) {
            LocalDate today = sortedDates.get(i);
            LocalDate tomorrow = sortedDates.get(i + 1);
            
            HealthLog todayLog = logsByDay.get(today);
            HealthLog tomorrowLog = logsByDay.get(tomorrow);
            
            if (isBadSleep(todayLog.getSleepQuality())) {
                totalBadSleep++;
                
                if (tomorrowLog.getPainLevel() != null && 
                    tomorrowLog.getPainLevel() >= 6) {
                    badSleepFollowedByPain++;
                }
            }
        }
        
        double correlation = totalBadSleep > 0 
            ? (badSleepFollowedByPain * 100.0 / totalBadSleep) 
            : 0;
        
        return CorrelationDTO.builder()
            .factor1("Sono ruim")
            .factor2("Dor alta no dia seguinte")
            .correlationPercentage(correlation)
            .sampleSize(totalBadSleep)
            .build();
    }
    
    private boolean isBadSleep(String sleepQuality) {
        if (sleepQuality == null) return false;
        return sleepQuality.equalsIgnoreCase("ruim") || 
               sleepQuality.equalsIgnoreCase("péssimo");
    }
    
    // ========================================
    // DASHBOARD COMPLETO
    // ========================================
    
    public HealthDashboardDTO generateDashboard(UUID patientId, int days) {
        return HealthDashboardDTO.builder()
            .patientId(patientId)
            .period(days + " dias")
            .painAnalysis(analyzePain(patientId, days))
            .sleepAnalysis(analyzeSleep(patientId, days))
            .moodAnalysis(analyzeMood(patientId, days))
            .sleepPainCorrelation(analyzeSleepPainCorrelation(patientId, days))
            .generatedAt(LocalDateTime.now())
            .build();
    }
}
```

---

## 📊 DTOs

### **PainAnalysisDTO.java:**

```java
@Data
@Builder
public class PainAnalysisDTO {
    private Double averagePain;
    private Integer maxPain;
    private Integer minPain;
    private Integer highPainDays;
    private Integer totalDays;
    private String trend; // IMPROVING, STABLE, WORSENING
    private Map<LocalDate, Double> painByDay;
}
```

### **HealthDashboardDTO.java:**

```java
@Data
@Builder
public class HealthDashboardDTO {
    private UUID patientId;
    private String period;
    private PainAnalysisDTO painAnalysis;
    private SleepAnalysisDTO sleepAnalysis;
    private MoodAnalysisDTO moodAnalysis;
    private CorrelationDTO sleepPainCorrelation;
    private LocalDateTime generatedAt;
}
```

---

## 🎯 Próximos Passos

1. 🧪 [Testes](../10-testing/01-unit-tests.md)
2. 🚀 [Deploy](../11-deployment/01-railway-deploy.md)
3. 📚 [Referências](../13-references/01-glossary.md)

---

[⬅️ Anterior: Check-ins Proativos](../08-checkins/01-proactive-checkins.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Testes](../10-testing/01-unit-tests.md)

