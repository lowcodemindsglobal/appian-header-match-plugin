# AI Intelligence Plugin

A configurable, LLM-agnostic Appian plugin for intelligent column header matching using AI providers.

## 🚀 **Current Status: Production Ready**

**✅ Latest Updates (v1.0.0):**
- **OpenAI Integration**: Fully functional with 2-minute timeout support
- **Robust JSON Parsing**: Handles truncated responses gracefully
- **Timeout Optimization**: Resolved network timeout issues for complex operations
- **Production Testing**: Successfully tested with real CSV data and column matching

## Overview

This plugin provides a flexible, extensible architecture for AI-powered column header matching that can work with multiple AI providers including:
- **OpenAI** ✅ (Fully tested and working)
- **AWS Bedrock** ✅ (Ready for production)
- **Appian LL** (future)
- **Any other AI service** (easily extensible)

## Architecture

The plugin uses several software engineering design patterns to achieve LLM-agnostic functionality:

### 1. Strategy Pattern
- **AIProvider Interface**: Defines the contract for all AI providers
- **Provider Implementations**: Concrete implementations for different AI services
- **Runtime Selection**: Providers can be selected at runtime based on configuration

### 2. Factory Pattern
- **AIProviderFactory**: Creates provider instances based on configuration
- **ServiceLoader Integration**: Automatically discovers available providers
- **Caching**: Maintains provider instances for performance

### 3. Template Method Pattern
- **AbstractAIProvider**: Provides common functionality and defines the algorithm structure
- **Provider-Specific Implementation**: Subclasses implement only the provider-specific details
- **Consistent Behavior**: All providers follow the same workflow

### 4. Configuration-Driven Design
- **ProviderConfiguration**: Flexible configuration for provider-specific parameters
- **ModelConfiguration**: Standard AI model parameters across all providers
- **Dynamic Configuration**: No hardcoded provider dependencies

### 5. Utility Layer
- **ConfigurationLoader**: Centralized configuration management
- **Path Resolution**: Cross-platform file path handling
- **Environment Integration**: Support for environment variables and system properties

## Key Benefits

1. **Provider Agnostic**: Switch between AI providers without code changes
2. **Extensible**: Easy to add new AI providers
3. **Consistent Interface**: Same API regardless of underlying AI service
4. **Configuration Driven**: No recompilation needed to change providers
5. **Maintainable**: Clean separation of concerns and well-defined interfaces
6. **Testable**: Easy to mock and test individual components
7. **Production Ready**: Robust error handling and timeout management
8. **Resilient**: Handles network issues and truncated responses gracefully
9. **Configurable**: Centralized configuration management with environment variable support

## Recent Improvements (v1.0.0)

### 🔧 **Timeout Optimization**
- **OpenAI Provider**: Extended timeout to 2 minutes for complex column matching operations
- **Network Resilience**: Handles slow network conditions and large AI processing requests
- **User Experience**: No more timeout errors during complex operations

### 🔐 **Security Improvements**
- **Removed Hardcoded API Keys**: No more hardcoded credentials in scripts
- **Configuration-Based Setup**: API keys now loaded from `config.properties` or environment variables
- **Template Configuration**: Added `config.properties.example` for easy setup
- **Git Security**: `.gitignore` already excludes sensitive configuration files

### 🛡️ **Robust JSON Parsing**
- **Truncated Response Handling**: Automatically handles responses that get cut off
- **Fallback Mechanisms**: Parses partial results when complete JSON is unavailable
- **Error Recovery**: Gracefully handles malformed or incomplete AI responses

### 🧪 **Enhanced Testing & Validation**
- **Comprehensive Testing**: Full end-to-end testing with real CSV data
- **Error Simulation**: Tested with various error conditions and edge cases
- **Performance Validation**: Verified timeout settings and response handling

### ⚙️ **Configuration System**
- **Centralized Configuration**: Single `config.properties` file for all settings
- **Environment Variable Support**: Flexible configuration via environment variables
- **Cross-Platform Paths**: Robust file path handling across operating systems
- **Build Integration**: Seamless integration with Maven build process

## Project Structure

