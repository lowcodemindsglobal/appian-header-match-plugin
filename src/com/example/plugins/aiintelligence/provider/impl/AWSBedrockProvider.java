package com.example.plugins.aiintelligence.provider.impl;

import com.example.plugins.aiintelligence.provider.*;
import com.example.plugins.aiintelligence.domain.ColumnMatchingResult;
import com.example.plugins.aiintelligence.domain.ColumnMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * AWS Bedrock implementation of the AI provider interface.
 * This provider uses AWS Bedrock services for intelligent column matching.
 */
public class AWSBedrockProvider extends AbstractAIProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(AWSBedrockProvider.class);
    private static final String PROVIDER_ID = "aws-bedrock";
    private static final String PROVIDER_NAME = "AWS Bedrock";
    
    private BedrockRuntimeClient bedrockClient;
    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    
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
        models.add("anthropic.claude-3-sonnet-20240229-v1:0");
        models.add("anthropic.claude-3-haiku-20240307-v1:0");
        models.add("anthropic.claude-3-opus-20240229-v1:0");
        models.add("meta.llama-3-8b-instruct-v1:0");
        models.add("meta.llama-3-70b-instruct-v1:0");
        models.add("amazon.titan-text-express-v1");
        models.add("amazon.titan-text-lite-v1");
        return models;
    }
    
    @Override
    protected void validateProviderConfiguration(ProviderConfiguration configuration) throws AIProviderException {
        // Extract AWS-specific configuration
        this.region = configuration.getParameter("region", String.class);
        this.accessKeyId = configuration.getParameter("accessKeyId", String.class);
        this.secretAccessKey = configuration.getParameter("secretAccessKey", String.class);
        
        if (region == null || region.trim().isEmpty()) {
            throw new AIProviderException("AWS region is required");
        }
        
        // Validate region format
        try {
            Region.of(region);
        } catch (IllegalArgumentException e) {
            throw new AIProviderException("Invalid AWS region: " + region);
        }
        
        // Initialize AWS client
        initializeBedrockClient();
    }
    
    @Override
    protected String sendAIRequest(String prompt, ModelConfiguration modelConfiguration) throws AIProviderException {
        try {
            String modelId = modelConfiguration.getModelId();
            
            // Prepare request body based on model type
            String requestBody = prepareRequestBody(prompt, modelConfiguration);
            
            // Create invoke request
            InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();
            
            // Invoke the model
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            // Extract content based on model type
            String content = extractContentFromResponse(responseBody, modelId);
            
            if (content == null || content.trim().isEmpty()) {
                throw new AIProviderException("Empty response from Bedrock model");
            }
            
            return content;
            
        } catch (Exception e) {
            throw new AIProviderException("Failed to send request to AWS Bedrock", e);
        }
    }
    
    /**
     * Initializes the AWS Bedrock client
     */
    private void initializeBedrockClient() throws AIProviderException {
        try {
            if (bedrockClient != null) {
                bedrockClient.close();
            }
            
            // Build client with configuration
            var clientBuilder = BedrockRuntimeClient.builder()
                .region(Region.of(region));
            
            // Add credentials if provided
            if (accessKeyId != null && secretAccessKey != null) {
                // Note: In production, use AWS SDK's default credential chain
                // This is just for explicit configuration
                logger.warn("Using explicit AWS credentials. Consider using IAM roles or credential chain.");
            }
            
            bedrockClient = clientBuilder.build();
            logger.info("AWS Bedrock client initialized for region: {}", region);
            
        } catch (Exception e) {
            throw new AIProviderException("Failed to initialize AWS Bedrock client", e);
        }
    }
    
    /**
     * Prepares the request body based on model type
     */
    private String prepareRequestBody(String prompt, ModelConfiguration modelConfiguration) throws AIProviderException {
        try {
            Map<String, Object> requestMap = new HashMap<>();
            String modelId = modelConfiguration.getModelId();
            
            if (modelId.startsWith("anthropic.claude") || modelId.startsWith("us.anthropic.claude")) {
                // Claude models use messages format
                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", prompt);
                
                requestMap.put("anthropic_version", "bedrock-2023-05-31");
                requestMap.put("messages", new Object[]{message});
                requestMap.put("max_tokens", modelConfiguration.getMaxTokens());
                requestMap.put("temperature", modelConfiguration.getTemperature());
                requestMap.put("top_p", modelConfiguration.getTopP());
                
            } else if (modelId.contains("llama") || modelId.contains("meta.llama")) {
                // Llama models use prompt format
                requestMap.put("prompt", prompt);
                requestMap.put("max_gen_len", modelConfiguration.getMaxTokens());
                requestMap.put("temperature", modelConfiguration.getTemperature());
                requestMap.put("top_p", modelConfiguration.getTopP());
                
            } else if (modelId.startsWith("amazon.titan")) {
                // Titan models use prompt format
                requestMap.put("inputText", prompt);
                requestMap.put("textGenerationConfig", Map.of(
                    "maxTokenCount", modelConfiguration.getMaxTokens(),
                    "temperature", modelConfiguration.getTemperature(),
                    "topP", modelConfiguration.getTopP()
                ));
                
            } else {
                // Default format for other models
                requestMap.put("prompt", prompt);
                requestMap.put("max_tokens_to_sample", modelConfiguration.getMaxTokens());
                requestMap.put("temperature", modelConfiguration.getTemperature());
                requestMap.put("top_p", modelConfiguration.getTopP());
            }
            
            return objectMapper.writeValueAsString(requestMap);
            
        } catch (Exception e) {
            throw new AIProviderException("Failed to prepare request body", e);
        }
    }
    
    /**
     * Extracts content from response based on model type
     */
    private String extractContentFromResponse(String responseBody, String modelId) throws AIProviderException {
        try {
            JsonNode responseNode = objectMapper.readTree(responseBody);
            
            if (modelId.startsWith("anthropic.claude") || modelId.startsWith("us.anthropic.claude")) {
                JsonNode contentArray = responseNode.path("content");
                if (contentArray.isArray() && contentArray.size() > 0) {
                    return contentArray.get(0).path("text").asText();
                } else {
                    return responseNode.path("completion").asText();
                }
                
            } else if (modelId.startsWith("meta.llama")) {
                return responseNode.path("generation").asText();
                
            } else if (modelId.startsWith("amazon.titan")) {
                return responseNode.path("results").path(0).path("outputText").asText();
                
            } else {
                // Default extraction for other models
                return responseNode.path("completion").asText();
            }
            
        } catch (Exception e) {
            throw new AIProviderException("Failed to extract content from response", e);
        }
    }
    
    /**
     * Cleanup method to close AWS client
     */
    public void cleanup() {
        if (bedrockClient != null) {
            try {
                bedrockClient.close();
                logger.info("AWS Bedrock client closed");
            } catch (Exception e) {
                logger.warn("Error closing AWS Bedrock client", e);
            }
        }
    }
}
