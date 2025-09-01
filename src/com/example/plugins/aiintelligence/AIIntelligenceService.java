package com.example.plugins.aiintelligence;

import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;

import com.example.plugins.aiintelligence.provider.*;
import com.example.plugins.aiintelligence.domain.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.ArrayList;

/**
 * AI Intelligence Smart Service for Appian
 * Uses configurable AI providers to intelligently match column headers using existing mappings as learning data
 */
@PaletteInfo(paletteCategory = "AI & Machine Learning", palette = "AI Intelligence Service")
public class AIIntelligenceService extends AppianSmartService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIIntelligenceService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Provider Configuration
    private String providerId;
    private String providerName;
    
    // Model Configuration
    private String modelId;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Integer topK;
    
    // Provider-specific parameters (stored as key-value pairs)
    private String[] providerParameterKeys;
    private String[] providerParameterValues;
    
    // Column Matching Input parameters
    private String[] targetHeaders;
    private String[] sourceHeaders;
    private String existingMappingsJson;  // Changed from ColumnMapping[] to JSON string
    private String industryContext;
    
    // Output parameters
    private String errorMessage;
    private Boolean success;
    
    // Column Matching Output parameters
    private String columnMatchingResultsJson;  // Changed from ColumnMatchingResult[] to JSON string
    private Integer matchedHeadersCount;
    private Double averageConfidence;
    private Integer existingMappingsUsedCount;
    private Double existingMappingUtilizationRate;
    private String rawResponse;
    private String usedProviderName;
    
    @Override
    public void run() throws SmartServiceException {
        try {
            logger.info("Starting AI Intelligence service execution with provider: {}", providerId);
            
            // Validate inputs
            validateInputs();
            
            // Create provider configuration
            ProviderConfiguration providerConfig = createProviderConfiguration();
            
            // Create model configuration
            ModelConfiguration modelConfig = createModelConfiguration();
            
            // Get AI provider
            AIProvider aiProvider = AIProviderFactory.createProvider(providerId, providerConfig);
            
            // Convert existing mappings to list
            List<ColumnMapping> mappingsList = parseExistingMappings();
            
            // Perform column matching
            List<ColumnMatchingResult> results = aiProvider.performColumnMatching(
                sourceHeaders, targetHeaders, mappingsList, industryContext, modelConfig);
            
            // Process results
            processResults(results);
            
            // Set success output
            this.success = true;
            this.errorMessage = null;
            this.usedProviderName = aiProvider.getProviderName();
            
            logger.info("AI Intelligence service execution completed successfully");
            
        } catch (Exception e) {
            logger.error("Error in AI Intelligence service execution", e);
            this.success = false;
            this.errorMessage = e.getMessage();
            // Clear column matching results on error
            clearResults();
            throw new SmartServiceException(AIIntelligenceService.class, e, e.getMessage());
        }
    }
    
    /**
     * Validates required inputs
     */
    private void validateInputs() throws Exception {
        if (providerId == null || providerId.trim().isEmpty()) {
            throw new Exception("Provider ID is required");
        }
        
        if (modelId == null || modelId.trim().isEmpty()) {
            throw new Exception("Model ID is required");
        }
        
        if (targetHeaders == null || targetHeaders.length == 0) {
            throw new Exception("Target headers are required");
        }
        
        if (sourceHeaders == null || sourceHeaders.length == 0) {
            throw new Exception("Source headers are required");
        }
        
        // Validate provider parameter arrays
        if (providerParameterKeys != null && providerParameterValues != null) {
            if (providerParameterKeys.length != providerParameterValues.length) {
                throw new Exception("Provider parameter keys and values arrays must have the same length");
            }
        }
    }
    
    /**
     * Creates provider configuration from input parameters
     */
    private ProviderConfiguration createProviderConfiguration() {
        ProviderConfiguration config = new ProviderConfiguration(providerId, providerName);
        
        // Add provider-specific parameters
        if (providerParameterKeys != null && providerParameterValues != null) {
            for (int i = 0; i < providerParameterKeys.length; i++) {
                if (providerParameterKeys[i] != null && providerParameterValues[i] != null) {
                    config.setParameter(providerParameterKeys[i], providerParameterValues[i]);
                }
            }
        }
        
        return config;
    }
    
    /**
     * Creates model configuration from input parameters
     */
    private ModelConfiguration createModelConfiguration() {
        return new ModelConfiguration(modelId, temperature, maxTokens, topP, topK);
    }
    
    /**
     * Parses existing mappings from JSON string
     */
    private List<ColumnMapping> parseExistingMappings() throws Exception {
        List<ColumnMapping> list = new ArrayList<>();
        if (existingMappingsJson != null && !existingMappingsJson.trim().isEmpty()) {
            try {
                ColumnMapping[] mappings = objectMapper.readValue(existingMappingsJson, ColumnMapping[].class);
                for (ColumnMapping mapping : mappings) {
                    if (mapping != null && mapping.isValid()) {
                        list.add(mapping);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to parse existing mappings JSON: {}", e.getMessage());
                // Continue with empty list if JSON parsing fails
            }
        }
        return list;
    }
    
    /**
     * Processes the column matching results
     */
    private void processResults(List<ColumnMatchingResult> results) throws Exception {
        // Convert to JSON string for Appian
        this.columnMatchingResultsJson = objectMapper.writeValueAsString(results);
        
        // Calculate statistics
        this.matchedHeadersCount = results.size();
        
        double totalConfidence = 0.0;
        int existingMappingsUsedCount = 0;
        
        for (ColumnMatchingResult result : results) {
            if (result.getConfidencePercentage() != null) {
                totalConfidence += result.getConfidencePercentage();
            }
            if (result.getUsedExistingMapping() != null && result.getUsedExistingMapping()) {
                existingMappingsUsedCount++;
            }
        }
        
        this.averageConfidence = results.size() > 0 ? totalConfidence / results.size() : 0.0;
        this.existingMappingsUsedCount = existingMappingsUsedCount;
        this.existingMappingUtilizationRate = results.size() > 0 ? 
            (double) existingMappingsUsedCount / results.size() * 100 : 0.0;
    }
    
    /**
     * Clears all results on error
     */
    private void clearResults() {
        this.columnMatchingResultsJson = null;
        this.matchedHeadersCount = null;
        this.averageConfidence = null;
        this.existingMappingsUsedCount = null;
        this.existingMappingUtilizationRate = null;
        this.rawResponse = null;
        this.usedProviderName = null;
    }
    
    // Input setters with annotations
    
    @Input(required = Required.ALWAYS)
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
    @Input(required = Required.ALWAYS)
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setTopP(Double topP) {
        this.topP = topP;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setTopK(Integer topK) {
        this.topK = topK;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setProviderParameterKeys(String[] providerParameterKeys) {
        this.providerParameterKeys = providerParameterKeys;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setProviderParameterValues(String[] providerParameterValues) {
        this.providerParameterValues = providerParameterValues;
    }
    
    @Input(required = Required.ALWAYS)
    public void setTargetHeaders(String[] targetHeaders) {
        this.targetHeaders = targetHeaders;
    }
    
    @Input(required = Required.ALWAYS)
    public void setSourceHeaders(String[] sourceHeaders) {
        this.sourceHeaders = sourceHeaders;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setExistingMappingsJson(String existingMappingsJson) {
        this.existingMappingsJson = existingMappingsJson;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setIndustryContext(String industryContext) {
        this.industryContext = industryContext;
    }
    
    // Output getters
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public String getRawResponse() {
        return rawResponse;
    }
    
    public String getUsedProviderName() {
        return usedProviderName;
    }
    
    // Column Matching Output getters
    
    public String getColumnMatchingResultsJson() {
        return columnMatchingResultsJson;
    }
    
    public Integer getMatchedHeadersCount() {
        return matchedHeadersCount;
    }
    
    public Double getAverageConfidence() {
        return averageConfidence;
    }
    
    public Integer getExistingMappingsUsedCount() {
        return existingMappingsUsedCount;
    }
    
    public Double getExistingMappingUtilizationRate() {
        return existingMappingUtilizationRate;
    }
}
