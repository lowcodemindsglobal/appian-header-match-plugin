package com.lcm.plugins.aiintelligence.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a mapping between a source column and a target column.
 * This class encapsulates known good mappings that serve as training data for the AI matching process.
 */
public class ColumnMapping {
    
    private static final Logger logger = LoggerFactory.getLogger(ColumnMapping.class);
    
    private String targetColumn;
    private String sourceColumn;
    private String mappingContext;
    private Boolean valid;
    
    /**
     * Default constructor for JSON deserialization
     */
    public ColumnMapping() {
    }
    
    /**
     * Constructor for creating a column mapping
     * 
     * @param targetColumn The target (standard) column name
     * @param sourceColumn The source column name
     */
    public ColumnMapping(String targetColumn, String sourceColumn) {
        this.targetColumn = targetColumn;
        this.sourceColumn = sourceColumn;
        logger.debug("Created ColumnMapping: {} -> {}", sourceColumn, targetColumn);
    }
    
    /**
     * Constructor for creating a column mapping with context
     * 
     * @param targetColumn The target (standard) column name
     * @param sourceColumn The source column name
     * @param mappingContext Additional context about this mapping
     */
    public ColumnMapping(String targetColumn, String sourceColumn, String mappingContext) {
        this.targetColumn = targetColumn;
        this.sourceColumn = sourceColumn;
        this.mappingContext = mappingContext;
        logger.debug("Created ColumnMapping with context: {} -> {} ({})", sourceColumn, targetColumn, mappingContext);
    }
    
    // Getters and Setters
    
    public String getTargetColumn() {
        return targetColumn;
    }
    
    public void setTargetColumn(String targetColumn) {
        this.targetColumn = targetColumn;
    }
    
    public String getSourceColumn() {
        return sourceColumn;
    }
    
    public void setSourceColumn(String sourceColumn) {
        this.sourceColumn = sourceColumn;
    }
    
    public String getMappingContext() {
        return mappingContext;
    }
    
    public void setMappingContext(String mappingContext) {
        this.mappingContext = mappingContext;
    }
    
    public Boolean getValid() {
        return valid;
    }
    
    public void setValid(Boolean valid) {
        this.valid = valid;
    }
    
    @Override
    public String toString() {
        return String.format("ColumnMapping{targetColumn='%s', sourceColumn='%s', mappingContext='%s'}", 
                           targetColumn, sourceColumn, mappingContext);
    }
    
    /**
     * Validates that this column mapping contains required fields
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        boolean isValid = targetColumn != null && !targetColumn.trim().isEmpty() &&
               sourceColumn != null && !sourceColumn.trim().isEmpty() &&
               (valid == null || valid); // Consider valid if not set or explicitly true
        
        if (!isValid) {
            logger.debug("ColumnMapping validation failed: targetColumn='{}', sourceColumn='{}', valid={}", 
                        targetColumn, sourceColumn, valid);
        }
        
        return isValid;
    }
    
    /**
     * Checks if this mapping matches a given source header
     * (case-insensitive comparison)
     * 
     * @param sourceHeader The source header to check
     * @return true if this mapping's source column matches the header
     */
    public boolean matchesSourceHeader(String sourceHeader) {
        if (sourceHeader == null || sourceColumn == null) {
            return false;
        }
        return sourceColumn.trim().equalsIgnoreCase(sourceHeader.trim());
    }
    
    /**
     * Gets the target column for a given source header if it matches
     * 
     * @param sourceHeader The source header to look up
     * @return The target column if matched, null otherwise
     */
    public String getTargetColumnForHeader(String sourceHeader) {
        return matchesSourceHeader(sourceHeader) ? targetColumn : null;
    }
    
    /**
     * Creates a formatted string for use in AI prompts
     * 
     * @return A formatted string describing this column mapping
     */
    public String toPromptFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(sourceColumn).append("\" → \"").append(targetColumn).append("\"");
        if (mappingContext != null && !mappingContext.trim().isEmpty()) {
            sb.append(" (").append(mappingContext).append(")");
        }
        return sb.toString();
    }
    
    /**
     * Checks if two ColumnMapping objects are equivalent
     * 
     * @param other The other mapping to compare
     * @return true if they represent the same mapping
     */
    public boolean isEquivalent(ColumnMapping other) {
        if (other == null) {
            return false;
        }
        
        return this.targetColumn != null && this.targetColumn.equalsIgnoreCase(other.targetColumn) &&
               this.sourceColumn != null && this.sourceColumn.equalsIgnoreCase(other.sourceColumn);
    }
}
