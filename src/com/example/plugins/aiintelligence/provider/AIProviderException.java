package com.example.plugins.aiintelligence.provider;

/**
 * Exception thrown when errors occur in AI provider operations.
 * This exception provides detailed information about what went wrong
 * and can be used for error handling and logging.
 */
public class AIProviderException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    private final String providerId;
    private final String operation;
    
    /**
     * Constructor with message
     * 
     * @param message The error message
     */
    public AIProviderException(String message) {
        super(message);
        this.providerId = null;
        this.operation = null;
    }
    
    /**
     * Constructor with message and cause
     * 
     * @param message The error message
     * @param cause The underlying cause
     */
    public AIProviderException(String message, Throwable cause) {
        super(message, cause);
        this.providerId = null;
        this.operation = null;
    }
    
    /**
     * Constructor with provider context
     * 
     * @param message The error message
     * @param providerId The ID of the provider that caused the error
     * @param operation The operation that was being performed
     */
    public AIProviderException(String message, String providerId, String operation) {
        super(String.format("Provider %s failed during %s: %s", providerId, operation, message));
        this.providerId = providerId;
        this.operation = operation;
    }
    
    /**
     * Constructor with provider context and cause
     * 
     * @param message The error message
     * @param providerId The ID of the provider that caused the error
     * @param operation The operation that was being performed
     * @param cause The underlying cause
     */
    public AIProviderException(String message, String providerId, String operation, Throwable cause) {
        super(String.format("Provider %s failed during %s: %s", providerId, operation, message), cause);
        this.providerId = providerId;
        this.operation = operation;
    }
    
    /**
     * Gets the provider ID that caused the error
     * 
     * @return The provider ID, or null if not specified
     */
    public String getProviderId() {
        return providerId;
    }
    
    /**
     * Gets the operation that was being performed when the error occurred
     * 
     * @return The operation name, or null if not specified
     */
    public String getOperation() {
        return operation;
    }
    
    /**
     * Creates a formatted error message with context
     * 
     * @return A formatted error message
     */
    public String getFormattedMessage() {
        if (providerId != null && operation != null) {
            return String.format("[%s] %s: %s", providerId, operation, getMessage());
        } else if (providerId != null) {
            return String.format("[%s] %s", providerId, getMessage());
        } else {
            return getMessage();
        }
    }
}
