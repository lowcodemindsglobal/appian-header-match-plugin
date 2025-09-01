package com.example.plugins.aiintelligence.provider;

/**
 * Configuration for AI model parameters that are common across different providers.
 * This class encapsulates the standard parameters used to control AI model behavior.
 */
public class ModelConfiguration {
    
    private String modelId;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Integer topK;
    
    /**
     * Default constructor
     */
    public ModelConfiguration() {
        // Set sensible defaults
        this.temperature = 0.3;
        this.maxTokens = 4000;
        this.topP = 1.0;
        this.topK = 50;
    }
    
    /**
     * Constructor with required model ID
     * 
     * @param modelId The AI model identifier
     */
    public ModelConfiguration(String modelId) {
        this();
        this.modelId = modelId;
    }
    
    /**
     * Full constructor
     * 
     * @param modelId The AI model identifier
     * @param temperature Controls randomness (0.0 = deterministic, 1.0 = very random)
     * @param maxTokens Maximum number of tokens to generate
     * @param topP Nucleus sampling parameter (0.0 to 1.0)
     * @param topK Top-k sampling parameter
     */
    public ModelConfiguration(String modelId, Double temperature, Integer maxTokens, 
                           Double topP, Integer topK) {
        this.modelId = modelId;
        this.temperature = temperature != null ? temperature : 0.3;
        this.maxTokens = maxTokens != null ? maxTokens : 4000;
        this.topP = topP != null ? topP : 1.0;
        this.topK = topK != null ? topK : 50;
    }
    
    // Getters and Setters
    
    public String getModelId() {
        return modelId;
    }
    
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature != null ? temperature : 0.3;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens != null ? maxTokens : 4000;
    }
    
    public Double getTopP() {
        return topP;
    }
    
    public void setTopP(Double topP) {
        this.topP = topP != null ? topP : 1.0;
    }
    
    public Integer getTopK() {
        return topK;
    }
    
    public void setTopK(Integer topK) {
        this.topK = topK != null ? topK : 50;
    }
    
    /**
     * Validates the configuration
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return modelId != null && !modelId.trim().isEmpty() &&
               temperature >= 0.0 && temperature <= 2.0 &&
               maxTokens > 0 && maxTokens <= 100000 &&
               topP >= 0.0 && topP <= 1.0 &&
               topK > 0;
    }
    
    @Override
    public String toString() {
        return String.format("ModelConfiguration{modelId='%s', temperature=%.2f, maxTokens=%d, topP=%.2f, topK=%d}", 
                           modelId, temperature, maxTokens, topP, topK);
    }
}
