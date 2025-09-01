package com.example.plugins.aiintelligence.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.List;
import java.util.ArrayList;

/**
 * Factory for creating AI provider instances.
 * This class uses the Factory pattern and ServiceLoader to dynamically discover
 * and instantiate AI providers based on configuration.
 */
public class AIProviderFactory {
    
    private static final Map<String, AIProvider> providerCache = new HashMap<>();
    private static final Map<String, Class<? extends AIProvider>> providerClasses = new HashMap<>();
    
    static {
        // Discover available providers using ServiceLoader
        discoverProviders();
    }
    
    /**
     * Discovers available AI providers using ServiceLoader
     */
    private static void discoverProviders() {
        ServiceLoader<AIProvider> loader = ServiceLoader.load(AIProvider.class);
        for (AIProvider provider : loader) {
            String providerId = provider.getProviderId();
            providerClasses.put(providerId, provider.getClass());
            providerCache.put(providerId, provider);
        }
    }
    
    /**
     * Creates an AI provider instance based on the provider ID
     * 
     * @param providerId The unique identifier for the provider
     * @param configuration The configuration for the provider
     * @return An AI provider instance
     * @throws AIProviderException if the provider cannot be created
     */
    public static AIProvider createProvider(String providerId, ProviderConfiguration configuration) 
            throws AIProviderException {
        
        if (providerId == null || providerId.trim().isEmpty()) {
            throw new AIProviderException("Provider ID cannot be null or empty");
        }
        
        // Check if we have a cached instance
        if (providerCache.containsKey(providerId)) {
            AIProvider cachedProvider = providerCache.get(providerId);
            if (cachedProvider.isReady()) {
                return cachedProvider;
            }
        }
        
        // Try to create a new instance
        Class<? extends AIProvider> providerClass = providerClasses.get(providerId);
        if (providerClass == null) {
            throw new AIProviderException("Unknown provider ID: " + providerId);
        }
        
        try {
            AIProvider provider = providerClass.getDeclaredConstructor().newInstance();
            
            // Validate configuration
            if (configuration != null) {
                provider.validateConfiguration(configuration);
            }
            
            // Cache the provider
            providerCache.put(providerId, provider);
            
            return provider;
            
        } catch (Exception e) {
            throw new AIProviderException("Failed to create provider: " + providerId, e);
        }
    }
    
    /**
     * Gets a list of all available provider IDs
     * 
     * @return List of available provider IDs
     */
    public static List<String> getAvailableProviders() {
        return new ArrayList<>(providerClasses.keySet());
    }
    
    /**
     * Checks if a provider is available
     * 
     * @param providerId The provider ID to check
     * @return true if the provider is available, false otherwise
     */
    public static boolean isProviderAvailable(String providerId) {
        return providerClasses.containsKey(providerId);
    }
    
    /**
     * Gets the provider class for a given provider ID
     * 
     * @param providerId The provider ID
     * @return The provider class, or null if not found
     */
    public static Class<? extends AIProvider> getProviderClass(String providerId) {
        return providerClasses.get(providerId);
    }
    
    /**
     * Clears the provider cache
     */
    public static void clearCache() {
        providerCache.clear();
    }
    
    /**
     * Removes a specific provider from the cache
     * 
     * @param providerId The provider ID to remove from cache
     */
    public static void removeFromCache(String providerId) {
        providerCache.remove(providerId);
    }
    
    /**
     * Registers a provider class manually (useful for testing or custom providers)
     * 
     * @param providerId The provider ID
     * @param providerClass The provider class
     */
    public static void registerProvider(String providerId, Class<? extends AIProvider> providerClass) {
        providerClasses.put(providerId, providerClass);
        // Remove from cache to force recreation
        providerCache.remove(providerId);
    }
}
