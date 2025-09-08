package com.lcm.plugins.aiintelligence.provider;

import com.lcm.plugins.aiintelligence.domain.ColumnMatchingResult;
import com.lcm.plugins.aiintelligence.domain.ColumnMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base class for AI providers that implements common functionality.
 * This class uses the Template Method pattern to define the structure of
 * column matching operations while allowing subclasses to implement
 * provider-specific details.
 */
public abstract class AbstractAIProvider implements AIProvider {
    
    protected static final Logger logger = LoggerFactory.getLogger(AbstractAIProvider.class);
    protected static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    protected ProviderConfiguration configuration;
    protected boolean initialized = false;
    
    @Override
    public boolean isReady() {
        boolean ready = initialized && configuration != null && configuration.isValid();
        logger.debug("Provider {} readiness check: initialized={}, configuration={}, valid={}, ready={}", 
                    getProviderId(), initialized, configuration != null, 
                    configuration != null ? configuration.isValid() : false, ready);
        return ready;
    }
    
    @Override
    public void validateConfiguration(ProviderConfiguration configuration) throws AIProviderException {
        logger.info("Validating configuration for provider: {}", getProviderId());
        
        if (configuration == null) {
            logger.error("Configuration cannot be null for provider: {}", getProviderId());
            throw new AIProviderException("Configuration cannot be null");
        }
        
        if (!configuration.isValid()) {
            logger.error("Configuration is invalid for provider: {}", getProviderId());
            throw new AIProviderException("Configuration is invalid");
        }
        
        logger.debug("Basic configuration validation passed for provider: {}", getProviderId());
        
        // Validate provider-specific configuration
        logger.debug("Performing provider-specific configuration validation");
        validateProviderConfiguration(configuration);
        
        this.configuration = configuration;
        this.initialized = true;
        logger.info("Configuration validation completed successfully for provider: {}", getProviderId());
    }
    
    @Override
    public List<ColumnMatchingResult> performColumnMatching(
            String[] sourceHeaders,
            String[] targetHeaders,
            List<ColumnMapping> existingMappings,
            String industryContext,
            ModelConfiguration modelConfiguration) throws AIProviderException {
        
        try {
            logger.info("Starting column matching with provider: {}", getProviderId());
            
            // Validate inputs
            validateColumnMatchingInputs(sourceHeaders, targetHeaders, existingMappings, modelConfiguration);
            
            // Filter out columns that already have confirmed mappings
            List<String> unmappedHeaders = filterUnmappedHeaders(sourceHeaders, existingMappings);
            logger.info("Found {} unmapped headers out of {} total headers ({} already mapped)", 
                       unmappedHeaders.size(), sourceHeaders.length, sourceHeaders.length - unmappedHeaders.size());
            
            if (unmappedHeaders.isEmpty()) {
                logger.info("All headers already have confirmed mappings, returning existing results");
                return createResultsFromExistingMappings(sourceHeaders, existingMappings);
            }
            
            List<ColumnMatchingResult> allResults = new ArrayList<>();
            
            // Process each unmapped header individually to avoid JSON truncation
            for (int i = 0; i < unmappedHeaders.size(); i++) {
                String sourceHeader = unmappedHeaders.get(i);
                try {
                    logger.debug("Processing unmapped header {} of {}: {}", i + 1, unmappedHeaders.size(), sourceHeader);
                    
                    // Build prompt for single column
                    String prompt = buildSingleColumnMatchingPrompt(sourceHeader, targetHeaders, existingMappings, industryContext);
                    logger.debug("Built prompt for header '{}', length: {} characters", sourceHeader, prompt.length());
                    
                    // Send request to AI provider
                    String response = sendAIRequest(prompt, modelConfiguration);
                    logger.debug("Received response for header '{}', length: {} characters", sourceHeader, response.length());
                    
                    // Parse the single result
                    ColumnMatchingResult result = parseSingleColumnResponse(response, sourceHeader);
                    allResults.add(result);
                    
                    logger.debug("Successfully processed: {} -> {} (confidence: {}%)", 
                               sourceHeader, result.getMatchedTargetHeader(), result.getConfidencePercentage());
                    
                } catch (Exception e) {
                    logger.warn("Failed to process unmapped header '{}', using default result: {}", sourceHeader, e.getMessage());
                    logger.debug("Exception details for header '{}':", sourceHeader, e);
                    
                    // Create a default result for failed columns
                    ColumnMatchingResult defaultResult = new ColumnMatchingResult(
                        sourceHeader, "", 0.0, "Processing failed: " + e.getMessage(), false);
                    allResults.add(defaultResult);
                }
            }
            
            // Add existing confirmed mappings to results
            allResults.addAll(createResultsFromExistingMappings(sourceHeaders, existingMappings));
            
            // Post-process results
            postProcessResults(allResults, sourceHeaders, targetHeaders, existingMappings);
            
            logger.info("Column matching completed successfully. Found {} total matches ({} new + {} existing)", 
                       allResults.size(), unmappedHeaders.size(), sourceHeaders.length - unmappedHeaders.size());
            return allResults;
            
        } catch (Exception e) {
            String errorMsg = "Column matching failed";
            logger.error("{} for provider: {}", errorMsg, getProviderId(), e);
            logger.debug("Column matching failure details - Source headers: {}, Target headers: {}, Existing mappings: {}", 
                        sourceHeaders != null ? sourceHeaders.length : 0, 
                        targetHeaders != null ? targetHeaders.length : 0,
                        existingMappings != null ? existingMappings.size() : 0);
            throw new AIProviderException(errorMsg, getProviderId(), "column_matching", e);
        }
    }
    
