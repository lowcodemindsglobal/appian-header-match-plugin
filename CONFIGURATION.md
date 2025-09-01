# Configuration Guide

This document explains how to configure the AI Intelligence Plugin using the new configuration system that replaces hardcoded paths with environment variables.

## Overview

The plugin now uses a `config.properties` file to store all configuration values, making it easy to:
- Change paths without modifying source code
- Deploy to different environments
- Share configuration templates
- Avoid committing sensitive information to version control

## Configuration File

The main configuration file is `config.properties` in the root directory. This file contains all the configurable paths and settings.

### File Structure

```properties
# Maven Configuration
maven.home=C:\\Users\\Shan\\Documents\\MAS\\maven-mvnd-1.0.2-windows-amd64\\mvn\\bin\\mvn.cmd

# Appian SDK Configuration
appian.sdk.path=lib/appian-plug-in-sdk.jar

# Data File Paths
standard.headers.file=data/input/sibs.csv
buy.sheet.headers.file=data/input/VS - OCC Extract MAS -NEW.csv
existing.mappings.file=data/input/vsi-existing-mappings.csv
vsi.mapping.file=data/input/VSI mapping.csv

# Client Configuration
client.config.file=src/main/resources/client-config.properties
output.directory=client-output/responses

# Build Output Paths
client.target.dir=client/target
plugin.target.dir=target

# API Keys (set these in your environment)
openai.api.key=your_openai_api_key_here
aws.access.key.id=your_aws_access_key_here
aws.secret.access.key=your_aws_secret_key_here
aws.default.region=us-west-2

# AI Model Configuration
default.model.id=anthropic.claude-3-sonnet-20240229-v1:0
default.temperature=0.7
default.max.tokens=1000
```

## Environment Variables

The system also supports environment variables for sensitive information. Set these in your system environment:

### Required Environment Variables

- `OPENAI_API_KEY`: Your OpenAI API key
- `AWS_ACCESS_KEY_ID`: Your AWS access key
- `AWS_SECRET_ACCESS_KEY`: Your AWS secret key
- `AWS_DEFAULT_REGION`: Your AWS region (default: us-west-2)

### Setting Environment Variables

#### Windows (PowerShell)
```powershell
$env:OPENAI_API_KEY="your_api_key_here"
$env:AWS_ACCESS_KEY_ID="your_access_key_here"
$env:AWS_SECRET_ACCESS_KEY="your_secret_key_here"
```

#### Windows (Command Prompt)
```cmd
set OPENAI_API_KEY=your_api_key_here
set AWS_ACCESS_KEY_ID=your_access_key_here
set AWS_SECRET_ACCESS_KEY=your_secret_key_here
```

#### Linux/macOS
```bash
export OPENAI_API_KEY="your_api_key_here"
export AWS_ACCESS_KEY_ID="your_access_key_here"
export AWS_SECRET_ACCESS_KEY="your_secret_key_here"
```

## Configuration Priority

The system loads configuration in the following order (later values override earlier ones):

1. **Main config file** (`config.properties`)
2. **Client config file** (`src/main/resources/client-config.properties`)
3. **Environment variables**
4. **Command line arguments**
5. **Default values**

## Customizing Paths

### Maven Installation
Update the `maven.home` property to point to your Maven installation:
```properties
maven.home=C:\\path\\to\\your\\maven\\bin\\mvn.cmd
```

### Data Files
Update the data file paths to match your file structure:
```properties
standard.headers.file=path/to/your/standard-headers.csv
buy.sheet.headers.file=path/to/your/buy-sheet-headers.csv
existing.mappings.file=path/to/your/existing-mappings.csv
```

### Output Directory
Change where responses are saved:
```properties
output.directory=your/custom/output/path
```

## Building and Testing

### Building the Plugin
```bash
# The build script now uses the configured Maven path
./build-plugin-and-client.bat
```

### Testing the Client
```bash
# The test script now uses the configured data file paths
./test-client.bat
```

## Troubleshooting

### Configuration Not Loading
- Ensure `config.properties` exists in the root directory
- Check file permissions
- Verify the file format (no extra spaces around `=`)

### Path Not Found
- Use absolute paths if relative paths don't work
- Ensure the configured paths exist
- Check for typos in file names

### Environment Variables Not Working
- Restart your terminal after setting environment variables
- Use `echo $VARIABLE_NAME` (Linux/macOS) or `echo %VARIABLE_NAME%` (Windows) to verify
- Ensure no spaces around the `=` when setting variables

## Security Notes

- **Never commit** `config.properties` with real API keys
- Use environment variables for sensitive information
- Consider using `.gitignore` to exclude configuration files with secrets
- Rotate API keys regularly

## Migration from Hardcoded Paths

If you're upgrading from the previous version:

1. Copy the `config.properties` file to your project root
2. Update the paths to match your environment
3. Set your API keys as environment variables
4. Test the build and test scripts

The system will fall back to default values if configuration files are missing, ensuring backward compatibility.
