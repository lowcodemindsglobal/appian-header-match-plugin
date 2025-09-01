package com.example.plugins.aiintelligence.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for loading configuration from config.properties file
 */
public class ConfigurationLoader {
    
    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties;
    
    static {
        loadConfiguration();
    }
    
    private static void loadConfiguration() {
        properties = new Properties();
        try {
            // Try to load from current directory first
            try (InputStream input = new FileInputStream(CONFIG_FILE)) {
                properties.load(input);
            } catch (IOException e) {
                // If not found in current directory, try classpath
                try (InputStream input = ConfigurationLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                    if (input != null) {
                        properties.load(input);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load config.properties: " + e.getMessage());
        }
    }
    
    /**
     * Get a configuration value as String
     */
    public static String getString(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get a configuration value as String
     */
    public static String getString(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Get a configuration value as Integer
     */
    public static Integer getInteger(String key, Integer defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid integer value for " + key + ": " + value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Get a configuration value as Double
     */
    public static Double getDouble(String key, Double defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid double value for " + key + ": " + value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Get a configuration value as Boolean
     */
    public static Boolean getBoolean(String key, Boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    /**
     * Reload configuration from file
     */
    public static void reload() {
        loadConfiguration();
    }
}