    /**
     * Validates provider-specific configuration
     * 
     * @param configuration The configuration to validate
     * @throws AIProviderException if the configuration is invalid
     */
    protected abstract void validateProviderConfiguration(ProviderConfiguration configuration) 
            throws AIProviderException;
    
    /**
     * Sends a request to the AI provider
     * 
     * @param prompt The prompt to send
     * @param modelConfiguration The model configuration
     * @return The AI response
     * @throws AIProviderException if the request fails
     */
    protected abstract String sendAIRequest(String prompt, ModelConfiguration modelConfiguration) 
            throws AIProviderException;
    
    /**
     * Validates column matching inputs
     */
    private void validateColumnMatchingInputs(String[] sourceHeaders, String[] targetHeaders, 
                                           List<ColumnMapping> existingMappings, 
                                           ModelConfiguration modelConfiguration) throws AIProviderException {
        
        if (sourceHeaders == null || sourceHeaders.length == 0) {
            throw new AIProviderException("Source headers cannot be null or empty");
        }
        
        if (targetHeaders == null || targetHeaders.length == 0) {
            throw new AIProviderException("Target headers cannot be null or empty");
        }
        
        if (modelConfiguration == null || !modelConfiguration.isValid()) {
            throw new AIProviderException("Model configuration is invalid");
        }
        
        // Check if the model is supported
        if (!getSupportedModels().contains(modelConfiguration.getModelId())) {
            throw new AIProviderException("Model not supported: " + modelConfiguration.getModelId());
        }
    }
    
    /**
     * Filters out headers that already have confirmed mappings
     */
    private List<String> filterUnmappedHeaders(String[] sourceHeaders, List<ColumnMapping> existingMappings) {
        List<String> unmappedHeaders = new ArrayList<>();
        
        if (existingMappings == null || existingMappings.isEmpty()) {
            // No existing mappings, all headers need processing
            for (String header : sourceHeaders) {
                unmappedHeaders.add(header);
            }
            return unmappedHeaders;
        }
        
        // Create a set of already mapped source headers for efficient lookup
        Set<String> mappedHeaders = existingMappings.stream()
            .map(ColumnMapping::getSourceColumn)
            .collect(Collectors.toSet());
        
        // Add only unmapped headers
        for (String header : sourceHeaders) {
            if (!mappedHeaders.contains(header)) {
                unmappedHeaders.add(header);
            }
        }
        
        return unmappedHeaders;
    }
    
