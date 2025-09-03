@echo off
echo ===============================================
echo AI Intelligence Plugin - Test Client
echo ===============================================
echo.
echo Testing with OpenAI API...
echo Setting up OpenAI credentials...
echo.

REM Load OpenAI API key from config.properties
for /f "tokens=1,2 delims==" %%a in (config.properties) do (
    if "%%a"=="openai.api.key" set OPENAI_API_KEY=%%b
)

REM Check if API key is configured
if "%OPENAI_API_KEY%"=="" (
    echo ⚠️  OpenAI API key not found in config.properties
    echo Checking for environment variable OPENAI_API_KEY...
    
    REM Check if environment variable is set
    if "%OPENAI_API_KEY%"=="" (
        echo ❌ OpenAI API key not configured
        echo Please either:
        echo 1. Add your OpenAI API key to config.properties under 'openai.api.key='
        echo 2. Set the OPENAI_API_KEY environment variable
        echo.
        pause
        exit /b 1
    ) else (
        echo ✅ OpenAI API key loaded from environment variable
    )
) else (
    echo ✅ OpenAI API key loaded from config.properties
)

echo ✅ OpenAI API key loaded from configuration
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
echo Running AIIntelligenceClient...
echo Using OpenAI GPT-4 model
echo.

REM Use proper Windows path separators and quote the filename with spaces
java -cp "target/classes;target/dependency/*" com.example.client.AIIntelligenceClient --standard-headers-file "%STANDARD_HEADERS%" --buy-sheet-headers-file "%BUY_SHEET_HEADERS%" --existing-mappings-file "%EXISTING_MAPPINGS%"

REM Check if the command was successful
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Test completed successfully!
) else (
    echo.
    echo ❌ Test failed with error code: %ERRORLEVEL%
)

REM Return to parent directory
cd ..

echo.
pause
