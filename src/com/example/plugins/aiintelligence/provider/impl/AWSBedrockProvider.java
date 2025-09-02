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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Properties;

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
        models.add("meta.llama4-scout-17b-instruct-v1:0");
        models.add("amazon.titan-text-express-v1");
        models.add("amazon.titan-text-lite-v1");
        
        // Add support for application inference profiles
        models.add("arn:aws:bedrock:us-west-2:211125498297:application-inference-profile/hx2e1juc8tej");
        
        return models;
    }
    
    @Override
    protected void validateProviderConfiguration(ProviderConfiguration configuration) throws AIProviderException {
        logger.info("Validating AWS Bedrock provider configuration");
        
        // Log all received parameters for debugging
        logger.info("Received provider configuration parameters:");
        logger.info("Total parameters received: {}", configuration.getAllParameters().size());
        for (Map.Entry<String, Object> entry : configuration.getAllParameters().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.toLowerCase().contains("key") || key.toLowerCase().contains("secret")) {
                logger.info("  {} = ***", key);
            } else {
                logger.info("  {} = {}", key, value);
            }
        }
        
        // Log configuration details
        logger.info("Provider configuration details:");
        logger.info("  Provider ID: {}", configuration.getProviderId());
        logger.info("  Provider Name: {}", configuration.getProviderName());
        logger.info("  Configuration valid: {}", configuration.isValid());
        
        // Extract AWS-specific configuration from provider parameters
        this.region = configuration.getParameter("region", String.class);
        this.accessKeyId = configuration.getParameter("accessKeyId", String.class);
        this.secretAccessKey = configuration.getParameter("secretAccessKey", String.class);
        
        logger.debug("Extracted parameters - Region: {}, AccessKeyId: {}, SecretAccessKey: {}", 
                   region, accessKeyId != null ? "***" : "null", secretAccessKey != null ? "***" : "null");
        
        // Always try to load from config.properties if parameters are missing
        // This provides a fallback when Appian parameter configuration is complex
        if (accessKeyId == null || secretAccessKey == null || region == null || region.trim().isEmpty()) {
            logger.info("AWS credentials not fully provided via parameters, attempting to load from config.properties");
            logger.info("Current state - Region: {}, AccessKeyId: {}, SecretAccessKey: {}", 
                       region, accessKeyId != null ? "***" : "null", secretAccessKey != null ? "***" : "null");
            loadCredentialsFromConfig();
            
            // Log the final state after loading
            logger.info("After loading from config - Region: {}, AccessKeyId: {}, SecretAccessKey: {}", 
                       region, accessKeyId != null ? "***" : "null", secretAccessKey != null ? "***" : "null");
        } else {
            logger.info("All AWS credentials provided via parameters");
        }
        
        logger.debug("AWS configuration - Region: {}, AccessKeyId: {}, SecretAccessKey: {}", 
                    region, accessKeyId != null ? "***" : "null", secretAccessKey != null ? "***" : "null");
        
        if (region == null || region.trim().isEmpty()) {
            logger.error("AWS region is required but not provided");
            logger.error("No parameters were received from Appian configuration");
            logger.error("SOLUTION OPTIONS:");
            logger.error("Option 1 - Use config.properties (RECOMMENDED):");
            logger.error("  Place config.properties in Appian server classpath with:");
            logger.error("  aws.region=us-west-2");
            logger.error("  aws.accessKeyId=YOUR_ACCESS_KEY");
            logger.error("  aws.secretAccessKey=YOUR_SECRET_KEY");
            logger.error("Option 2 - Configure Appian parameters:");
            logger.error("  Provider Parameter Keys: [\"region\", \"accessKeyId\", \"secretAccessKey\"]");
            logger.error("  Provider Parameter Values: [\"us-west-2\", \"YOUR_ACCESS_KEY\", \"YOUR_SECRET_KEY\"]");
            throw new AIProviderException("AWS region is required. " +
                "SOLUTION: Either configure Provider Parameters in Appian OR place config.properties in server classpath with aws.region, aws.accessKeyId, and aws.secretAccessKey properties.");
        }
        
        // Validate that credentials are available (either from parameters or config)
        if ((accessKeyId == null || accessKeyId.trim().isEmpty()) || 
            (secretAccessKey == null || secretAccessKey.trim().isEmpty())) {
            logger.warn("AWS credentials not provided via parameters. Will attempt to use default credential chain or config file.");
            logger.info("For explicit credentials, provide 'accessKeyId' and 'secretAccessKey' parameters.");
        }
        
        // Validate region format
        try {
            Region.of(region);
            logger.debug("AWS region validation successful: {}", region);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid AWS region: {}", region, e);
            throw new AIProviderException("Invalid AWS region: " + region);
        }
        
        // Don't initialize client here - do it when actually needed
        logger.info("AWS Bedrock provider configuration validation completed successfully");
    }
    
    @Override
    protected String sendAIRequest(String prompt, ModelConfiguration modelConfiguration) throws AIProviderException {
        logger.info("Sending AI request to AWS Bedrock with model: {}", modelConfiguration.getModelId());
        logger.debug("Prompt length: {} characters", prompt.length());
        
        try {
            // Ensure client is initialized before use
            if (bedrockClient == null) {
                logger.info("AWS Bedrock client not initialized, initializing now");
                initializeBedrockClient();
            }
            
            String modelId = modelConfiguration.getModelId();
            
            // Prepare request body based on model type
            logger.debug("Preparing request body for model: {}", modelId);
            String requestBody = prepareRequestBody(prompt, modelConfiguration);
            logger.debug("Request body prepared, length: {} characters", requestBody.length());
            
            // Create invoke request
            InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();
            
            logger.debug("Invoking AWS Bedrock model: {}", modelId);
            long startTime = System.currentTimeMillis();
            
            // Invoke the model
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("AWS Bedrock model invocation completed in {} ms", duration);
            logger.debug("Response body length: {} characters", responseBody.length());
            
            // Extract content based on model type
            String content = extractContentFromResponse(responseBody, modelId);
            
            if (content == null || content.trim().isEmpty()) {
                logger.error("Empty response from Bedrock model: {}", modelId);
                throw new AIProviderException("Empty response from Bedrock model");
            }
            
            logger.info("Successfully extracted content from AWS Bedrock response, length: {} characters", content.length());
            return content;
            
        } catch (Exception e) {
            logger.error("Failed to send request to AWS Bedrock", e);
            throw new AIProviderException("Failed to send request to AWS Bedrock", e);
        }
    }
    
    /**
     * Loads AWS credentials from config.properties file
     */
    private void loadCredentialsFromConfig() {
        try {
            Properties config = new Properties();
            
            // Try to find config.properties using absolute path approach
            String[] possiblePaths = {
                "config.properties",                    // Current directory
                "../config.properties",                 // Parent directory
                "../../config.properties",              // Grandparent directory
                "src/main/resources/config.properties", // Maven resources
                "target/classes/config.properties"      // Maven target
            };
            
            InputStream configStream = null;
            String usedPath = null;
            
            logger.info("Attempting to load config.properties from multiple locations...");
            logger.info("Current working directory: {}", System.getProperty("user.dir"));
            logger.info("Java classpath: {}", System.getProperty("java.class.path"));
            
            // Try classpath first (most reliable for deployed plugins)
            logger.info("Trying to load config.properties from classpath...");
            configStream = getClass().getClassLoader().getResourceAsStream("config.properties");
            if (configStream != null) {
                usedPath = "classpath:config.properties";
                logger.info("SUCCESS: Found config.properties in classpath");
            } else {
                logger.info("FAILED: config.properties not found in classpath");
                
                // Try file system paths as fallback (for development)
                for (String path : possiblePaths) {
                    try {
                        logger.info("Trying to load config.properties from: {}", path);
                        configStream = new FileInputStream(path);
                        usedPath = path;
                        logger.info("SUCCESS: Found config.properties at: {}", path);
                        break;
                    } catch (Exception e) {
                        logger.info("FAILED: config.properties not found at: {} - {}", path, e.getMessage());
                    }
                }
            }
            
            if (configStream != null) {
                config.load(configStream);
                configStream.close();
                
                // Load AWS credentials from config if not already set
                if (accessKeyId == null) {
                    accessKeyId = config.getProperty("aws.accessKeyId");
                    logger.debug("Loaded accessKeyId from config: {}", accessKeyId != null ? "***" : "null");
                }
                if (secretAccessKey == null) {
                    secretAccessKey = config.getProperty("aws.secretAccessKey");
                    logger.debug("Loaded secretAccessKey from config: {}", secretAccessKey != null ? "***" : "null");
                }
                if (region == null || region.trim().isEmpty()) {
                    region = config.getProperty("aws.region");
                    logger.debug("Loaded region from config: {}", region);
                }
                
                logger.info("Successfully loaded AWS credentials from config.properties at: {}", usedPath);
                logger.info("Final credential state - Region: {}, AccessKeyId: {}, SecretAccessKey: {}", 
                           region, accessKeyId != null ? "***" : "null", secretAccessKey != null ? "***" : "null");
            } else {
                logger.error("config.properties file not found in any of the expected locations");
                logger.error("Searched paths: {}", String.join(", ", possiblePaths));
                logger.error("Current working directory: {}", System.getProperty("user.dir"));
                logger.error("Java classpath: {}", System.getProperty("java.class.path"));
                
                // Fallback to environment variables
                logger.info("Attempting to load credentials from environment variables as fallback");
                if (accessKeyId == null) {
                    accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
                    logger.info("Loaded accessKeyId from env: {}", accessKeyId != null ? "***" : "null");
                }
                if (secretAccessKey == null) {
                    secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
                    logger.info("Loaded secretAccessKey from env: {}", secretAccessKey != null ? "***" : "null");
                }
                if (region == null || region.trim().isEmpty()) {
                    region = System.getenv("AWS_REGION");
                    logger.info("Loaded region from env: {}", region);
                }
                
                if (accessKeyId != null || secretAccessKey != null || region != null) {
                    logger.info("Loaded some credentials from environment variables");
                } else {
                    logger.error("No credentials found in environment variables either");
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load AWS credentials from config.properties: {}", e.getMessage());
            logger.debug("Exception details:", e);
        }
    }
    
    /**
     * Initializes the AWS Bedrock client
     */
    private void initializeBedrockClient() throws AIProviderException {
        logger.info("Initializing AWS Bedrock client for region: {}", region);
        try {
            if (bedrockClient != null) {
                logger.debug("Closing existing AWS Bedrock client");
                bedrockClient.close();
            }
            
            // Build client with configuration
            logger.debug("Building AWS Bedrock client with region: {}", region);
            var clientBuilder = BedrockRuntimeClient.builder()
                .region(Region.of(region));
            
            // Add credentials if provided
            if (accessKeyId != null && secretAccessKey != null) {
                logger.info("Using explicit AWS credentials from configuration");
                AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
                clientBuilder.credentialsProvider(StaticCredentialsProvider.create(credentials));
                logger.debug("AWS credentials configured successfully with StaticCredentialsProvider");
            } else {
                logger.warn("No explicit AWS credentials provided, using default credential chain");
                logger.warn("This may fail if no credentials are configured in the environment");
                logger.info("Make sure AWS credentials are configured via one of the following methods:");
                logger.info("1. Environment variables: AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY");
                logger.info("2. System properties: aws.accessKeyId and aws.secretKey");
                logger.info("3. AWS credentials file: ~/.aws/credentials");
                logger.info("4. IAM roles (if running on EC2)");
                logger.info("5. Container credentials (if running in ECS/EKS)");
                logger.info("6. Provider parameters: accessKeyId and secretAccessKey");
                logger.info("7. config.properties file: aws.accessKeyId, aws.secretAccessKey, aws.region");
            }
            
            bedrockClient = clientBuilder.build();
            logger.info("AWS Bedrock client successfully initialized for region: {}", region);
            
        } catch (Exception e) {
            logger.error("Failed to initialize AWS Bedrock client for region: {}", region, e);
            
            // Provide more specific error message for credential issues
            if (e.getMessage() != null && e.getMessage().contains("credentials")) {
                throw new AIProviderException("AWS credentials not found. Please configure AWS credentials using one of the following methods:\n" +
                    "1. Environment variables: AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY\n" +
                    "2. System properties: aws.accessKeyId and aws.secretKey\n" +
                    "3. AWS credentials file: ~/.aws/credentials\n" +
                    "4. IAM roles (if running on EC2)\n" +
                    "5. Container credentials (if running in ECS/EKS)\n" +
                    "6. Provider parameters: accessKeyId and secretAccessKey\n" +
                    "7. config.properties file: aws.accessKeyId, aws.secretAccessKey, aws.region\n" +
                    "Original error: " + e.getMessage(), e);
            } else {
                throw new AIProviderException("Failed to initialize AWS Bedrock client: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Prepares the request body based on model type
     */
    private String prepareRequestBody(String prompt, ModelConfiguration modelConfiguration) throws AIProviderException {
        try {
            Map<String, Object> requestMap = new HashMap<>();
            String modelId = modelConfiguration.getModelId();
            
            // Get the effective model ID to handle application inference profiles
            String effectiveModelId = getEffectiveModelId(modelId);
            
            if (effectiveModelId.startsWith("anthropic.claude") || effectiveModelId.startsWith("us.anthropic.claude")) {
                // Claude models use messages format
                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", prompt);
                
                requestMap.put("anthropic_version", "bedrock-2023-05-31");
                requestMap.put("messages", new Object[]{message});
                requestMap.put("max_tokens", modelConfiguration.getMaxTokens());
                requestMap.put("temperature", modelConfiguration.getTemperature());
                requestMap.put("top_p", modelConfiguration.getTopP());
                
            } else if (effectiveModelId.contains("llama") || effectiveModelId.contains("meta.llama")) {
                // Llama models use prompt format
                requestMap.put("prompt", prompt);
                requestMap.put("max_gen_len", modelConfiguration.getMaxTokens());
                requestMap.put("temperature", modelConfiguration.getTemperature());
                requestMap.put("top_p", modelConfiguration.getTopP());
                
            } else if (effectiveModelId.startsWith("amazon.titan")) {
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
            
            // Handle application inference profile ARNs by detecting the underlying model type
            String effectiveModelId = getEffectiveModelId(modelId);
            
            if (effectiveModelId.startsWith("anthropic.claude") || effectiveModelId.startsWith("us.anthropic.claude")) {
                JsonNode contentArray = responseNode.path("content");
                if (contentArray.isArray() && contentArray.size() > 0) {
                    return contentArray.get(0).path("text").asText();
                } else {
                    return responseNode.path("completion").asText();
                }
                
            } else if (effectiveModelId.startsWith("meta.llama")) {
                return responseNode.path("generation").asText();
                
            } else if (effectiveModelId.startsWith("amazon.titan")) {
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
     * Gets the effective model ID by resolving application inference profile ARNs
     * to their underlying foundation model IDs
     */
    private String getEffectiveModelId(String modelId) {
        // If it's an application inference profile ARN, we need to determine the underlying model
        if (modelId.contains("application-inference-profile")) {
            // For your specific ARN, we know it's using Llama 4 Scout 17B
            if (modelId.contains("hx2e1juc8tej")) {
                return "meta.llama4-scout-17b-instruct-v1:0";
            }
            // Add more mappings as needed for other application inference profiles
        }
        return modelId;
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