```
appian-header-match-plugin/                    # Root project directory
├── src/main/java/com/example/plugins/aiintelligence/
│   ├── domain/                          # Domain models
│   │   ├── ColumnMapping.java           # Column mapping entity
│   │   ├── ColumnMatchingResult.java    # Matching result entity
│   │   └── OperationMode.java           # Operation modes enum
│   ├── provider/                        # AI provider abstraction
│   │   ├── AIProvider.java              # Provider interface
│   │   ├── AbstractAIProvider.java      # Abstract base class
│   │   ├── AIProviderFactory.java       # Provider factory
│   │   ├── AIProviderException.java     # Custom exceptions
│   │   ├── ModelConfiguration.java      # Model configuration
│   │   ├── ProviderConfiguration.java   # Provider configuration
│   │   └── impl/                        # Provider implementations
│   │       ├── AWSBedrockProvider.java  # AWS Bedrock implementation
│   │       └── OpenAIProvider.java      # OpenAI implementation (with timeout fixes)
│   ├── util/                            # Utility classes
│   │   └── ConfigurationLoader.java     # Configuration loading utilities
│   └── AIIntelligenceService.java       # Main Appian service
├── src/main/resources/
│   ├── appian-plugin.xml                # Appian plugin configuration
│   └── META-INF/services/               # Service loader configuration
├── client/                              # Test client for development
│   ├── src/main/java/                   # Test client source code
│   ├── data/input/                      # Sample CSV files for testing
│   │   ├── sibs.csv                     # Standard SIBS headers
│   │   ├── VS - OCC Extract MAS -NEW.csv # Buy sheet headers
│   │   └── VSI mapping.csv              # VSI mapping data
│   └── pom.xml                          # Maven configuration
├── lib/                                 # Appian SDK and dependencies
│   └── appian-plug-in-sdk.jar          # Appian Plugin SDK
├── config.properties                    # Central configuration file
├── build-plugin-and-client.bat          # Build script for Windows
├── test-client.bat                      # Test script for Windows
├── CONFIGURATION.md                     # Configuration documentation
├── .gitignore                           # Git ignore rules
└── pom.xml                              # Maven configuration
```

## Appian Setup and Installation

### Prerequisites

1. **Appian Environment**: Appian 23.1 or later
2. **Java Runtime**: Java 11 or later
3. **Plugin JAR**: The compiled `ai-intelligence-plugin-1.0.0.jar` file
4. **AI Provider Credentials**: API keys and credentials for your chosen AI provider

### Installation Steps

#### Step 1: Deploy the Plugin

1. **Access Appian Admin Console**
   - Log into your Appian environment as an administrator
   - Navigate to **Admin Console** → **Plugins**

2. **Upload Plugin JAR**
   - Click **Upload Plugin**
   - Select the `ai-intelligence-plugin-1.0.0.jar` file
   - Click **Upload**

3. **Verify Installation**
   - The plugin should appear in the plugins list
   - Status should show as "Active"
   - Note the plugin ID for future reference

#### Step 2: Configure AI Provider Credentials

##### For AWS Bedrock:
1. **Create AWS IAM User** (if not exists)
   - Access to AWS Bedrock services
   - Appropriate permissions for model invocation

2. **Configure AWS Credentials in Appian**
   - Navigate to **Admin Console** → **System Properties**
   - Add the following properties:
     ```
     ai.provider.aws.region=us-east-1
     ai.provider.aws.accessKeyId=YOUR_ACCESS_KEY
     ai.provider.aws.secretAccessKey=YOUR_SECRET_KEY
     ```

