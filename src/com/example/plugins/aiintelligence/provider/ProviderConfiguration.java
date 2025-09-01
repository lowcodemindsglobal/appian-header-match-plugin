package com.example.plugins.aiintelligence.provider;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for AI providers that contains provider-specific parameters.
 * This class allows for flexible configuration of different AI services.
 */
public class ProviderConfiguration {
    
    private String providerId;
    private String providerName;
    private Map<String, Object> parameters;
    
    /**
     * Default constructor
     */
    public ProviderConfiguration() {
        this.parameters = new HashMap<>();
    }
    
    /**
     * Constructor with provider identification
     * 
     * @param providerId The unique provider identifier
     * @param providerName The display name for the provider
     */
    public ProviderConfiguration(String providerId, String providerName) {
        this();
        this.providerId = providerId;
        this.providerName = providerName;
    }
    
    // Getters and Setters
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
    /**
     * Gets a parameter value by key
     * 
     * @param key The parameter key
     * @return The parameter value, or null if not found
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    /**
     * Gets a parameter value by key with type casting
     * 
     * @param key The parameter key
     * @param type The expected type
     * @param <T> The type parameter
     * @return The parameter value cast to the specified type, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Sets a parameter value
     * 
     * @param key The parameter key
     * @param value The parameter value
     */
    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }
    
    /**
     * Removes a parameter
     * 
     * @param key The parameter key to remove
     */
    public void removeParameter(String key) {
        parameters.remove(key);
    }
    
    /**
     * Checks if a parameter exists
     * 
     * @param key The parameter key to check
     * @return true if the parameter exists, false otherwise
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }
    
    /**
     * Gets all parameters as a map
     * 
     * @return A copy of the parameters map
     */
    public Map<String, Object> getAllParameters() {
        return new HashMap<>(parameters);
    }
    
    /**
     * Sets multiple parameters at once
     * 
     * @param parameters The parameters to set
     */
    public void setParameters(Map<String, Object> parameters) {
        if (parameters != null) {
            this.parameters.putAll(parameters);
        }
    }
    
    /**
     * Clears all parameters
     */
    public void clearParameters() {
        parameters.clear();
    }
    
    /**
     * Validates the configuration
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return providerId != null && !providerId.trim().isEmpty() &&
               providerName != null && !providerName.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("ProviderConfiguration{providerId='%s', providerName='%s', parameters=%s}", 
                           providerId, providerName, parameters);
    }
}
