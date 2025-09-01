package com.example.plugins.aiintelligence.domain;

/**
 * Represents a mapping between a source column and a target column.
 * This class encapsulates known good mappings that serve as training data for the AI matching process.
 */
public class ColumnMapping {
    
    private String targetColumn;
    private String sourceColumn;
    private String mappingContext;
    
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
        return targetColumn != null && !targetColumn.trim().isEmpty() &&
               sourceColumn != null && !sourceColumn.trim().isEmpty();
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
