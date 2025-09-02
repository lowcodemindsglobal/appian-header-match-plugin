package com.example.client;

import com.example.plugins.aiintelligence.AIIntelligenceService;
import com.example.plugins.aiintelligence.domain.ColumnMapping;
import com.example.plugins.aiintelligence.domain.ColumnMatchingResult;
import java.util.Scanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Simple console client for testing AI Intelligence Plugin
 * Tests the plugin using OpenAI provider for column header matching
 */
public class AIIntelligenceClient {
    
    private AIIntelligenceService aiService;
    private ClientConfiguration config;
    private ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    public static void main(String[] args) {
        AIIntelligenceClient client = new AIIntelligenceClient();
        client.run(args);
    }
    
    public void run(String[] args) {
        try {
            System.out.println("===============================================");
            System.out.println("AI Intelligence Plugin - Test Client");
            System.out.println("===============================================");
            System.out.println();
            
            // Initialize components
            config = ClientConfiguration.loadConfiguration(args);
            initializePlugin();
            
            // Display configuration
            printConfiguration();
            
            // Execute column matching
            if (config.hasRequiredHeaderMatchingFiles()) {
                executeColumnMatching();
            } else {
                System.out.println("ERROR: Required files not found!");
                System.out.println("Please provide:");
                System.out.println("  --standard-headers-file <file>");
                System.out.println("  --buy-sheet-headers-file <file>");
                System.out.println("  --existing-mappings-file <file> (optional)");
                System.out.println();
                System.out.println("Using files from client/data/input/ folder");
                System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: Client Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializePlugin() {
        aiService = new AIIntelligenceService();
        
        // Check if we should use AWS Bedrock or OpenAI
        String provider = System.getenv("AI_PROVIDER");
        if (provider == null || provider.isEmpty()) {
            provider = "openai"; // Default to OpenAI
        }
        
        if ("aws-bedrock".equalsIgnoreCase(provider)) {
            // Configure AWS Bedrock provider
            aiService.setProviderId("aws-bedrock");
            aiService.setProviderName("AWS Bedrock");
            aiService.setModelId("arn:aws:bedrock:us-west-2:211125498297:application-inference-profile/hx2e1juc8tej");
            
            // Set AWS provider parameters
            String[] providerKeys = {"region", "accessKeyId", "secretAccessKey"};
            String[] providerValues = {
                System.getenv("AWS_DEFAULT_REGION") != null ? System.getenv("AWS_DEFAULT_REGION") : "us-west-2",
                System.getenv("AWS_ACCESS_KEY_ID"),
                System.getenv("AWS_SECRET_ACCESS_KEY")
            };
            aiService.setProviderParameterKeys(providerKeys);
            aiService.setProviderParameterValues(providerValues);
            
        } else {
            // Configure OpenAI provider (default)
            aiService.setProviderId("openai");
            aiService.setProviderName("OpenAI");
            aiService.setModelId("gpt-4");
            
            // Set provider parameters (OpenAI API key will be set via environment variable)
            String[] providerKeys = {"apiKey"};
            String[] providerValues = {System.getenv("OPENAI_API_KEY")};
            aiService.setProviderParameterKeys(providerKeys);
            aiService.setProviderParameterValues(providerValues);
        }
        
        // Set model parameters
        aiService.setTemperature(0.3);
        aiService.setMaxTokens(1000);
        aiService.setTopP(0.9);
        aiService.setTopK(40);
        
        // Operation mode is automatically set to COLUMN_MATCHING by the service
    }
    
    private void printConfiguration() {
        System.out.println("�� Configuration:");
        
        // Determine which provider is being used based on environment variable
        String provider = System.getenv("AI_PROVIDER");
        if ("aws-bedrock".equalsIgnoreCase(provider)) {
            System.out.println("  Provider: AWS Bedrock");
            System.out.println("  Model: Application Inference Profile (Llama 4 Scout 17B)");
            System.out.println("  ARN: arn:aws:bedrock:us-west-2:211125498297:application-inference-profile/hx2e1juc8tej");
        } else {
            System.out.println("  Provider: OpenAI");
            System.out.println("  Model: GPT-4");
        }
        
        System.out.println("  Temperature: 0.3");
        System.out.println("  Max Tokens: 1000");
        System.out.println("  Standard Headers: " + config.getStandardHeadersFile());
        System.out.println("  Buy Sheet Headers: " + config.getBuySheetHeadersFile());
        System.out.println("  Existing Mappings: " + config.getReferenceMappingsFile());
        System.out.println();
    }
    
    private void executeColumnMatching() {
        try {
            System.out.println("🚀 Executing Column Matching with AI Intelligence Plugin...");
            System.out.println();
            
            // Load data from files
            String[] targetHeaders = loadHeadersFromFile(config.getStandardHeadersFile());
            String[] sourceHeaders = loadHeadersFromFile(config.getBuySheetHeadersFile());
            List<ColumnMapping> existingMappings = loadExistingMappings(config.getReferenceMappingsFile());
            
            // Set service inputs
            aiService.setTargetHeaders(targetHeaders);
            aiService.setSourceHeaders(sourceHeaders);
            
            // Convert existing mappings to JSON string
            String existingMappingsJson = objectMapper.writeValueAsString(existingMappings);
            aiService.setExistingMappingsJson(existingMappingsJson);
            
            aiService.setIndustryContext("Financial Services - SIBS Integration");
            
            // Execute the service
            aiService.run();
            
            // Get results
            String resultsJson = aiService.getColumnMatchingResultsJson();
            List<ColumnMatchingResult> results = objectMapper.readValue(resultsJson, new TypeReference<List<ColumnMatchingResult>>() {});
            
            // Display results
            displayResults(results.toArray(new ColumnMatchingResult[0]));
            
        } catch (Exception e) {
            System.err.println("ERROR: Error during column matching: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String[] loadHeadersFromFile(String filePath) throws IOException {
        // Resolve the path relative to the current working directory
        Path resolvedPath = Paths.get(filePath).toAbsolutePath().normalize();
        System.out.println("Loading headers from: " + resolvedPath);
        
        if (!Files.exists(resolvedPath)) {
            throw new IOException("File not found: " + resolvedPath);
        }
        
        List<String> headers = Files.readAllLines(resolvedPath);
        if (headers.isEmpty()) {
            throw new IOException("File is empty: " + resolvedPath);
        }
        
        // Skip header row if it exists
        String firstLine = headers.get(0);
        if (firstLine.contains(",")) {
            // This looks like a CSV header, extract column names
            String[] columns = firstLine.split(",");
            String[] result = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                result[i] = columns[i].trim();
            }
            return result;
        } else {
            // Single column file
            return headers.toArray(new String[0]);
        }
    }
    
    private List<ColumnMapping> loadExistingMappings(String filePath) throws IOException {
        List<ColumnMapping> mappings = new ArrayList<>();
        
        if (filePath == null || filePath.trim().isEmpty()) {
            return mappings;
        }
        
        // Resolve the path relative to the current working directory
        Path resolvedPath = Paths.get(filePath).toAbsolutePath().normalize();
        System.out.println("Loading existing mappings from: " + resolvedPath);
        
        if (!Files.exists(resolvedPath)) {
            System.out.println("Warning: Existing mappings file not found: " + resolvedPath);
            return mappings;
        }
        
        List<String> lines = Files.readAllLines(resolvedPath);
        if (lines.isEmpty()) {
            return mappings;
        }
        
        // Skip header row
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) continue;
            
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                ColumnMapping mapping = new ColumnMapping();
                mapping.setTargetColumn(parts[0].trim());
                mapping.setSourceColumn(parts[1].trim());
                if (parts.length >= 3) {
                    mapping.setMappingContext(parts[2].trim());
                }
                mappings.add(mapping);
            }
        }
        
        return mappings;
    }
    
    private void displayResults(ColumnMatchingResult[] results) {
        System.out.println("✅ Column Matching Results:");
        System.out.println("==========================");
        System.out.println();
        
        if (results == null || results.length == 0) {
            System.out.println("No results returned");
            return;
        }
        
        for (int i = 0; i < results.length; i++) {
            ColumnMatchingResult result = results[i];
            System.out.println("Result " + (i + 1) + ":");
            System.out.println("  Source Header: " + result.getSourceHeader());
            System.out.println("  Matched Target: " + result.getMatchedTargetHeader());
            System.out.println("  Confidence: " + result.getConfidencePercentage() + "%");
            System.out.println("  Used Existing Mapping: " + result.getUsedExistingMapping());
            System.out.println("  Reasoning: " + result.getReasoning());
            System.out.println();
        }
        
        System.out.println("🎯 Summary:");
        System.out.println("  Total Headers Processed: " + results.length);
        System.out.println("  Existing Mappings Used: " + countExistingMappingsUsed(results));
        System.out.println("  Average Confidence: " + calculateAverageConfidence(results) + "%");
    }
    
    private int countExistingMappingsUsed(ColumnMatchingResult[] results) {
        int count = 0;
        for (ColumnMatchingResult result : results) {
            if (result.getUsedExistingMapping() != null && result.getUsedExistingMapping()) {
                count++;
            }
        }
        return count;
    }
    
    private double calculateAverageConfidence(ColumnMatchingResult[] results) {
        if (results.length == 0) return 0.0;
        
        double total = 0.0;
        for (ColumnMatchingResult result : results) {
            if (result.getConfidencePercentage() != null) {
                total += result.getConfidencePercentage();
            }
        }
        return Math.round((total / results.length) * 100.0) / 100.0;
    }
}