##### For OpenAI:
1. **Get OpenAI API Key**
   - Visit [OpenAI Platform](https://platform.openai.com/api-keys)
   - Create a new API key

2. **Configure OpenAI Credentials in Appian**
   - Navigate to **Admin Console** → **System Properties**
   - Add the following property:
     ```
     ai.provider.openai.apiKey=YOUR_OPENAI_API_KEY
     ```

#### Step 3: Configure Appian Process Model

1. **Create Process Model**
   - Navigate to **Design** → **Process Models**
   - Create a new process model or use existing one

2. **Add AI Intelligence Service**
   - Drag and drop **AI Intelligence Service** from the palette
   - Configure the service inputs (see Configuration section below)

3. **Configure Service Inputs**
   - Set the provider ID (e.g., "aws-bedrock" or "openai")
   - Configure model parameters
   - Set up input data sources

## Usage

### Configuration Setup

Before using the plugin, ensure your configuration is properly set up:

1. **Copy Configuration Template**: 
   ```bash
   cp config.properties.example config.properties
   ```

2. **Configure API Keys**: Edit `config.properties` and add your actual API keys:
   - `openai.api.key`: Your OpenAI API key
   - `aws.access.key.id`: Your AWS access key (if using AWS Bedrock)
   - `aws.secret.access.key`: Your AWS secret key (if using AWS Bedrock)

3. **Security Note**: Never commit `config.properties` to version control. The `.gitignore` file already excludes it.

4. **Alternative**: You can also set the `OPENAI_API_KEY` environment variable instead of editing the config file.

5. **Verify File Paths**: Ensure all referenced files exist and are accessible

For detailed configuration information, see `CONFIGURATION.md`.

### Basic Usage

1. **Configure Provider**: Set the provider ID and required parameters
2. **Configure Model**: Set the AI model and parameters
3. **Provide Data**: Input source headers, target headers, and existing mappings
4. **Execute**: Run the service to get intelligent column matching results

### Example Configuration

#### AWS Bedrock Provider
```java
// Provider Configuration
Provider ID: "aws-bedrock"
Provider Parameter Keys: ["region", "accessKeyId", "secretAccessKey"]
Provider Parameter Values: ["us-east-1", "AKIA...", "secret..."]

// Model Configuration
Model ID: "anthropic.claude-3-sonnet-20240229-v1:0"
Temperature: 0.3
Max Tokens: 4000
```

#### OpenAI Provider (Recommended)
```java
// Provider Configuration
Provider ID: "openai"
Provider Parameter Keys: ["apiKey"]
Provider Parameter Values: ["sk-..."]

// Model Configuration
Model ID: "gpt-4"
Temperature: 0.3
Max Tokens: 1000
```

**Note**: OpenAI provider now includes optimized timeout settings (2-minute read timeout) for reliable operation with complex column matching tasks.

### Detailed Usage Guide

#### Service Configuration

1. **Provider Configuration**
   - `Provider ID`: The AI provider to use ("aws-bedrock" or "openai")
   - `Model ID`: Specific model from the provider (e.g., "gpt-4", "anthropic.claude-v2")

2. **Provider Parameters**
   - **AWS Bedrock**:
     - `region`: AWS region (e.g., "us-east-1", "us-west-2")
     - `accessKeyId`: AWS access key ID
     - `secretAccessKey`: AWS secret access key
   
   - **OpenAI**:
     - `apiKey`: OpenAI API key

3. **Model Configuration**
   - `temperature`: Controls randomness (0.0 = deterministic, 1.0 = creative)
   - `maxTokens`: Maximum response length
   - `topP`: Nucleus sampling parameter
   - `topK`: Top-k sampling parameter

4. **Data Inputs**
   - `Source Headers`: Array of source column headers to match
   - `Target Headers`: Array of target column headers to match against
   - `Existing Mappings`: Previous mappings for training/context
   - `Industry Context`: Business context for better matching

#### Output Parameters

1. **Column Matching Results**: Array of matching results with confidence scores
2. **Statistics**: Summary of the matching process
3. **Processing Time**: Time taken for the operation

### Example Process Flow

#### Scenario: Matching CSV Headers

1. **Data Preparation**
   ```
   Source Headers: ["Customer Name", "Product ID", "Order Date"]
   Target Headers: ["Client Name", "Item ID", "Purchase Date"]
   Existing Mappings: [{"source": "Customer Name", "target": "Client Name"}]
   Industry Context: "E-commerce retail"
   ```

2. **Service Configuration**
   ```
   Provider ID: "openai"
   Model ID: "gpt-4"
   Temperature: 0.3
   Max Tokens: 500
   ```

3. **Expected Output**
   ```json
   {
     "results": [
       {
         "sourceHeader": "Customer Name",
         "matchedTargetHeader": "Client Name",
         "confidencePercentage": 95.0,
         "reasoning": "Direct semantic match with existing mapping",
         "usedExistingMapping": true
       },
       {
         "sourceHeader": "Product ID",
         "matchedTargetHeader": "Item ID",
         "confidencePercentage": 88.0,
         "reasoning": "Product and Item are synonymous in retail context",
         "usedExistingMapping": false
       },
       {
         "sourceHeader": "Order Date",
         "matchedTargetHeader": "Purchase Date",
         "confidencePercentage": 92.0,
         "reasoning": "Order and Purchase are equivalent in e-commerce",
         "usedExistingMapping": false
       }
     ]
   }
   ```

### Advanced Configuration

#### Custom Prompt Engineering

The plugin uses intelligent prompt engineering, but you can customize by:

1. **Modifying Industry Context**
   - Provide detailed business domain information
   - Include specific terminology and abbreviations
   - Add regulatory or compliance requirements

2. **Leveraging Existing Mappings**
   - Use historical mapping data for training
   - Include domain-specific examples
   - Provide feedback on previous matches

#### Performance Optimization

1. **Batch Processing**
   - Process multiple headers in single request
   - Use appropriate batch sizes (10-50 headers per request)

2. **Caching Strategy**
   - Cache frequently used mappings
   - Implement result caching for repeated requests

3. **Model Selection**
   - Use faster models for real-time applications
   - Use more accurate models for critical operations

### Adding New Providers

To add a new AI provider:

1. **Implement AIProvider Interface**:
```java
public class NewAIProvider extends AbstractAIProvider {
    @Override
    protected void validateProviderConfiguration(ProviderConfiguration config) {
        // Validate provider-specific configuration
    }
    
    @Override
    protected String sendAIRequest(String prompt, ModelConfiguration modelConfig) {
        // Implement provider-specific AI request logic
        return response;
    }
}
```

2. **Register in Service Loader**:
```properties
# META-INF/services/com.example.plugins.aiintelligence.provider.AIProvider
com.example.plugins.aiintelligence.provider.impl.NewAIProvider
```

3. **Configure in Appian**:
```java
Provider ID: "new-ai-provider"
Provider Parameter Keys: ["param1", "param2"]
Provider Parameter Values: ["value1", "value2"]
```

## Configuration Parameters

### Common Parameters
- **Provider ID**: Unique identifier for the AI provider
- **Model ID**: Specific AI model to use
- **Temperature**: Controls randomness (0.0 = deterministic, 1.0 = very random)
- **Max Tokens**: Maximum response length
- **Top P**: Nucleus sampling parameter
- **Top K**: Top-k sampling parameter

### Provider-Specific Parameters

#### AWS Bedrock
- `region`: AWS region (e.g., "us-east-1")
- `accessKeyId`: AWS access key (optional, uses IAM roles by default)
- `secretAccessKey`: AWS secret key (optional, uses IAM roles by default)

#### OpenAI
- `apiKey`: OpenAI API key
- `organization`: OpenAI organization ID (optional)
- **Timeout**: 2-minute read timeout (automatically configured)

## Configuration System

The plugin uses a centralized configuration system to manage all configurable paths and settings:

### Configuration Files

1. **`config.properties`** - Main configuration file containing:
   - AI provider settings
   - File paths for data sources
   - Maven and build configurations
   - Client application settings

2. **`CONFIGURATION.md`** - Detailed configuration documentation

### Key Configuration Features

- **Environment Variables**: Support for environment-based configuration
- **Centralized Management**: Single source of truth for all settings
- **Cross-Platform**: Works on Windows, Linux, and macOS
- **Flexible Paths**: Relative and absolute path support
- **Build Integration**: Maven properties integration

### Configuration Priority

1. Command-line arguments (highest priority)
2. Environment variables
3. `config.properties` file
4. Default values (lowest priority)

## Development and Testing

### Local Development Environment

The plugin includes a complete development environment:

1. **Test Client**: Standalone Java client for testing plugin functionality
2. **Sample Data**: CSV files for testing column matching scenarios
3. **Build Scripts**: Automated build and test scripts for Windows
4. **Dependencies**: All required JAR files and Maven configuration

### Running Tests

1. **Build the Plugin and Client**:
   ```bash
   .\build-plugin-and-client.bat
   ```

2. **Test with Sample Data**:
   ```bash
   .\test-client.bat
   ```

3. **Sample CSV Files**:
   - `sibs.csv`: Standard SIBS headers
   - `VS - OCC Extract MAS -NEW.csv`: Buy sheet headers
   - `vsi-existing-mappings.csv`: Existing column mappings

### Testing Results

**✅ Current Test Status:**
- **OpenAI Integration**: Fully functional with 2-minute timeout
- **Column Matching**: Successfully processes CSV headers
- **Error Handling**: Gracefully handles truncated responses
- **Performance**: Optimized for production workloads

## Troubleshooting

### Common Issues

#### 1. Plugin Not Found
- **Symptom**: "AI Intelligence Service" not available in palette
- **Solution**: Verify plugin is uploaded and active in Admin Console

#### 2. Authentication Errors
- **AWS Bedrock**: Check IAM permissions and credentials
- **OpenAI**: Verify API key and account status

#### 3. Model Not Available
- **Symptom**: "Model not found" error
- **Solution**: Check model ID spelling and provider availability

#### 4. Timeout Issues (RESOLVED ✅)
- **Previous Symptom**: Request times out during column matching
- **Solution**: Plugin now includes 2-minute timeout for OpenAI operations
- **Status**: Fully resolved in v1.0.0

#### 5. JSON Parsing Errors (RESOLVED ✅)
- **Previous Symptom**: "Failed to parse AI response" errors
- **Solution**: Robust JSON parsing with fallback mechanisms
- **Status**: Fully resolved in v1.0.0

### Debug Mode

Enable debug logging in Appian:
1. **Admin Console** → **System Properties**
2. Add: `ai.intelligence.debug=true`
3. Check Appian logs for detailed information

### Configuration Issues

1. **File Not Found Errors**
   - Check `config.properties` for correct file paths
   - Verify files exist in the specified locations
   - Use absolute paths if relative paths cause issues

2. **Build Failures**
   - Ensure `config.properties` contains valid Maven paths
   - Check that all required JAR files are present in `lib/` directory
   - Verify Maven installation and configuration

### Error Codes

- `AI_PROVIDER_NOT_FOUND`: Provider ID not recognized
- `MODEL_NOT_SUPPORTED`: Model not available for provider
- `AUTHENTICATION_FAILED`: Invalid credentials
- `RATE_LIMIT_EXCEEDED`: API quota exceeded
- `INVALID_INPUT`: Malformed input data
- `JSON_PARSING_ERROR`: Response parsing failed (now handled gracefully)

## Error Handling

The plugin provides comprehensive error handling:

- **AIProviderException**: Custom exceptions with provider context
- **Validation**: Input validation at multiple levels
- **Logging**: Detailed logging for debugging and monitoring
- **Graceful Degradation**: Clear error messages and fallback behavior
- **Resilient Parsing**: Handles malformed or truncated responses
- **Timeout Management**: Configurable timeouts for different operations

## Performance Considerations

- **Provider Caching**: Providers are cached for reuse
- **Connection Pooling**: Efficient connection management
- **Async Support**: Ready for future async implementation
- **Resource Cleanup**: Proper cleanup of external connections
- **Optimized Timeouts**: 2-minute timeout for complex AI operations
- **Response Parsing**: Efficient JSON parsing with fallback mechanisms

## Security Considerations

### Credential Management

1. **Never hardcode credentials** in process models
2. **Use Appian System Properties** for sensitive data
3. **Rotate API keys** regularly
4. **Implement least privilege** access

### Data Privacy

1. **Review AI provider privacy policies**
2. **Ensure compliance** with data regulations
3. **Monitor data usage** and access logs
4. **Implement data retention** policies

### Network Security

1. **Use HTTPS** for all API communications
2. **Implement network segmentation** if required
3. **Monitor API access** and usage patterns
4. **Set up alerts** for unusual activity

## Security

- **Credential Management**: Secure handling of API keys and credentials
- **Input Validation**: Comprehensive input sanitization
- **Error Information**: No sensitive data in error messages
- **IAM Integration**: Support for AWS IAM roles and policies

## Monitoring and Maintenance

### Performance Monitoring

1. **Response Time Tracking**
   - Monitor average response times
   - Set up alerts for performance degradation
   - Track success/failure rates

2. **Usage Analytics**
   - Monitor API usage patterns
   - Track cost implications
   - Identify optimization opportunities

### Regular Maintenance

1. **Update Dependencies**
   - Keep AI provider SDKs updated
   - Monitor for security patches
   - Test compatibility with new versions

2. **Review and Optimize**
   - Analyze usage patterns
   - Optimize prompt engineering
   - Update industry context data

## Best Practices

### 1. Start Simple
- Begin with basic configurations
- Test with small datasets
- Gradually increase complexity

### 2. Use Appropriate Models
- Match model capabilities to use case
- Consider cost vs. performance trade-offs
- Test multiple models for best results

### 3. Provide Rich Context
- Include industry-specific terminology
- Add business rules and constraints
- Use existing mappings for training

### 4. Implement Error Handling
- Graceful degradation on failures
- Retry logic for transient errors
- User-friendly error messages

### 5. Monitor and Iterate
- Track success rates
- Gather user feedback
- Continuously improve configurations

### 6. Leverage Recent Improvements
- Use the optimized timeout settings
- Take advantage of robust JSON parsing
- Test with the provided sample data

### 7. Configuration Management
- Use `config.properties` for centralized configuration
- Leverage environment variables for environment-specific settings
- Keep configuration files in version control (excluding sensitive data)
- Document any custom configuration changes

## Testing

The architecture supports comprehensive testing:

- **Unit Testing**: Individual components can be tested in isolation
- **Mock Providers**: Easy to create mock providers for testing
- **Integration Testing**: Test with real AI providers
- **Performance Testing**: Benchmark different providers and models
- **Error Simulation**: Test error handling and recovery mechanisms
- **End-to-End Testing**: Full workflow testing with real data

## Current Status and Roadmap

### ✅ **v1.0.0 - Production Ready**
- **OpenAI Integration**: Fully functional with timeout optimization
- **AWS Bedrock Integration**: Ready for production use
- **Robust Error Handling**: Graceful handling of network and parsing issues
- **Comprehensive Testing**: Validated with real-world scenarios
- **Development Tools**: Complete testing environment included
- **Configuration System**: Centralized configuration management with environment variable support

### 🚀 **Future Enhancements**
1. **Appian LL Integration**: Native Appian LL provider
2. **Async Processing**: Non-blocking AI requests
3. **Batch Processing**: Process multiple requests efficiently
4. **Provider Metrics**: Performance and usage analytics
5. **Model Fine-tuning**: Support for custom model training
6. **Multi-Provider Fallback**: Automatic failover between providers
7. **Enhanced Monitoring**: Real-time performance dashboards
8. **Advanced Caching**: Intelligent result caching strategies

## Migration from AWS Bedrock Plugin

To migrate from the old AWS Bedrock plugin:

1. **Update Dependencies**: Use the new plugin JAR
2. **Update Service Calls**: Change from `AWSBedrockService` to `AIIntelligenceService`
3. **Update Parameters**: Use the new parameter structure
4. **Test Configuration**: Verify provider and model settings

### Parameter Mapping

| Old Parameter | New Parameter |
|---------------|---------------|
| `modelId` | `modelId` |
| `temperature` | `temperature` |
| `maxTokens` | `maxTokens` |
| `region` | Provider Parameter: `region` |
| `accessKeyId` | Provider Parameter: `accessKeyId` |
| `secretAccessKey` | Provider Parameter: `secretAccessKey` |
| `standardHeaders` | `targetHeaders` |
| `buySheetHeaders` | `sourceHeaders` |
| `existingMappings` | `existingMappings` (updated structure) |

## Support and Resources

### Documentation
- [Appian Plugin Development Guide](https://docs.appian.com/suite/help/23.1/Appian_Plugin_Development_Guide.html)
- [AI Provider Documentation](https://docs.aws.amazon.com/bedrock/ for AWS, https://platform.openai.com/docs for OpenAI)

### Community
- [Appian Community](https://community.appian.com/)
- [GitHub Issues](https://github.com/your-repo/issues)

### Getting Help
1. Check the troubleshooting section above
2. Review Appian logs for error details
3. Verify configuration parameters
4. Test with minimal configuration
5. Contact support with detailed error information

## Support

For questions and support:
- **Documentation**: This README and inline code comments
- **Issues**: Create GitHub issues for bugs and feature requests
- **Contributions**: Pull requests welcome for improvements and new providers

## License

Apache 2.0 License - see LICENSE file for details.
