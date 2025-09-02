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
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
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
    
    // AWS Credentials (for AWS Bedrock provider)
    private String accessKeyId;
    private String secretAccessKey;
    
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
            
            // Provide more user-friendly error messages
            String errorMsg = e.getMessage();
            if (e.getMessage() != null && e.getMessage().contains("credentials")) {
                errorMsg = "AWS credentials not configured. Please set up AWS credentials using environment variables, system properties, or AWS credentials file.";
            } else if (e.getMessage() != null && e.getMessage().contains("region")) {
                errorMsg = "Invalid AWS region specified. Please check the region parameter.";
            } else if (e.getMessage() != null && e.getMessage().contains("JSON")) {
                errorMsg = "Invalid JSON format in existing mappings. Please check the JSON structure.";
            }
            
            this.errorMessage = errorMsg;
            // Clear column matching results on error
            clearResults();
            throw new SmartServiceException(AIIntelligenceService.class, e, errorMsg);
        }
    }
    
    /**
     * Validates required inputs
     */
    private void validateInputs() throws Exception {
        logger.info("Validating input parameters");
        
        if (providerId == null || providerId.trim().isEmpty()) {
            logger.error("Provider ID is required but not provided");
            throw new Exception("Provider ID is required");
        }
        logger.debug("Provider ID validation passed: {}", providerId);
        
        if (modelId == null || modelId.trim().isEmpty()) {
            logger.error("Model ID is required but not provided");
            throw new Exception("Model ID is required");
        }
        logger.debug("Model ID validation passed: {}", modelId);
        
        if (targetHeaders == null || targetHeaders.length == 0) {
            logger.error("Target headers are required but not provided");
            throw new Exception("Target headers are required");
        }
        logger.debug("Target headers validation passed: {} headers", targetHeaders.length);
        
        if (sourceHeaders == null || sourceHeaders.length == 0) {
            logger.error("Source headers are required but not provided");
            throw new Exception("Source headers are required");
        }
        logger.debug("Source headers validation passed: {} headers", sourceHeaders.length);
        
        // Validate AWS credentials for AWS Bedrock provider
        if ("aws-bedrock".equalsIgnoreCase(providerId)) {
            if (accessKeyId == null || accessKeyId.trim().isEmpty()) {
                logger.warn("AWS Access Key ID not provided for AWS Bedrock provider - will use default AWS credentials chain");
            }
            if (secretAccessKey == null || secretAccessKey.trim().isEmpty()) {
                logger.warn("AWS Secret Access Key not provided for AWS Bedrock provider - will use default AWS credentials chain");
            }
            if ((accessKeyId != null && !accessKeyId.trim().isEmpty()) && (secretAccessKey == null || secretAccessKey.trim().isEmpty())) {
                logger.error("AWS Secret Access Key is required when Access Key ID is provided");
                throw new Exception("AWS Secret Access Key is required when Access Key ID is provided");
            }
            if ((secretAccessKey != null && !secretAccessKey.trim().isEmpty()) && (accessKeyId == null || accessKeyId.trim().isEmpty())) {
                logger.error("AWS Access Key ID is required when Secret Access Key is provided");
                throw new Exception("AWS Access Key ID is required when Secret Access Key is provided");
            }
            logger.debug("AWS credentials validation passed for AWS Bedrock provider");
        }
        
        // Validate provider parameter arrays
        if (providerParameterKeys != null && providerParameterValues != null) {
            if (providerParameterKeys.length != providerParameterValues.length) {
                logger.error("Provider parameter arrays length mismatch: keys={}, values={}", 
                           providerParameterKeys.length, providerParameterValues.length);
                throw new Exception("Provider parameter keys and values arrays must have the same length");
            }
            logger.debug("Provider parameter arrays validation passed: {} parameters", providerParameterKeys.length);
        } else {
            logger.debug("No provider parameters provided");
        }
        
        logger.info("Input validation completed successfully");
    }
    
    /**
     * Creates provider configuration from input parameters
     */
    private ProviderConfiguration createProviderConfiguration() {
        logger.info("Creating provider configuration for provider: {}", providerId);
        logger.info("Provider parameter keys: {}", providerParameterKeys != null ? java.util.Arrays.toString(providerParameterKeys) : "null");
        logger.info("Provider parameter values: {}", providerParameterValues != null ? java.util.Arrays.toString(providerParameterValues) : "null");
        
        // Set default provider name if not provided
        String effectiveProviderName = providerName;
        if (effectiveProviderName == null || effectiveProviderName.trim().isEmpty()) {
            effectiveProviderName = providerId; // Use provider ID as default name
        }
        
        ProviderConfiguration config = new ProviderConfiguration(providerId, effectiveProviderName);
        
        // Add AWS credentials if provided
        if (accessKeyId != null && !accessKeyId.trim().isEmpty()) {
            config.setParameter("accessKeyId", accessKeyId);
            logger.info("Added AWS access key ID");
        }
        
        if (secretAccessKey != null && !secretAccessKey.trim().isEmpty()) {
            config.setParameter("secretAccessKey", secretAccessKey);
            logger.info("Added AWS secret access key");
        }
        
        // Add provider-specific parameters
        if (providerParameterKeys != null && providerParameterValues != null) {
            logger.info("Adding {} provider-specific parameters", providerParameterKeys.length);
            for (int i = 0; i < providerParameterKeys.length; i++) {
                if (providerParameterKeys[i] != null && providerParameterValues[i] != null) {
                    config.setParameter(providerParameterKeys[i], providerParameterValues[i]);
                    if (providerParameterKeys[i].toLowerCase().contains("key") || providerParameterKeys[i].toLowerCase().contains("secret")) {
                        logger.info("Added parameter: {} = ***", providerParameterKeys[i]);
                    } else {
                        logger.info("Added parameter: {} = {}", providerParameterKeys[i], providerParameterValues[i]);
                    }
                } else {
                    logger.warn("Skipping null parameter at index {}: key={}, value={}", 
                               i, providerParameterKeys[i], providerParameterValues[i]);
                }
            }
        } else {
            logger.warn("No provider-specific parameters provided - keys: {}, values: {}", 
                       providerParameterKeys != null, providerParameterValues != null);
        }
        
        logger.info("Provider configuration created successfully with {} parameters", config.getAllParameters().size());
        return config;
    }
    
    /**
     * Creates model configuration from input parameters
     */
    private ModelConfiguration createModelConfiguration() {
        logger.info("Creating model configuration for model: {}", modelId);
        ModelConfiguration config = new ModelConfiguration(modelId, temperature, maxTokens, topP, topK);
        logger.info("Model configuration created successfully: temperature={}, maxTokens={}, topP={}, topK={}", 
                   config.getTemperature(), config.getMaxTokens(), config.getTopP(), config.getTopK());
        return config;
    }
    
    /**
     * Parses existing mappings from JSON string
     */
    private List<ColumnMapping> parseExistingMappings() throws Exception {
        logger.info("Parsing existing mappings from JSON");
        List<ColumnMapping> list = new ArrayList<>();
        
        if (existingMappingsJson != null && !existingMappingsJson.trim().isEmpty()) {
            logger.debug("Existing mappings JSON length: {} characters", existingMappingsJson.length());
            try {
                // First try to parse as ColumnMapping array
                try {
                    ColumnMapping[] mappings = objectMapper.readValue(existingMappingsJson, ColumnMapping[].class);
                    logger.debug("Parsed {} mappings from JSON as ColumnMapping array", mappings.length);
                    
                    for (ColumnMapping mapping : mappings) {
                        if (mapping != null && mapping.isValid()) {
                            list.add(mapping);
                            logger.debug("Added valid mapping: {} -> {}", mapping.getSourceColumn(), mapping.getTargetColumn());
                        } else {
                            logger.warn("Skipping invalid mapping: {}", mapping);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Failed to parse as ColumnMapping array, trying as string array: {}", e.getMessage());
                    
                    // If that fails, try to parse as string array and convert to ColumnMapping
                    String[] stringArray = objectMapper.readValue(existingMappingsJson, String[].class);
                    logger.debug("Parsed {} strings from JSON as string array", stringArray.length);
                    
                    for (String str : stringArray) {
                        if (str != null && !str.trim().isEmpty()) {
                            // Create a ColumnMapping with the string as both source and target
                            // This assumes the string represents a column name that maps to itself
                            ColumnMapping mapping = new ColumnMapping(str.trim(), str.trim());
                            list.add(mapping);
                            logger.debug("Added string-based mapping: {} -> {}", str.trim(), str.trim());
                        }
                    }
                }
                
                logger.info("Successfully parsed {} valid mappings from existing mappings JSON", list.size());
            } catch (Exception e) {
                logger.error("Failed to parse existing mappings JSON: {}", e.getMessage(), e);
                // Continue with empty list if JSON parsing fails
            }
        } else {
            logger.debug("No existing mappings JSON provided");
        }
        
        logger.info("Existing mappings parsing completed: {} valid mappings found", list.size());
        return list;
    }
    
    /**
     * Processes the column matching results
     */
    private void processResults(List<ColumnMatchingResult> results) throws Exception {
        logger.info("Processing {} column matching results", results.size());
        
        // Convert to JSON string for Appian
        logger.debug("Converting results to JSON string");
        this.columnMatchingResultsJson = objectMapper.writeValueAsString(results);
        logger.debug("Results JSON length: {} characters", this.columnMatchingResultsJson.length());
        
        // Calculate statistics
        this.matchedHeadersCount = results.size();
        logger.debug("Matched headers count: {}", this.matchedHeadersCount);
        
        double totalConfidence = 0.0;
        int existingMappingsUsedCount = 0;
        int validResultsCount = 0;
        
        for (ColumnMatchingResult result : results) {
            if (result.getConfidencePercentage() != null) {
                totalConfidence += result.getConfidencePercentage();
                validResultsCount++;
            }
            if (result.getUsedExistingMapping() != null && result.getUsedExistingMapping()) {
                existingMappingsUsedCount++;
            }
        }
        
        this.averageConfidence = validResultsCount > 0 ? totalConfidence / validResultsCount : 0.0;
        this.existingMappingsUsedCount = existingMappingsUsedCount;
        this.existingMappingUtilizationRate = results.size() > 0 ? 
            (double) existingMappingsUsedCount / results.size() * 100 : 0.0;
        
        logger.info("Results processing completed - Average confidence: {:.2f}%, Existing mappings used: {}/{}, Utilization rate: {:.2f}%", 
                   this.averageConfidence, this.existingMappingsUsedCount, this.matchedHeadersCount, this.existingMappingUtilizationRate);
    }
    
    /**
     * Clears all results on error
     */
    private void clearResults() {
        logger.info("Clearing all results due to error");
        this.columnMatchingResultsJson = null;
        this.matchedHeadersCount = null;
        this.averageConfidence = null;
        this.existingMappingsUsedCount = null;
        this.existingMappingUtilizationRate = null;
        this.rawResponse = null;
        this.usedProviderName = null;
        logger.debug("All results cleared");
    }
    
    // Input setters with annotations
    
    @Input(required = Required.ALWAYS)
    public void setProviderId(String providerId) {
        logger.debug("Setting providerId: {}", providerId);
        this.providerId = providerId;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setProviderName(String providerName) {
        logger.debug("Setting providerName: {}", providerName);
        this.providerName = providerName;
    }
    
    @Input(required = Required.ALWAYS)
    public void setModelId(String modelId) {
        logger.debug("Setting modelId: {}", modelId);
        this.modelId = modelId;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setTemperature(Double temperature) {
        logger.debug("Setting temperature: {}", temperature);
        this.temperature = temperature;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setMaxTokens(Integer maxTokens) {
        logger.debug("Setting maxTokens: {}", maxTokens);
        this.maxTokens = maxTokens;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setTopP(Double topP) {
        logger.debug("Setting topP: {}", topP);
        this.topP = topP;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setTopK(Integer topK) {
        logger.debug("Setting topK: {}", topK);
        this.topK = topK;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setProviderParameterKeys(String[] providerParameterKeys) {
        logger.debug("Setting providerParameterKeys: {} keys", providerParameterKeys != null ? providerParameterKeys.length : 0);
        this.providerParameterKeys = providerParameterKeys;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setProviderParameterValues(String[] providerParameterValues) {
        logger.debug("Setting providerParameterValues: {} values", providerParameterValues != null ? providerParameterValues.length : 0);
        this.providerParameterValues = providerParameterValues;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setAccessKeyId(String accessKeyId) {
        logger.debug("Setting accessKeyId: {}", accessKeyId != null ? "***" : "null");
        this.accessKeyId = accessKeyId;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setSecretAccessKey(String secretAccessKey) {
        logger.debug("Setting secretAccessKey: {}", secretAccessKey != null ? "***" : "null");
        this.secretAccessKey = secretAccessKey;
    }
    
    @Input(required = Required.ALWAYS)
    public void setTargetHeaders(String[] targetHeaders) {
        logger.debug("Setting targetHeaders: {} headers", targetHeaders != null ? targetHeaders.length : 0);
        this.targetHeaders = targetHeaders;
    }
    
    @Input(required = Required.ALWAYS)
    public void setSourceHeaders(String[] sourceHeaders) {
        logger.debug("Setting sourceHeaders: {} headers", sourceHeaders != null ? sourceHeaders.length : 0);
        this.sourceHeaders = sourceHeaders;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setExistingMappingsJson(String existingMappingsJson) {
        logger.debug("Setting existingMappingsJson: {} characters", existingMappingsJson != null ? existingMappingsJson.length() : 0);
        this.existingMappingsJson = existingMappingsJson;
    }
    
    @Input(required = Required.OPTIONAL)
    public void setIndustryContext(String industryContext) {
        logger.debug("Setting industryContext: {}", industryContext);
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
