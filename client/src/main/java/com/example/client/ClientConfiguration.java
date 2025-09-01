package com.example.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration management for the Simple Bedrock Client
 * Handles command line arguments, configuration files, and environment variables
 */
public class ClientConfiguration {
    
    // Default values
    private static final String DEFAULT_MODEL_ID = "anthropic.claude-3-sonnet-20240229-v1:0";
    private static final String DEFAULT_REGION = "us-west-2";
    private static final Double DEFAULT_TEMPERATURE = 0.7;
    private static final Integer DEFAULT_MAX_TOKENS = 1000;
    private static final String DEFAULT_CONFIG_FILE = "src/main/resources/client-config.properties";
    
    // Configuration file path
    private static final String CONFIG_FILE = "config.properties";
    
    // Configuration properties
    private String modelId;
    private String prompt;
    private String awsRegion;
    private String accessKeyId;
    private String secretAccessKey;
    private Double temperature;
    private Integer maxTokens;
    private boolean interactiveMode;
    private String batchFile;
    private boolean saveResponses;
    private String outputDirectory;
    
    // Header Matching Configuration
    private String operationMode;
    private String[] standardHeadersArray;
    private String[] buySheetHeadersArray;
    private String standardHeadersFile;
    private String buySheetHeadersFile;
    private String referenceMappingsFile;
    private String industryContext;
    
    public static ClientConfiguration loadConfiguration(String[] args) {
        ClientConfiguration config = new ClientConfiguration();
        
        // 1. Load from main config file if exists
        config.loadFromMainConfigFile();
        
        // 2. Load from default config file if exists
        config.loadFromPropertiesFile(DEFAULT_CONFIG_FILE);
        
        // 3. Load from environment variables
        config.loadFromEnvironment();
        
        // 4. Override with command line arguments
        config.parseCommandLineArgs(args);
        
        // Debug: Show what was parsed
        System.out.println("After parsing - Standard headers file: " + config.standardHeadersFile);
        System.out.println("After parsing - Buy sheet headers file: " + config.buySheetHeadersFile);
        System.out.println("After parsing - Existing mappings file: " + config.referenceMappingsFile);
        
        // 5. Set defaults for any missing values
        config.setDefaults();
        
        return config;
    }
    
    private void loadFromPropertiesFile(String filename) {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(filename));
            
            modelId = props.getProperty("default.model.id");
            awsRegion = props.getProperty("aws.region");
            accessKeyId = props.getProperty("aws.access.key.id");
            secretAccessKey = props.getProperty("aws.secret.access.key");
            
            String tempStr = props.getProperty("default.temperature");
            if (tempStr != null) {
                temperature = Double.parseDouble(tempStr);
            }
            
            String tokensStr = props.getProperty("default.max.tokens");
            if (tokensStr != null) {
                maxTokens = Integer.parseInt(tokensStr);
            }
            
            saveResponses = Boolean.parseBoolean(props.getProperty("output.save.responses", "true"));
            outputDirectory = props.getProperty("output.directory", "client-output/responses");
            
