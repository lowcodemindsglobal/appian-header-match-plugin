package com.example.plugins.aiintelligence.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating AI provider instances.
 * This class uses the Factory pattern and ServiceLoader to dynamically discover
 * and instantiate AI providers based on configuration.
 */
public class AIProviderFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(AIProviderFactory.class);
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
        logger.info("Starting AI provider discovery using ServiceLoader");
        try {
            ServiceLoader<AIProvider> loader = ServiceLoader.load(AIProvider.class);
            int providerCount = 0;
            for (AIProvider provider : loader) {
                String providerId = provider.getProviderId();
                providerClasses.put(providerId, provider.getClass());
                providerCache.put(providerId, provider);
                providerCount++;
                logger.info("Discovered AI provider: {} (class: {})", providerId, provider.getClass().getSimpleName());
            }
            logger.info("AI provider discovery completed. Found {} providers: {}", providerCount, providerClasses.keySet());
        } catch (Exception e) {
            logger.error("Error during AI provider discovery", e);
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
        
        logger.info("Creating AI provider: {} with configuration: {}", providerId, configuration);
        
        if (providerId == null || providerId.trim().isEmpty()) {
            logger.error("Provider ID cannot be null or empty");
            throw new AIProviderException("Provider ID cannot be null or empty");
        }
        
        // Check if we have a cached instance
        if (providerCache.containsKey(providerId)) {
            AIProvider cachedProvider = providerCache.get(providerId);
            if (cachedProvider.isReady()) {
                logger.info("Returning cached provider instance for: {}", providerId);
                return cachedProvider;
            } else {
                logger.warn("Cached provider {} is not ready, creating new instance", providerId);
            }
        }
        
        // Try to create a new instance
        Class<? extends AIProvider> providerClass = providerClasses.get(providerId);
        if (providerClass == null) {
            logger.error("Unknown provider ID: {}. Available providers: {}", providerId, providerClasses.keySet());
            throw new AIProviderException("Unknown provider ID: " + providerId);
        }
        
        try {
            logger.debug("Instantiating provider class: {}", providerClass.getName());
            AIProvider provider = providerClass.getDeclaredConstructor().newInstance();
            
            // Validate configuration
            if (configuration != null) {
                logger.debug("Validating configuration for provider: {}", providerId);
                provider.validateConfiguration(configuration);
                logger.info("Configuration validation successful for provider: {}", providerId);
            } else {
                logger.warn("No configuration provided for provider: {}", providerId);
            }
            
            // Cache the provider
            providerCache.put(providerId, provider);
            logger.info("Successfully created and cached provider: {}", providerId);
            
            return provider;
            
        } catch (Exception e) {
            logger.error("Failed to create provider: {}", providerId, e);
            throw new AIProviderException("Failed to create provider: " + providerId, e);
        }
    }
    
    /**
     * Gets a list of all available provider IDs
     * 
     * @return List of available provider IDs
     */
    public static List<String> getAvailableProviders() {
        logger.debug("Getting available providers: {}", providerClasses.keySet());
        return new ArrayList<>(providerClasses.keySet());
    }
    
    /**
     * Checks if a provider is available
     * 
     * @param providerId The provider ID to check
     * @return true if the provider is available, false otherwise
     */
    public static boolean isProviderAvailable(String providerId) {
        boolean available = providerClasses.containsKey(providerId);
        logger.debug("Provider {} availability: {}", providerId, available);
        return available;
    }
    
    /**
     * Gets the provider class for a given provider ID
     * 
     * @param providerId The provider ID
     * @return The provider class, or null if not found
     */
    public static Class<? extends AIProvider> getProviderClass(String providerId) {
        Class<? extends AIProvider> providerClass = providerClasses.get(providerId);
        logger.debug("Provider class for {}: {}", providerId, providerClass != null ? providerClass.getName() : "null");
        return providerClass;
    }
    
    /**
     * Clears the provider cache
     */
    public static void clearCache() {
        logger.info("Clearing provider cache. Current cache size: {}", providerCache.size());
        providerCache.clear();
        logger.info("Provider cache cleared");
    }
    
    /**
     * Removes a specific provider from the cache
     * 
     * @param providerId The provider ID to remove from cache
     */
    public static void removeFromCache(String providerId) {
        logger.info("Removing provider {} from cache", providerId);
        AIProvider removed = providerCache.remove(providerId);
        if (removed != null) {
            logger.info("Successfully removed provider {} from cache", providerId);
        } else {
            logger.warn("Provider {} was not found in cache", providerId);
        }
    }
    
    /**
     * Registers a provider class manually (useful for testing or custom providers)
     * 
     * @param providerId The provider ID
     * @param providerClass The provider class
     */
    public static void registerProvider(String providerId, Class<? extends AIProvider> providerClass) {
        logger.info("Manually registering provider: {} with class: {}", providerId, providerClass.getName());
        providerClasses.put(providerId, providerClass);
        // Remove from cache to force recreation
        providerCache.remove(providerId);
        logger.info("Provider {} registered and removed from cache", providerId);
    }
}
