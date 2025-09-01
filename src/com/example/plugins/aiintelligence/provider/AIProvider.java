package com.example.plugins.aiintelligence.provider;

import com.example.plugins.aiintelligence.domain.ColumnMatchingResult;
import com.example.plugins.aiintelligence.domain.ColumnMapping;

import java.util.List;

/**
 * Core interface for AI providers that can perform intelligent column matching.
 * This abstraction allows the system to work with different AI services
 * (AWS Bedrock, OpenAI, Appian LL, etc.) through a common interface.
 */
public interface AIProvider {
    
    /**
     * Gets the unique identifier for this AI provider
     * 
     * @return The provider identifier
     */
    String getProviderId();
    
    /**
     * Gets the display name for this AI provider
     * 
     * @return The provider display name
     */
    String getProviderName();
    
    /**
     * Checks if this provider is properly configured and ready to use
     * 
     * @return true if the provider is ready, false otherwise
     */
    boolean isReady();
    
    /**
     * Performs intelligent column header matching using AI
     * 
     * @param sourceHeaders The source column headers to be matched
     * @param targetHeaders The available target column headers
     * @param existingMappings Existing mappings to learn from
     * @param industryContext Optional industry context for better matching
     * @param modelConfiguration Configuration for the AI model
     * @return List of column matching results
     * @throws AIProviderException if an error occurs during processing
     */
    List<ColumnMatchingResult> performColumnMatching(
        String[] sourceHeaders,
        String[] targetHeaders,
        List<ColumnMapping> existingMappings,
        String industryContext,
        ModelConfiguration modelConfiguration
    ) throws AIProviderException;
    
    /**
     * Gets the supported models for this provider
     * 
     * @return List of supported model identifiers
     */
    List<String> getSupportedModels();
    
    /**
     * Validates the configuration for this provider
     * 
     * @param configuration The configuration to validate
     * @throws AIProviderException if the configuration is invalid
     */
    void validateConfiguration(ProviderConfiguration configuration) throws AIProviderException;
}