    /**
     * Creates results from existing confirmed mappings
     */
    private List<ColumnMatchingResult> createResultsFromExistingMappings(String[] sourceHeaders, 
                                                                       List<ColumnMapping> existingMappings) {
        List<ColumnMatchingResult> existingResults = new ArrayList<>();
        
        if (existingMappings == null || existingMappings.isEmpty()) {
            return existingResults;
        }
        
        // Create a map for efficient lookup
        Map<String, ColumnMapping> mappingMap = existingMappings.stream()
            .collect(Collectors.toMap(ColumnMapping::getSourceColumn, mapping -> mapping));
        
        // Create results for all source headers, using existing mappings where available
        for (String sourceHeader : sourceHeaders) {
            ColumnMapping existingMapping = mappingMap.get(sourceHeader);
            if (existingMapping != null) {
                // Create result from existing confirmed mapping
                ColumnMatchingResult result = new ColumnMatchingResult(
                    existingMapping.getSourceColumn(),
                    existingMapping.getTargetColumn(),
                    100.0, // 100% confidence for confirmed mappings
                    "Confirmed existing mapping",
                    true
                );
                existingResults.add(result);
                logger.debug("Using confirmed mapping: {} -> {}", 
                           existingMapping.getSourceColumn(), existingMapping.getTargetColumn());
            }
        }
        
        return existingResults;
    }
    
