@echo off
echo ===============================================
echo Building AI Intelligence Plugin & Client
echo ===============================================
echo.

echo 🔧 Step 1: Building AI Intelligence Plugin (with timeout fix)...
echo.

REM Load configuration
for /f "tokens=1,2 delims==" %%a in (config.properties) do (
    if "%%a"=="maven.home" set MAVEN_CMD=%%b
)

REM Use configured Maven path or default
if "%MAVEN_CMD%"=="" (
    set MAVEN_CMD=C:\Users\Shan\Documents\MAS\maven-mvnd-1.0.2-windows-amd64\mvn\bin\mvn.cmd
    echo Warning: maven.home not found in config.properties, using default path
)

echo Using Maven: %MAVEN_CMD%
"%MAVEN_CMD%" -f "pom.xml" clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Plugin build failed!
    pause
    exit /b 1
)
echo ✅ Plugin built successfully: target\ai-intelligence-plugin-1.0.0.jar
echo.

echo 🔧 Step 2: Building Client...
echo.
"%MAVEN_CMD%" -f "client\pom.xml" clean compile
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Client build failed!
    pause
    exit /b 1
)
echo ✅ Client compiled successfully
echo.

echo 🔧 Step 3: Downloading Dependencies...
echo.
"%MAVEN_CMD%" -f "client\pom.xml" dependency:copy-dependencies
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Dependency download failed!
    pause
    exit /b 1
)
echo ✅ Dependencies downloaded successfully
echo.

echo ===============================================
echo 🎉 Build Complete! Ready for testing.
echo ===============================================
echo.
echo Next steps:
echo 1. Run test-client.bat to test the AI Intelligence Plugin
echo 2. The timeout issue should now be resolved
echo 3. Column matching should work with OpenAI
echo.
pause