            System.out.println("Configuration loaded from: " + filename);
            
        } catch (IOException e) {
            System.out.println("Config file not found, using defaults: " + filename);
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in config file: " + e.getMessage());
        }
    }
    
    private void loadFromMainConfigFile() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(CONFIG_FILE));
            
            // Load data file paths
            standardHeadersFile = props.getProperty("standard.headers.file");
            buySheetHeadersFile = props.getProperty("buy.sheet.headers.file");
            referenceMappingsFile = props.getProperty("existing.mappings.file");
            
            // Load other configuration
            String tempStr = props.getProperty("default.temperature");
            if (tempStr != null) {
                temperature = Double.parseDouble(tempStr);
            }
            
            String tokensStr = props.getProperty("default.max.tokens");
            if (tokensStr != null) {
                maxTokens = Integer.parseInt(tokensStr);
            }
            
            if (modelId == null) {
                modelId = props.getProperty("default.model.id");
            }
            
            if (awsRegion == null) {
                awsRegion = props.getProperty("aws.default.region");
            }
            
            System.out.println("Main configuration loaded from: " + CONFIG_FILE);
            
        } catch (IOException e) {
            System.out.println("Main config file not found, using defaults: " + CONFIG_FILE);
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in main config file: " + e.getMessage());
        }
    }
    
    private void loadFromEnvironment() {
        if (accessKeyId == null) {
            accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        }
        if (secretAccessKey == null) {
            secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        }
        if (awsRegion == null) {
            awsRegion = System.getenv("AWS_DEFAULT_REGION");
        }
    }
    
    private void parseCommandLineArgs(String[] args) {
        System.out.println("Parsing " + args.length + " arguments:");
        for (int i = 0; i < args.length; i++) {
            System.out.println("  [" + i + "] = '" + args[i] + "'");
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            switch (arg) {
                case "--prompt":
                case "-p":
                    if (i + 1 < args.length) {
                        prompt = args[++i];
                    }
                    break;
                    
                case "--model":
                case "-m":
                    if (i + 1 < args.length) {
                        modelId = args[++i];
                    }
                    break;
                    
                case "--region":
                case "-r":
                    if (i + 1 < args.length) {
                        awsRegion = args[++i];
                    }
                    break;
                    
                case "--temperature":
                case "-t":
                    if (i + 1 < args.length) {
                        try {
                            temperature = Double.parseDouble(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid temperature value: " + args[i]);
                        }
                    }
                    break;
                    
                case "--max-tokens":
                case "-mt":
                    if (i + 1 < args.length) {
                        try {
                            maxTokens = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid max tokens value: " + args[i]);
                        }
                    }
                    break;
                    
                case "--access-key":
                    if (i + 1 < args.length) {
                        accessKeyId = args[++i];
                    }
                    break;
                    
                case "--secret-key":
                    if (i + 1 < args.length) {
                        secretAccessKey = args[++i];
                    }
                    break;
                    
                case "--interactive":
                case "-i":
                    interactiveMode = true;
                    break;
                    
                case "--batch-file":
                case "-bf":
                    if (i + 1 < args.length) {
                        batchFile = args[++i];
                    }
                    break;
                    
                case "--no-save":
                    saveResponses = false;
                    break;
                    
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;
                    

                    
                // Header Matching Options
                case "--header-matching":
                    operationMode = "HEADER_MATCHING";
                    break;
                    
                case "--chat-completion":
                    operationMode = "CHAT_COMPLETION";
                    break;
                    
                case "--standard-headers-file":
                    if (i + 1 < args.length) {
                        standardHeadersFile = args[++i];
                    }
                    break;
                    
                case "--buy-sheet-headers-file":
                    if (i + 1 < args.length) {
                        buySheetHeadersFile = args[++i];
                    }
                    break;
                    
                case "--reference-mappings-file":
                case "--existing-mappings-file":
                case "--vsi-mapping-file":
                    if (i + 1 < args.length) {
                        referenceMappingsFile = args[++i];
                    }
                    break;
                    
                case "--industry-context":
                    if (i + 1 < args.length) {
                        industryContext = args[++i];
                    }
                    break;
                    
                default:
                    if (arg.startsWith("-")) {
                        System.err.println("Unknown option: " + arg);
                    }
                    break;
            }
        }
    }
    
    private void setDefaults() {
        if (modelId == null) modelId = DEFAULT_MODEL_ID;
        if (awsRegion == null) awsRegion = DEFAULT_REGION;
        if (temperature == null) temperature = DEFAULT_TEMPERATURE;
        if (maxTokens == null) maxTokens = DEFAULT_MAX_TOKENS;
        if (outputDirectory == null) outputDirectory = "client-output/responses";
    }
    
    public void printUsage() {
        System.out.println("\nAWS Bedrock Plugin Test Client");
        System.out.println("Usage: java SimpleBedrockClient [options]");
        System.out.println("\nOptions:");
        System.out.println("  --prompt, -p <text>          Prompt text to send to AI model");
        System.out.println("  --model, -m <model-id>       AI model to use (default: Claude 3 Sonnet)");
        System.out.println("  --region, -r <region>        AWS region (default: us-west-2)");
        System.out.println("  --temperature, -t <value>    Temperature 0.0-1.0 (default: 0.7)");
        System.out.println("  --max-tokens, -mt <number>   Maximum tokens to generate (default: 1000)");
        System.out.println("  --access-key <key>           AWS access key ID");
        System.out.println("  --secret-key <key>           AWS secret access key");
        System.out.println("  --interactive, -i            Run in interactive mode");
        System.out.println("  --batch-file, -bf <file>     Process prompts from file");
        System.out.println("  --no-save                    Don't save responses to files");
        System.out.println("\nHeader Matching Options:");
        System.out.println("  --header-matching            Enable header matching mode");
        System.out.println("  --chat-completion            Enable chat completion mode (default)");
        System.out.println("  --standard-headers-file      CSV file with standard headers");
        System.out.println("  --buy-sheet-headers-file     CSV file with buy sheet headers");
        System.out.println("  --reference-mappings-file    CSV file with VSI reference mappings");
        System.out.println("  --vsi-mapping-file           Alias for --reference-mappings-file");
        System.out.println("  --industry-context <text>    Industry context for better matching");
        System.out.println("  --help, -h                   Show this help message");
        System.out.println("\nExamples:");
        System.out.println("  java SimpleBedrockClient --prompt \"Hello, how are you?\"");
        System.out.println("  java SimpleBedrockClient --model claude --prompt \"Write a poem\" --temperature 0.9");
        System.out.println("  java SimpleBedrockClient --interactive");
        System.out.println("  java SimpleBedrockClient --batch-file prompts.txt");
        System.out.println("\nHeader Matching Examples:");
        System.out.println("  java SimpleBedrockClient --header-matching \\");
        System.out.println("    --standard-headers-file standard.csv \\");
        System.out.println("    --buy-sheet-headers-file buysheet.csv \\");
        System.out.println("    --vsi-mapping-file vsi-mappings.csv");
        System.out.println("\nNote: AWS credentials can be provided via:");
        System.out.println("  - Command line arguments (--access-key, --secret-key)");
        System.out.println("  - Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)");
        System.out.println("  - Configuration file (client-config.properties)");
        System.out.println("  - AWS credentials file or IAM roles");
    }
    
    // Getters
    public String getModelId() { return modelId; }
    public String getPrompt() { return prompt; }
    public String getAwsRegion() { return awsRegion; }
    public String getAccessKeyId() { return accessKeyId; }
    public String getSecretAccessKey() { return secretAccessKey; }
    public Double getTemperature() { return temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public boolean isInteractiveMode() { return interactiveMode; }
    public String getBatchFile() { return batchFile; }
    public boolean shouldSaveResponses() { return saveResponses; }
    public String getOutputDirectory() { return outputDirectory; }
    
    // Header Matching Getters
    public String getOperationMode() { return operationMode; }
    public String[] getStandardHeadersArray() { return standardHeadersArray; }
    public String[] getBuySheetHeadersArray() { return buySheetHeadersArray; }
    public String getStandardHeadersFile() { return standardHeadersFile; }
    public String getBuySheetHeadersFile() { return buySheetHeadersFile; }
    public String getReferenceMappingsFile() { return referenceMappingsFile; }
    public String getIndustryContext() { return industryContext; }
    
    // Helper methods
    public boolean hasPrompt() { return prompt != null && !prompt.trim().isEmpty(); }
    public boolean hasBatchFile() { return batchFile != null && !batchFile.trim().isEmpty(); }
    public boolean hasCredentials() { 
        return accessKeyId != null && secretAccessKey != null && 
               !accessKeyId.trim().isEmpty() && !secretAccessKey.trim().isEmpty(); 
    }
    
    // Header Matching Helper methods
    public boolean isHeaderMatchingMode() { 
        return "HEADER_MATCHING".equals(operationMode); 
    }
    public boolean hasStandardHeadersFile() { 
        return standardHeadersFile != null && !standardHeadersFile.trim().isEmpty(); 
    }
    public boolean hasBuySheetHeadersFile() { 
        return buySheetHeadersFile != null && !buySheetHeadersFile.trim().isEmpty(); 
    }
    public boolean hasReferenceMappingsFile() { 
        return referenceMappingsFile != null && !referenceMappingsFile.trim().isEmpty(); 
    }
    public boolean hasRequiredHeaderMatchingFiles() {
        return hasStandardHeadersFile() && hasBuySheetHeadersFile();
    }
}