    /**
     * Builds the column matching prompt for a single source header
     */
    private String buildSingleColumnMatchingPrompt(String sourceHeader, String[] targetHeaders, 
                                                List<ColumnMapping> existingMappings, String industryContext) {
        
        logger.debug("Building prompt for source header: '{}' with {} target headers and {} existing mappings", 
                    sourceHeader, targetHeaders.length, existingMappings != null ? existingMappings.size() : 0);
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("CRITICAL INSTRUCTION: You are a JSON API. You must respond with ONLY valid JSON. No markdown, no explanations, no additional text.\n\n");
        prompt.append("TASK: Match ONE source header to the most appropriate target header using the following approach:\n");
        prompt.append("1. EXACT matches from existing mappings (100% confidence)\n");
        prompt.append("2. PATTERN-based matches learned from existing mappings (high confidence)\n");
        prompt.append("3. SEMANTIC similarity and business logic (variable confidence)\n");
        prompt.append("4. Common abbreviations and naming conventions\n\n");
        
        // Add existing mappings context if available
        if (existingMappings != null && !existingMappings.isEmpty()) {
            prompt.append("EXISTING MAPPINGS (Learn from these patterns):\n");
            for (ColumnMapping mapping : existingMappings) {
                prompt.append("- ").append(mapping.toPromptFormat()).append("\n");
            }
            prompt.append("\n");
            prompt.append("PATTERN ANALYSIS: Study the above mappings to understand naming conventions, abbreviation patterns, and business logic relationships.\n\n");
        }
        
        // Add target headers (available options)
        prompt.append("TARGET HEADERS (Available options to match to):\n");
        for (int i = 0; i < targetHeaders.length; i++) {
            prompt.append((i + 1)).append(". ").append(targetHeaders[i]).append("\n");
        }
        prompt.append("\n");
        
        // Add single source header (to be matched)
        prompt.append("SOURCE HEADER TO MATCH:\n");
        prompt.append(sourceHeader).append("\n\n");
        
        // Add matching guidelines
        prompt.append("MATCHING GUIDELINES:\n");
        prompt.append("- First check for exact matches in existing mappings\n");
        prompt.append("- Learn patterns from existing mappings (e.g., abbreviations, naming conventions)\n");
        prompt.append("- Apply business logic and semantic similarity\n");
        prompt.append("- Consider common abbreviations (Qty=Quantity, Desc=Description, etc.)\n");
        prompt.append("- Use context clues from similar mappings\n");
        prompt.append("- Be consistent with learned patterns\n");
        prompt.append("- Flag whether you used an existing mapping or inferred the match\n\n");
        
        // Add industry context if provided
        if (industryContext != null && !industryContext.trim().isEmpty()) {
            prompt.append("INDUSTRY CONTEXT: ").append(industryContext).append("\n\n");
        }
        
        prompt.append("CRITICAL: You must respond with ONLY a valid JSON object. Do not include any markdown formatting, explanations, or additional text.\n");
        prompt.append("OUTPUT FORMAT: Return ONLY this JSON object:\n");
        prompt.append("{\n");
        prompt.append("  \"sourceHeader\": \"").append(sourceHeader).append("\",\n");
        prompt.append("  \"matchedTargetHeader\": \"string\",\n");
        prompt.append("  \"confidencePercentage\": number,\n");
        prompt.append("  \"reasoning\": \"string\",\n");
        prompt.append("  \"usedExistingMapping\": boolean\n");
        prompt.append("}\n");
        prompt.append("\n");
        prompt.append("IMPORTANT: Start your response with { and end with }. No markdown backticks, no explanations before or after the JSON.\n");
        
        String finalPrompt = prompt.toString();
        logger.debug("Prompt building completed for '{}', final length: {} characters", sourceHeader, finalPrompt.length());
        return finalPrompt;
    }
    

    
    /**
     * Parses a single column matching response from AI
     */
    private ColumnMatchingResult parseSingleColumnResponse(String response, String sourceHeader) throws AIProviderException {
        try {
            logger.debug("Parsing single column response for '{}' of length: {}", sourceHeader, response.length());
            logger.debug("Raw response content: {}", response.length() > 500 ? response.substring(0, 500) + "..." : response);
            
            // Extract JSON from the response content
            String jsonContent = extractSingleColumnJsonFromContent(response);
            logger.debug("Extracted JSON content of length: {}", jsonContent.length());
            logger.debug("Extracted JSON content: {}", jsonContent);
            
            // Parse the JSON object
            JsonNode resultNode = objectMapper.readTree(jsonContent);
            
            if (!resultNode.isObject()) {
                throw new AIProviderException("Response is not a JSON object");
            }
            
            // Create the result
            ColumnMatchingResult result = new ColumnMatchingResult();
            result.setSourceHeader(resultNode.path("sourceHeader").asText(sourceHeader));
            result.setMatchedTargetHeader(resultNode.path("matchedTargetHeader").asText());
            result.setConfidencePercentage(resultNode.path("confidencePercentage").asDouble(0.0));
            result.setReasoning(resultNode.path("reasoning").asText(""));
            
            // Handle both field names for backward compatibility
            boolean usedExistingMapping = resultNode.path("usedExistingMapping").asBoolean(false) || 
                                        resultNode.path("usedReferenceMapping").asBoolean(false);
            result.setUsedExistingMapping(usedExistingMapping);
            
            logger.debug("Successfully parsed result: {} -> {} (confidence: {}%)", 
                        result.getSourceHeader(), result.getMatchedTargetHeader(), result.getConfidencePercentage());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to parse single column response for '{}': {}", sourceHeader, e.getMessage());
            logger.debug("Response content: {}", response);
            
            // Return a default result instead of throwing
            return new ColumnMatchingResult(sourceHeader, "", 0.0, 
                "Parsing failed: " + e.getMessage(), false);
        }
    }
    

    
    /**
     * Extracts JSON content from single column AI response text
     */
    private String extractSingleColumnJsonFromContent(String content) throws AIProviderException {
        logger.debug("Extracting single column JSON from response of length: {}", content.length());
        logger.debug("Response content preview: {}", content.length() > 200 ? content.substring(0, 200) + "..." : content);
        
        // Look for JSON object in the response
        int startIndex = content.indexOf('{');
        logger.debug("Found JSON start at index: {}", startIndex);
        
        if (startIndex == -1) {
            logger.error("No JSON object start found in response");
            throw new AIProviderException("No JSON object start found in response. Response preview: " + 
                (content.length() > 100 ? content.substring(0, 100) + "..." : content));
        }
        
        // Try to find complete JSON first
        String jsonContent = extractCompleteSingleColumnJson(content, startIndex);
        
        if (jsonContent != null) {
            logger.debug("Found complete single column JSON response");
            return jsonContent;
        }
        
        // If complete JSON not found, try to extract partial results
        logger.warn("Single column response appears to be truncated, attempting to extract partial results");
        return extractPartialSingleColumnJson(content, startIndex);
    }
    

    
    /**
     * Attempts to extract complete single column JSON from the response
     */
    private String extractCompleteSingleColumnJson(String content, int startIndex) {
        try {
            // Look for the closing brace
            int endIndex = content.indexOf('}', startIndex);
            if (endIndex == -1) {
                return null;
            }
            
            String jsonContent = content.substring(startIndex, endIndex + 1);
            
            // Try to parse the original JSON first
            try {
                objectMapper.readTree(jsonContent);
                return jsonContent;
            } catch (Exception e) {
                logger.debug("Original single column JSON parsing failed, attempting to fix common issues: {}", e.getMessage());
                
                // Try to fix common JSON issues
                String fixedJson = fixCommonJsonIssues(jsonContent);
                if (fixedJson != null && !fixedJson.equals(jsonContent)) {
                    try {
                        objectMapper.readTree(fixedJson);
                        logger.info("Successfully fixed single column JSON formatting issues");
                        return fixedJson;
                    } catch (Exception e2) {
                        logger.debug("Fixed single column JSON still invalid: {}", e2.getMessage());
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.debug("Complete single column JSON extraction failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts partial single column JSON from a truncated response
     */
    private String extractPartialSingleColumnJson(String content, int startIndex) throws AIProviderException {
        // For single column, we can be more aggressive in salvaging
        int endIndex = content.length() - 1;
        
        // Find the last closing brace
        for (int i = endIndex; i >= startIndex; i--) {
            if (content.charAt(i) == '}') {
                String partialContent = content.substring(startIndex, i + 1);
                
                try {
                    // Validate the salvaged JSON
                    objectMapper.readTree(partialContent);
                    logger.info("Successfully salvaged partial single column JSON content");
                    return partialContent;
                } catch (Exception e) {
                    logger.debug("Salvaged single column content still invalid: {}", e.getMessage());
                    continue;
                }
            }
        }
        
        // If we still can't salvage anything, throw a more descriptive error
        String responsePreview = content.length() > 200 ? 
            content.substring(0, 200) + "..." : content;
        throw new AIProviderException("Unable to extract valid single column JSON from truncated response. " +
            "Response preview: " + responsePreview);
    }
    
    /**
     * Attempts to fix common JSON formatting issues
     */
    private String fixCommonJsonIssues(String jsonContent) {
        if (jsonContent == null) return null;
        
        String fixed = jsonContent;
        
        // Fix common issues
        fixed = fixed.replaceAll(",\\s*]", "]"); // Remove trailing commas
        fixed = fixed.replaceAll(",\\s*}", "}"); // Remove trailing commas in objects
        fixed = fixed.replaceAll("\\s+", " "); // Normalize whitespace
        
        // Fix unescaped quotes in strings (basic fix)
        fixed = fixed.replaceAll("([^\\\\])\"([^\"]*?)([^\\\\])\"", "$1\"$2$3\"");
        
        return fixed;
    }
    

    

    
    /**
     * Post-processes the results to ensure quality and consistency
     */
    private void postProcessResults(List<ColumnMatchingResult> results, String[] sourceHeaders, 
                                  String[] targetHeaders, List<ColumnMapping> existingMappings) {
        
        // Validate that all source headers have results
        if (results.size() != sourceHeaders.length) {
            logger.warn("Result count ({}) doesn't match source header count ({})", 
                       results.size(), sourceHeaders.length);
        }
        
        // Validate results
        results.removeIf(result -> !result.isValid());
        
        // Ensure all source headers are covered
        List<String> coveredHeaders = new ArrayList<>();
        for (ColumnMatchingResult result : results) {
            coveredHeaders.add(result.getSourceHeader());
        }
        
        // Add missing headers with default values
        for (String sourceHeader : sourceHeaders) {
            if (!coveredHeaders.contains(sourceHeader)) {
                logger.warn("No result found for source header: {}", sourceHeader);
                ColumnMatchingResult defaultResult = new ColumnMatchingResult(
                    sourceHeader, "", 0.0, "No match found", false);
                results.add(defaultResult);
            }
        }
    }
}
