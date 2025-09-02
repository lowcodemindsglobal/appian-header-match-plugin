# AI Intelligence Plugin - Architecture Diagrams

This document provides visual representations of the AI Intelligence Plugin's architecture and process flow using Mermaid diagrams.

## High-Level Architecture

The AI Intelligence Plugin follows a modular, extensible architecture with clear separation of concerns and support for multiple AI providers.

```mermaid
graph TB
    %% Appian Integration Layer
    subgraph "Appian Integration Layer"
        AIService[AIIntelligenceService<br/>Smart Service]
    end
    
    %% Core Business Logic
    subgraph "Core Business Logic"
        Domain[Domain Models<br/>ColumnMapping, ColumnMatchingResult]
        Config[Configuration Management<br/>ProviderConfiguration, ModelConfiguration]
    end
    
    %% Provider Abstraction Layer
    subgraph "Provider Abstraction Layer"
        Factory[AIProviderFactory<br/>Factory Pattern]
        Abstract[AbstractAIProvider<br/>Template Method Pattern]
        Interface[AIProvider Interface<br/>Strategy Pattern]
    end
    
    %% Provider Implementations
    subgraph "Provider Implementations"
        OpenAI[OpenAI Provider<br/>GPT Models]
        Bedrock[AWS Bedrock Provider<br/>Claude Models]
        Future[Future Providers<br/>Appian LL, etc.]
    end
    
    %% External Services
    subgraph "External AI Services"
        OpenAI_API[OpenAI API]
        Bedrock_API[AWS Bedrock API]
    end
    
    %% Configuration & Utilities
    subgraph "Configuration & Utilities"
        ConfigLoader[ConfigurationLoader]
        Props[config.properties]
        EnvVars[Environment Variables]
    end
    
    %% Connections
    AIService --> Domain
    AIService --> Factory
    Factory --> Interface
    Interface --> Abstract
    Abstract --> OpenAI
    Abstract --> Bedrock
    Abstract --> Future
    
    OpenAI --> OpenAI_API
    Bedrock --> Bedrock_API
    
    Factory --> Config
    Config --> ConfigLoader
    ConfigLoader --> Props
    ConfigLoader --> EnvVars
    
    %% Styling
    classDef appianLayer fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef coreLayer fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef providerLayer fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef implLayer fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef externalLayer fill:#ffebee,stroke:#b71c1c,stroke-width:2px
    classDef configLayer fill:#f1f8e9,stroke:#33691e,stroke-width:2px
    
    class AIService appianLayer
    class Domain,Config coreLayer
    class Factory,Abstract,Interface providerLayer
    class OpenAI,Bedrock,Future implLayer
    class OpenAI_API,Bedrock_API externalLayer
    class ConfigLoader,Props,EnvVars configLayer
```

## Process Flow

The column matching process follows a systematic workflow that handles input validation, AI provider selection, intelligent matching, and result processing.

```mermaid
flowchart TD
    %% Start
    Start([Start AI Intelligence Service]) --> Input[Receive Input Parameters<br/>sourceHeaders, targetHeaders,<br/>existingMappings, industryContext]
    
    %% Input Validation
    Input --> Validate{Validate Inputs}
    Validate -->|Invalid| Error1[Set Error Message<br/>Return Failure]
    Validate -->|Valid| Config[Create Provider & Model<br/>Configuration]
    
    %% Provider Setup
    Config --> Factory[AIProviderFactory<br/>Create Provider Instance]
    Factory --> ProviderReady{Provider Ready?}
    ProviderReady -->|No| Error2[Provider Initialization Error]
    ProviderReady -->|Yes| Parse[Parse Existing Mappings<br/>from JSON]
    
    %% Column Processing
    Parse --> Filter[Filter Unmapped Headers<br/>Exclude Already Mapped]
    Filter --> Empty{Unmapped Headers?}
    Empty -->|No| Existing[Return Existing Mappings<br/>as Results]
    Empty -->|Yes| Process[Process Each Unmapped Header]
    
    %% AI Processing Loop
    Process --> BuildPrompt[Build AI Prompt<br/>for Single Column]
    BuildPrompt --> SendRequest[Send Request to<br/>AI Provider]
    SendRequest --> ParseResponse[Parse AI Response<br/>Handle Truncation]
    ParseResponse --> ValidResult{Valid Result?}
    ValidResult -->|No| Default[Create Default Result<br/>with Low Confidence]
    ValidResult -->|Yes| AddResult[Add to Results List]
    Default --> NextHeader{More Headers?}
    AddResult --> NextHeader
    NextHeader -->|Yes| Process
    NextHeader -->|No| Combine[Combine All Results<br/>Existing + New]
    
    %% Result Processing
    Combine --> Calculate[Calculate Statistics<br/>matchedHeadersCount,<br/>averageConfidence, etc.]
    Calculate --> Success[Set Success Output<br/>Return Results]
    
    %% Error Handling
    Error1 --> End([End])
    Error2 --> End
    Existing --> End
    Success --> End
    
    %% Styling
    classDef startEnd fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    classDef process fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef decision fill:#fff3e0,stroke:#ef6c00,stroke-width:2px
    classDef error fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef success fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    
    class Start,End startEnd
    class Input,Config,Factory,Parse,Filter,Process,BuildPrompt,SendRequest,ParseResponse,AddResult,Combine,Calculate process
    class Validate,ProviderReady,Empty,ValidResult,NextHeader decision
    class Error1,Error2,Default error
    class Existing,Success success
```

## Key Design Patterns

The architecture implements several software engineering design patterns:

1. **Strategy Pattern**: `AIProvider` interface allows switching between different AI services
2. **Factory Pattern**: `AIProviderFactory` creates provider instances dynamically
3. **Template Method Pattern**: `AbstractAIProvider` defines common workflow structure
4. **Service Locator Pattern**: Uses `ServiceLoader` for automatic provider discovery

## Configuration Management

The plugin supports flexible configuration through:
- `config.properties` file
- Environment variables
- System properties
- Provider-specific parameters

## Extensibility

New AI providers can be easily added by:
1. Implementing the `AIProvider` interface
2. Extending `AbstractAIProvider` for common functionality
3. Adding provider configuration to `config.properties`
4. Registering the provider in `META-INF/services`

The plugin is designed to be production-ready with robust error handling, timeout management, and graceful degradation for various failure scenarios.
