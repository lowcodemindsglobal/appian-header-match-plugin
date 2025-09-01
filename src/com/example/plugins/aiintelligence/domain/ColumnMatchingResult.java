package com.example.plugins.aiintelligence.domain;

/**
 * Represents the result of matching a source column header to a target column header.
 * Contains confidence scoring, reasoning, and reference mapping usage information.
 */
public class ColumnMatchingResult {
    
    private String sourceHeader;
    private String matchedTargetHeader;
    private Double confidencePercentage;
    private String reasoning;
    private Boolean usedExistingMapping;
    
    /**
     * Default constructor for JSON deserialization
     */
    public ColumnMatchingResult() {
    }
    
    /**
     * Constructor for creating a complete column matching result
     * 
     * @param sourceHeader The original source header
     * @param matchedTargetHeader The matched target header
     * @param confidencePercentage Confidence score (0-100)
     * @param reasoning Explanation of the matching logic
     * @param usedExistingMapping Whether an existing mapping was used
     */
    public ColumnMatchingResult(String sourceHeader, String matchedTargetHeader, 
                               Double confidencePercentage, String reasoning, 
                               Boolean usedExistingMapping) {
        this.sourceHeader = sourceHeader;
        this.matchedTargetHeader = matchedTargetHeader;
        this.confidencePercentage = confidencePercentage;
        this.reasoning = reasoning;
        this.usedExistingMapping = usedExistingMapping;
    }
    
    // Getters and Setters
    
    public String getSourceHeader() {
        return sourceHeader;
    }
    
    public void setSourceHeader(String sourceHeader) {
        this.sourceHeader = sourceHeader;
    }
    
    public String getMatchedTargetHeader() {
        return matchedTargetHeader;
    }
    
    public void setMatchedTargetHeader(String matchedTargetHeader) {
        this.matchedTargetHeader = matchedTargetHeader;
    }
    
    public Double getConfidencePercentage() {
        return confidencePercentage;
    }
    
    public void setConfidencePercentage(Double confidencePercentage) {
        this.confidencePercentage = confidencePercentage;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    public Boolean getUsedExistingMapping() {
        return usedExistingMapping;
    }
    
    public void setUsedExistingMapping(Boolean usedExistingMapping) {
        this.usedExistingMapping = usedExistingMapping;
    }
    
    @Override
    public String toString() {
        return String.format("ColumnMatchingResult{sourceHeader='%s', matchedTargetHeader='%s', " +
                           "confidencePercentage=%.1f, usedExistingMapping=%s, reasoning='%s'}", 
                           sourceHeader, matchedTargetHeader, confidencePercentage, 
                           usedExistingMapping, reasoning);
    }
    
    /**
     * Validates that this result contains all required fields
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return sourceHeader != null && !sourceHeader.trim().isEmpty() &&
               matchedTargetHeader != null && !matchedTargetHeader.trim().isEmpty() &&
               confidencePercentage != null && confidencePercentage >= 0 && confidencePercentage <= 100 &&
               reasoning != null && !reasoning.trim().isEmpty() &&
               usedExistingMapping != null;
    }
}
