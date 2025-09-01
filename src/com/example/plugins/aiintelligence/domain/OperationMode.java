package com.example.plugins.aiintelligence.domain;

/**
 * Enumeration defining the operation mode for the AI Intelligence service.
 * This service is dedicated to intelligent column header matching.
 */
public enum OperationMode {
    
    /**
     * Column matching mode for intelligent column header matching using existing mappings
     */
    COLUMN_MATCHING("COLUMN_MATCHING");
    
    private final String value;
    
    /**
     * Constructor for the enum values
     * 
     * @param value The string value representing this operation mode
     */
    OperationMode(String value) {
        this.value = value;
    }
    
    /**
     * Gets the string value of this operation mode
     * 
     * @return The string representation of this operation mode
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Converts a string value to the corresponding OperationMode enum
     * 
     * @param value The string value to convert
     * @return The corresponding OperationMode, or null if not found
     */
    public static OperationMode fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (OperationMode mode : OperationMode.values()) {
            if (mode.getValue().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        
        return null;
    }
    
    /**
     * Checks if the given string value represents a valid operation mode
     * 
     * @param value The string value to check
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String value) {
        return fromValue(value) != null;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
