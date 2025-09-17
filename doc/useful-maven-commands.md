# Useful Maven & JAR Analysis Commands

This document lists commonly used Maven and JAR inspection commands for
maintaining and validating the **AI Intelligence Plugin** before
submission to Appian.

------------------------------------------------------------------------

## 🔍 Inspecting JAR Contents

``` bash
"C:\Program Files\Java\jdk-17\bin\jar.exe" tf target\ai-intelligence-plugin-1.0.0.jar | findstr /I "log4j wiremock jmh jol reactive-streams-tck commons-lang-2 classgraph junit-4 jetty netty"
```

**Purpose**:\
Checks if any forbidden or unwanted dependencies (e.g., `log4j`,
`wiremock`, `jmh`, `junit`, `netty`, etc.) are included in the packaged
JAR.

------------------------------------------------------------------------

## 🛡️ Run OWASP Dependency Check

``` bash
mvn org.owasp:dependency-check-maven:check -DskipTests
```

**Purpose**:\
Scans project dependencies for known vulnerabilities using the OWASP
dependency check plugin.

------------------------------------------------------------------------

## ✅ Security Scan Profile (Appian Submission Readiness)

``` bash
mvn -Psecurity-scan verify -DskipTests
```

**Purpose**:\
Runs the **security-scan** Maven profile, generating SBOM (CycloneDX),
licenses, and dependency checks.

------------------------------------------------------------------------

## 📦 Generate SBOM (CycloneDX)

``` bash
mvn org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom -DskipTests
```

**Purpose**:\
Generates **Software Bill of Materials (SBOM)** in JSON and XML formats
for dependency transparency.

Outputs: - `target/bom.xml` - `target/bom.json`

------------------------------------------------------------------------

## 🏗️ Build Plugin JAR

``` bash
mvn clean package
```

**Purpose**:\
Cleans and builds the plugin, producing: -
`target/ai-intelligence-plugin-1.0.0.jar` (regular JAR) -
`target/ai-intelligence-plugin-1.0.0-shaded.jar` (shaded JAR for Appian
upload)

------------------------------------------------------------------------

## ℹ️ Notes

-   **Always upload the `shaded.jar`**
    (`ai-intelligence-plugin-1.0.0-shaded.jar`) to the Appian
    AppMarket.\
-   Use `dependency:tree` when debugging dependency conflicts:

``` bash
mvn dependency:tree -Dverbose -Dscope=runtime
```

------------------------------------------------------------------------
