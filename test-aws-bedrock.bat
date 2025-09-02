@echo off
echo ===============================================
echo AI Intelligence Plugin - AWS Bedrock Test
echo ===============================================
echo.
echo Testing with AWS Bedrock Application Inference Profile...
echo Setting up AWS credentials...
echo.

REM Set environment variables for AWS Bedrock testing
set AI_PROVIDER=aws-bedrock
set AWS_DEFAULT_REGION=us-west-2

REM Load AWS credentials from config.properties
for /f "tokens=1,2 delims==" %%a in (config.properties) do (
    if "%%a"=="aws.access.key.id" set AWS_ACCESS_KEY_ID=%%b
    if "%%a"=="aws.secret.access.key" set AWS_SECRET_ACCESS_KEY=%%b
)

REM Check if AWS credentials are configured
if "%AWS_ACCESS_KEY_ID%"=="" (
    echo ⚠️  AWS Access Key ID not found in config.properties
    echo Checking for environment variable AWS_ACCESS_KEY_ID...
    
    if "%AWS_ACCESS_KEY_ID%"=="" (
        echo ❌ AWS Access Key ID not configured
        echo Please either:
        echo 1. Add your AWS Access Key ID to config.properties under 'aws.access.key.id='
        echo 2. Set the AWS_ACCESS_KEY_ID environment variable
        echo.
        pause
        exit /b 1
    ) else (
        echo ✅ AWS Access Key ID loaded from environment variable
    )
) else (
    echo ✅ AWS Access Key ID loaded from config.properties
)

if "%AWS_SECRET_ACCESS_KEY%"=="" (
    echo ⚠️  AWS Secret Access Key not found in config.properties
    echo Checking for environment variable AWS_SECRET_ACCESS_KEY...
    
    if "%AWS_SECRET_ACCESS_KEY%"=="" (
        echo ❌ AWS Secret Access Key not configured
        echo Please either:
        echo 1. Add your AWS Secret Access Key to config.properties under 'aws.secret.access.key='
        echo 2. Set the AWS_SECRET_ACCESS_KEY environment variable
        echo.
        pause
        exit /b 1
    ) else (
        echo ✅ AWS Secret Access Key loaded from environment variable
    )
) else (
    echo ✅ AWS Secret Access Key loaded from config.properties
)

echo ✅ AWS credentials loaded from configuration
echo.

REM Load configuration
for /f "tokens=1,2 delims==" %%a in (config.properties) do (
    if "%%a"=="client.target.dir" set CLIENT_TARGET=%%b
    if "%%a"=="standard.headers.file" set STANDARD_HEADERS=%%b
    if "%%a"=="buy.sheet.headers.file" set BUY_SHEET_HEADERS=%%b
    if "%%a"=="existing.mappings.file" set EXISTING_MAPPINGS=%%b
)

REM Use configured paths or defaults
if "%CLIENT_TARGET%"=="" set CLIENT_TARGET=client/target
if "%STANDARD_HEADERS%"=="" set STANDARD_HEADERS=data/input/sibs.csv
if "%BUY_SHEET_HEADERS%"=="" set BUY_SHEET_HEADERS=data/input/VS - OCC Extract MAS -NEW.csv
if "%EXISTING_MAPPINGS%"=="" set EXISTING_MAPPINGS=data/input/VSI mapping.csv

REM Check if client has been built
if not exist "%CLIENT_TARGET%\classes\com\example\client\AIIntelligenceClient.class" (
    echo ❌ Client not built yet! Please run build-plugin-and-client.bat first.
    echo.
    pause
    exit /b 1
)

REM Check if dependencies exist
if not exist "%CLIENT_TARGET%\dependency" (
    echo ❌ Dependencies not downloaded! Please run build-plugin-and-client.bat first.
    echo.
    pause
    exit /b 1
)

echo ✅ Client is built and ready for testing
echo.

REM Change to client directory
cd client

REM Run the compiled class with proper classpath
echo Running AIIntelligenceClient with AWS Bedrock...
echo Using Application Inference Profile: Llama 4 Scout 17B
echo ARN: arn:aws:bedrock:us-west-2:211125498297:application-inference-profile/hx2e1juc8tej
echo.

REM Use proper Windows path separators and quote the filename with spaces
java -cp "target/classes;target/dependency/*" com.example.client.AIIntelligenceClient --standard-headers-file "%STANDARD_HEADERS%" --buy-sheet-headers-file "%BUY_SHEET_HEADERS%" --existing-mappings-file "%EXISTING_MAPPINGS%"

REM Check if the command was successful
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ AWS Bedrock test completed successfully!
) else (
    echo.
    echo ❌ AWS Bedrock test failed with error code: %ERRORLEVEL%
)

REM Return to parent directory
cd ..

echo.
pause
