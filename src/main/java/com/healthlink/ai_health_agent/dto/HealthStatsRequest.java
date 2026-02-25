package com.healthlink.ai_health_agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para Function Calling - Extração de dados de saúde pela IA
 * 
 * A IA usa este schema para estruturar dados não estruturados da conversa
 * 
 * Exemplo de uso pela IA:
 * Paciente: "Estou com dor 8 hoje, não dormi bem e esqueci de tomar o remédio"
 * IA chama: recordDailyHealthStats(painLevel=8, sleepQuality="ruim", medicationTaken=false)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatsRequest {

    /**
     * Nível de dor relatado pelo paciente (0-10)
     * 0 = sem dor, 10 = dor insuportável
     */
    @JsonProperty(required = false)
    @JsonPropertyDescription("Pain level from 0 to 10, where 0 is no pain and 10 is unbearable pain")
    private Integer painLevel;

    /**
     * Humor/estado emocional do paciente
     * Valores: "bem", "ansioso", "triste", "irritado", "deprimido", "feliz"
     */
    @JsonProperty(required = false)
    @JsonPropertyDescription("Patient's mood: 'bem' (good), 'ansioso' (anxious), 'triste' (sad), 'irritado' (irritated), 'deprimido' (depressed), 'feliz' (happy)")
    private String mood;

    /**
     * Qualidade do sono
     * Valores: "ótimo", "bom", "regular", "ruim", "péssimo"
     */
    @JsonProperty(required = false)
    @JsonPropertyDescription("Sleep quality: 'ótimo' (excellent), 'bom' (good), 'regular' (fair), 'ruim' (poor), 'péssimo' (terrible)")
    private String sleepQuality;

    /**
     * Horas de sono (opcional)
     */
    @JsonProperty(required = false)
    @JsonPropertyDescription("Number of hours slept")
    private Double sleepHours;

    /**
     * Se o paciente tomou a medicação
     */
    @JsonProperty(required = false)
    @JsonPropertyDescription("Whether the patient took their medication (true/false)")
    private Boolean medicationTaken;

    /**
     * Nome da medicação (se mencionado)
     */
    @JsonProperty(required = false)
    @JsonPropertyDescription("Name of the medication mentioned")
    private String medicationName;

    /**
     * Nível de energia (0-10)
     */
    @JsonProperty(required = false)
    @JsonPropertyDescription("Energy level from 0 to 10, where 0 is no energy and 10 is very energetic")
    private Integer energyLevel;

    /**
     * Nível de estresse (0-10)
     */
    @JsonProperty(required = false)
    @JsonPropertyDescription("Stress level from 0 to 10, where 0 is no stress and 10 is extreme stress")
    private Integer stressLevel;

    /**
     * Observações adicionais extraídas da conversa
     */
    @JsonProperty(required = false)
    @JsonPropertyDescription("Additional notes or observations from the conversation")
    private String notes;

    /**
     * Valida se há pelo menos um dado preenchido
     */
    public boolean hasAnyData() {
        return painLevel != null || 
               mood != null || 
               sleepQuality != null || 
               sleepHours != null ||
               medicationTaken != null || 
               medicationName != null ||
               energyLevel != null ||
               stressLevel != null ||
               (notes != null && !notes.isBlank());
    }

    /**
     * Retorna resumo dos dados para log
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (painLevel != null) {
            summary.append("Dor: ").append(painLevel).append("/10");
        }
        
        if (mood != null) {
            if (summary.length() > 0) summary.append(", ");
            summary.append("Humor: ").append(mood);
        }
        
        if (sleepQuality != null) {
            if (summary.length() > 0) summary.append(", ");
            summary.append("Sono: ").append(sleepQuality);
        }
        
        if (medicationTaken != null) {
            if (summary.length() > 0) summary.append(", ");
            summary.append("Medicação: ").append(medicationTaken ? "Sim" : "Não");
        }
        
        return summary.toString();
    }
}

