package com.example.plugins.aiintelligence.provider.impl;

import com.example.plugins.aiintelligence.provider.*;
import com.example.plugins.aiintelligence.domain.ColumnMatchingResult;
import com.example.plugins.aiintelligence.domain.ColumnMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

/**
 * OpenAI implementation of the AI provider interface.
 * This provider uses OpenAI's GPT models for intelligent column matching.
 */
public class OpenAIProvider extends AbstractAIProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIProvider.class);
    private static final String PROVIDER_ID = "openai";
    private static final String PROVIDER_NAME = "OpenAI";
    
    private OpenAiService openAiService;
    private String apiKey;
    private String organization;
    
    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }
    
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public List<String> getSupportedModels() {
        List<String> models = new ArrayList<>();
        models.add("gpt-4");
        models.add("gpt-4-turbo-preview");
        models.add("gpt-4-32k");
        models.add("gpt-3.5-turbo");
        models.add("gpt-3.5-turbo-16k");
        return models;
    }
    
    @Override
    protected void validateProviderConfiguration(ProviderConfiguration configuration) throws AIProviderException {
        logger.info("Validating OpenAI provider configuration");
        
        // Extract OpenAI-specific configuration
        this.apiKey = configuration.getParameter("apiKey", String.class);
        this.organization = configuration.getParameter("organization", String.class);
        
        logger.debug("OpenAI configuration - API Key: {}, Organization: {}", 
                    apiKey != null ? "***" : "null", organization);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("OpenAI API key is required but not provided");
            throw new AIProviderException("OpenAI API key is required");
        }
        
        // Initialize OpenAI service
        logger.info("Initializing OpenAI service");
        initializeOpenAIService();
        logger.info("OpenAI provider configuration validation completed successfully");
    }
    
    @Override
    protected String sendAIRequest(String prompt, ModelConfiguration modelConfiguration) throws AIProviderException {
        logger.info("Sending AI request to OpenAI with model: {}", modelConfiguration.getModelId());
        logger.debug("Prompt length: {} characters", prompt.length());
        
        try {
            String modelId = modelConfiguration.getModelId();
            
            // Create chat message
            ChatMessage userMessage = new ChatMessage("user", prompt);
            logger.debug("Created chat message for OpenAI request");
            
            // Build completion request
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(modelId)
                .messages(List.of(userMessage))
                .maxTokens(modelConfiguration.getMaxTokens())
                .temperature(modelConfiguration.getTemperature())
                .topP(modelConfiguration.getTopP())
                .build();
            
            logger.debug("Built OpenAI completion request with maxTokens: {}, temperature: {}, topP: {}", 
                        modelConfiguration.getMaxTokens(), modelConfiguration.getTemperature(), modelConfiguration.getTopP());
            
            // Send request to OpenAI
            logger.debug("Sending request to OpenAI API");
            long startTime = System.currentTimeMillis();
            var response = openAiService.createChatCompletion(request);
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("OpenAI API request completed in {} ms", duration);
            
            if (response.getChoices() == null || response.getChoices().isEmpty()) {
                logger.error("Empty response from OpenAI API");
                throw new AIProviderException("Empty response from OpenAI");
            }
            
            String content = response.getChoices().get(0).getMessage().getContent();
            
            if (content == null || content.trim().isEmpty()) {
                logger.error("Empty content in OpenAI response");
                throw new AIProviderException("Empty content in OpenAI response");
            }
            
            logger.info("Successfully received content from OpenAI, length: {} characters", content.length());
            return content;
            
        } catch (Exception e) {
            logger.error("Failed to send request to OpenAI", e);
            throw new AIProviderException("Failed to send request to OpenAI", e);
        }
    }
    
    /**
     * Initializes the OpenAI service
     */
    private void initializeOpenAIService() throws AIProviderException {
        logger.info("Initializing OpenAI service");
        try {
            if (openAiService != null) {
                logger.debug("OpenAI service already exists, creating new instance");
                // OpenAI service doesn't have a close method, just create new instance
            }
            
            // Create service with custom API client and API key
            logger.debug("Creating OpenAI service with 2-minute timeout");
            openAiService = new OpenAiService(apiKey, Duration.ofMinutes(2));
            logger.info("OpenAI service successfully initialized with custom timeout settings (2-minute read timeout)");
            
        } catch (Exception e) {
            logger.error("Failed to initialize OpenAI service", e);
            throw new AIProviderException("Failed to initialize OpenAI service", e);
        }
    }
    
    /**
     * Cleanup method (OpenAI service doesn't require explicit cleanup)
     */
    public void cleanup() {
        // OpenAI service doesn't have a close method
        logger.debug("OpenAI service cleanup completed");
    }
}
