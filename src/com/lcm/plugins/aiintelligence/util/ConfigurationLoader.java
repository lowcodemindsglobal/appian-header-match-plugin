package com.lcm.plugins.aiintelligence.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for loading configuration from config.properties file
 */
public class ConfigurationLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLoader.class);
    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties;
    
    static {
        loadConfiguration();
    }
    
    private static void loadConfiguration() {
        logger.info("Loading configuration from: {}", CONFIG_FILE);
        properties = new Properties();
        try {
            // Try to load from classpath first (most reliable for deployed plugins)
            try (InputStream input = ConfigurationLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (input != null) {
                    properties.load(input);
                    logger.info("Successfully loaded configuration from classpath: {}", CONFIG_FILE);
                } else {
                    logger.debug("Configuration file not found in classpath: {}", CONFIG_FILE);
                    // If not found in classpath, try current directory (for development)
                    try (InputStream fileInput = new FileInputStream(CONFIG_FILE)) {
                        properties.load(fileInput);
                        logger.info("Successfully loaded configuration from current directory: {}", CONFIG_FILE);
                    } catch (IOException e) {
                        logger.warn("Could not load config from current directory: {}", e.getMessage());
                    }
                }
            }
            logger.info("Configuration loaded with {} properties", properties.size());
            if (logger.isDebugEnabled()) {
                logger.debug("Loaded configuration properties: {}", properties.keySet());
            }
        } catch (IOException e) {
            logger.error("Failed to load configuration file: {}", CONFIG_FILE, e);
        }
    }
    
    /**
     * Get a configuration value as String
     */
    public static String getString(String key, String defaultValue) {
        String value = properties.getProperty(key);
        String result = value != null ? value : defaultValue;
        logger.debug("Configuration getString({}, {}) = {}", key, defaultValue, result);
        return result;
    }
    
    /**
     * Get a configuration value as String
     */
    public static String getString(String key) {
        String value = properties.getProperty(key);
        logger.debug("Configuration getString({}) = {}", key, value);
        return value;
    }
    
    /**
     * Get a configuration value as Integer
     */
    public static Integer getInteger(String key, Integer defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                Integer result = Integer.parseInt(value);
                logger.debug("Configuration getInteger({}, {}) = {}", key, defaultValue, result);
                return result;
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for {}: {}", key, value);
            }
        }
        logger.debug("Configuration getInteger({}, {}) = {} (default)", key, defaultValue, defaultValue);
        return defaultValue;
    }
    
    /**
     * Get a configuration value as Double
     */
    public static Double getDouble(String key, Double defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                Double result = Double.parseDouble(value);
                logger.debug("Configuration getDouble({}, {}) = {}", key, defaultValue, result);
                return result;
            } catch (NumberFormatException e) {
                logger.warn("Invalid double value for {}: {}", key, value);
            }
        }
        logger.debug("Configuration getDouble({}, {}) = {} (default)", key, defaultValue, defaultValue);
        return defaultValue;
    }
    
    /**
     * Get a configuration value as Boolean
     */
    public static Boolean getBoolean(String key, Boolean defaultValue) {
        String value = properties.getProperty(key);
        Boolean result = value != null ? Boolean.parseBoolean(value) : defaultValue;
        logger.debug("Configuration getBoolean({}, {}) = {}", key, defaultValue, result);
        return result;
    }
    
    /**
     * Reload configuration from file
     */
    public static void reload() {
        logger.info("Reloading configuration from file");
        loadConfiguration();
    }
}
