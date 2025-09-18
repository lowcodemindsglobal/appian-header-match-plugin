## AI Intelligence Plugin — Root Cause Analysis and Fix Plan

Generated Data - 2025/09/18

### Context
You reported review feedback/issues when using or submitting the plugin. Below is a targeted analysis of likely root causes in this codebase and a concrete plan to address them. No code has been changed yet.

Reviewer feedback (verbatim, summarized):
- Plugin appears to be missing source code (.java files) in the submitted artifact — required for review.
- High volume of CVEs flagged; recommendation is to not include unpackaged dependency classes in the main JAR. Instead package dependent JARs under `META-INF/lib/`.
- Provided a Gradle example for placing dependencies into `META-INF/lib`.

Environment confirmed:
- Java 17; Appian 23.3 (latest) — aligned with `appian-plugin.xml` and `pom.xml`.
- Uploaded artifact: shaded JAR.
- Target model: AWS Bedrock Llama Scout 4 only (for now).
- Multi-region/credentials support needed in the environment.

### High-Probability Root Causes

1) Packaging approach not compliant with review expectations (sources + dependencies layout)
- Description: Review requires source code availability and recommends not embedding unpackaged dependency classes in the main JAR. Instead, include dependent JARs under `META-INF/lib/` and ensure sources are accessible.
- Evidence: Reviewer screenshot; project currently builds an uber-jar via `maven-shade-plugin`.
- Impact: Review rejection due to missing `.java` sources in upload and uber-jar style packaging.

2) AWS Bedrock invocation missing content type/accept headers
- Description: `AWSBedrockProvider` builds `InvokeModelRequest` without setting `contentType` and `accept`. Many Bedrock models require `contentType: application/json` and `accept: application/json`.
- Evidence: `InvokeModelRequest.builder().modelId(...).body(...).build()` with no headers.
- Impact: Bedrock returns validation/serialization errors (e.g., “Content-Type is missing or invalid”).

3) Cached provider uses stale configuration across executions
- Description: `AIProviderFactory` caches provider instances globally and returns the cached one when `isReady()` is true. The cached provider holds the first configuration provided and is not revalidated or reconfigured when subsequent calls specify new creds/region/model.
- Evidence: `providerCache` + `isReady()` short-circuit; `validateConfiguration` sets fields on the instance; no re-validate path for different configs.
- Impact: Incorrect region/credentials/model used in multi-execution or multi-environment scenarios.

4) SLF4J API major-version mismatch risk
- Description: Project compiles against `slf4j-api:2.0.16` with scope `provided`. Appian commonly ships with 1.7.x API. Compiling against 2.x but running against 1.7.x can cause runtime incompatibility.
- Evidence: `pom.xml` `<slf4j.version>2.0.16</slf4j.version>` with `<scope>provided</scope>`.
- Impact: Potential `NoSuchMethodError`/linkage errors in Appian runtime.

5) Excessive info-level logging and console prints (AppMarket standards)
- Description: Numerous `logger.info(...)` statements with detailed internal state, plus `System.err.println` in `ConfigurationLoader`. AppMarket guidelines discourage noisy logs and direct stdout/stderr.
- Evidence: High-volume info logs in `AIIntelligenceService`, `AbstractAIProvider`, `AWSBedrockProvider`; `System.err.println` in `ConfigurationLoader`.
- Impact: Review rejection for logging verbosity or direct console usage; noisy production logs.

6) Misformatted SLF4J message with printf-style placeholders
- Description: One log line uses `"{:.2f}%"`-style placeholders instead of `{}`.
- Evidence: `AIIntelligenceService.processResults(...)` final info log uses `{:.2f}%`.
- Impact: Log not formatted; confusing output; minor quality issue flagged in review.

7) Documentation mismatch and packaging quirks
- Description: README previously directed uploading the non-shaded JAR; actual upload is shaded. Non-standard resource copying may duplicate resources; `META-INF/services` for SmartService is likely unnecessary (Appian uses `appian-plugin.xml`).
- Evidence: `maven-resources-plugin` copies entire `src/` (excluding `.java`) plus Maven default includes `src/main/resources/`. SmartService is declared in both `appian-plugin.xml` and `META-INF/services`.
- Impact: Confusion during deployment; potential duplication (usually harmless); reviewer questions.

8) Configuration source of truth ambiguity
- Description: `AWSBedrockProvider` can load creds from `config.properties`, env vars, or Appian parameters. Appian best practice is to use Admin Console properties/secure credentials. Loading from arbitrary files may be flagged.
- Evidence: `loadCredentialsFromConfig()` searches several paths and logs guidance to place `config.properties` on server.
- Impact: Review concerns; increased operational complexity/security questions.

9) Minor hygiene items
- Unused import(s) (e.g., `TypeReference` in `AIIntelligenceService`).
- `ConfigurationLoader` is present but not actually used by core flow (duplication/redundancy).

### Additional Findings from Re-Verification

10) Potential secret leakage via parameter logging
- Description: `AIIntelligenceService.createProviderConfiguration` logs entire `providerParameterValues` array at info level using `Arrays.toString(...)` before masking per-key values in the loop, potentially exposing secrets.
- Evidence: Info logs at the start of `createProviderConfiguration` print raw arrays.
- Impact: Security and AppMarket review concerns.

11) `rawResponse` output is never populated
- Description: The service exposes `rawResponse` as an output with labels in `aiintelligence.properties` but no code path sets it.
- Impact: Confusing/empty output parameter; review nit.

12) Bedrock model validation too rigid and contains account-specific ARN
- Description: `AWSBedrockProvider.getSupportedModels()` hardcodes one specific application inference profile ARN and a model id `meta.llama4-scout-17b-instruct-v1:0`. Validation rejects any model not in this list.
- Impact: Legitimate models/ARNs may be blocked; submission may be questioned due to account-specific ARN.

13) Appian/Java runtime compatibility risk
- Description: Using Java 17 is fine for Appian 23.3 (as confirmed), but this remains a risk for earlier Appian versions; keep documented.
- Impact: None for your stated environment; retain note for portability.

### Fix Plan (Phase 1: Address Appian Review Feedback Only)

1) Packaging and sources compliance
- Provide sources to reviewers: produce a `-sources.jar` (or include repo link, per guidelines).
- For submission build, do NOT produce an uber-jar. Package dependent JARs under `META-INF/lib/`.
- Maven approach (no code changes; build-only):
  - Add a profile `appmarket` that:
    - Uses `maven-dependency-plugin` to copy runtime deps to a staging dir and configures `maven-jar-plugin` to place them under `META-INF/lib/` in the plugin JAR.
    - Uses `maven-source-plugin` to attach `-sources.jar`.
  - Keep current shaded build in a separate profile (e.g., `shaded`) for internal use only.
- Update README/submission notes to explain which artifact to upload for Appian review.

2) Bedrock request headers
- In `AWSBedrockProvider.sendAIRequest`, set headers when building `InvokeModelRequest`:
  - `contentType("application/json")`
  - `accept("application/json")`
- Verify request body schema per model family (Anthropic, Llama, Titan) against current AWS docs.

3) Provider configuration lifecycle
- Reconfigure on each execution: do not return a cached instance unless its configuration matches the new request. Options:
  - Remove caching altogether (simplest, safest for Appian execution model), or
  - Include configuration hash in the cache key, or
  - Always call `validateConfiguration(configuration)` on the instance prior to use and allow the provider to update its internal state safely.

4) SLF4J compatibility (Appian 23.3 → 25.2)
- Verify SLF4J API version bundled with Appian 25.2. If Appian ships SLF4J 1.7.x, set compile-time `slf4j-api` to 1.7.x with scope `provided`. If 2.x is present, current config is fine. This avoids linkage errors noted by reviewers.

5) Logging standards
- Downgrade routine logs from `info` to `debug` (retain `info` for start/finish and key outcomes; `warn`/`error` for real issues).
- Remove direct `System.out`/`System.err` prints; use logger consistently.
- Scrub logs to ensure no secrets or sensitive configuration values are logged.

6) Correct SLF4J placeholders
- Replace printf-style placeholders with `{}` placeholders and format values explicitly if needed (e.g., `String.format`) before logging.

7) Documentation and packaging
- Fix README deployment section to point to shaded JAR and list disallowed dependencies already excluded by shade configuration.
- Consider removing `META-INF/services/com.appiancorp.suiteapi.process.framework.SmartService` (keep `appian-plugin.xml` as the authoritative registration) to avoid confusion.
- Simplify resource copying: prefer standard Maven layout (`src/main/java`, `src/main/resources`) and remove the broad copy of entire `src/` if feasible in a follow-up.

8) Configuration source of truth
- Prefer Appian Admin Console properties/secure credential stores for AWS config. Keep environment/credentials chain as fallback but de-emphasize `config.properties` file on server.
- Update README and inline messages accordingly.

9) Hygiene
- Remove unused imports and dead code; ensure linter passes.

10) Prevent secret leakage
- Remove or downgrade the broad array logging of `providerParameterValues`; only log safe keys/values and mask secrets.

11) Populate or remove `rawResponse`
- If needed by users, set `rawResponse` to the provider raw response (or a sanitized version). Otherwise, remove the output to avoid confusion.

12) Model validation flexibility
- Replace the hardcoded model list with either:
  - A documented, maintainable allowlist per provider updated from configuration, or
  - A looser validation that accepts Bedrock model IDs and ARNs matching known patterns, with runtime errors handled gracefully.
 - Since you only target Bedrock Llama Scout 4 right now, restrict to that model plus pattern-based acceptance for its ARN variants. Remove account-specific ARNs from the code and move to configuration.

13) Appian/Java version alignment
- Confirmed OK (Java 17 + Appian 23.3). Keep a note in README that Java 17 is required.

### Validation Plan
- Local: `mvn clean package` ensures shaded JAR builds successfully and dependency checks pass.
- Static checks: run enforcer and OWASP Dependency-Check already present; confirm no high CVSS issues. For CVEs mentioned (commons-io, commons-lang3, jetty, guava, wiremock), ensure they are not in the final plugin classpath. If transitively present (e.g., via AWS SDK), update versions or exclude offending modules.
- Runtime (Appian): deploy shaded JAR; execute the Smart Service against Bedrock test model with known-good config; verify successful invocation and correct JSON parsing.
- Logs: confirm reduced verbosity and correct formatting; verify no secrets printed.

Note on CVEs and packaging strategy (Phase 1 scope):
- The reviewer’s CVEs (commons-io, commons-lang3, jetty, guava, wiremock) likely came from scanning the uber-jar and/or older transitive deps.
- Phase 1 actions:
  - Stop using the uber-jar for submission; place runtime deps under `META-INF/lib/` to preserve artifact boundaries and versions.
  - Use `versions-maven-plugin` to bump transitive offenders where feasible and/or add targeted `<exclusions>` if not needed at runtime.
  - Re-run OWASP Dependency-Check to confirm CVSS < 7 for submission.
  - Ensure disallowed libs (e.g., jetty, wiremock) are not bundled.

### Appian 25.2 Impact (Phase 1)
- Java: 17 remains supported; no code changes required.
- SLF4J: verify the API version bundled with 25.2 and align `slf4j-api` dependency scope `provided` to match.
- Packaging: The move to `META-INF/lib/` is compatible across Appian versions; no behavioral impact to plugin logic.

### Phase 1 Implementation Status
- Implemented a new Maven profile `appmarket` that:
  - Copies runtime dependencies into `META-INF/lib/` within the plugin JAR (no uber-jar).
  - Attaches a `-sources.jar` for reviewer access.
- Moved the shade build into a separate default `shaded` profile for internal use only.

Build commands:
- AppMarket submission artifact (dependencies in META-INF/lib + sources):
  - `mvn clean package -P appmarket`
  - Submit BOTH artifacts for review: the main plugin JAR and the generated `-sources.jar`.
- Internal shaded build (unchanged):
  - `mvn clean package -P shaded`

Post-build checks:
- Run: `mvn org.owasp:dependency-check-maven:check -P appmarket`
- Confirm no high (CVSS ≥ 7) vulnerabilities and that disallowed libs are not present.

### Conclusion and Confidence
- High confidence these changes fix the most likely causes of runtime failures and AppMarket review concerns: shaded JAR deployment, Bedrock headers, provider caching behavior, logging placeholder fix, and secret-safe logging.
- Medium confidence areas depend on environment/policies: SLF4J version alignment with your Appian runtime, Java 17 vs 11 compatibility, and how strict model validation should be for your use cases.
- No incompatible code edits have been made yet; the plan is staged to minimize risk and validate after each change.

Given your confirmations (Java 17, moving to Appian 25.2, shaded JAR previously uploaded, Llama Scout 4 only), confidence is high that Phase 1 (packaging + dependency hygiene + sources) resolves the reviewer’s concerns without logic changes. Logic/formatting items are deferred to Phase 2.

### Review Submission Checklist (Phase 1)
- Built with `-P appmarket` (no uber-jar). 
- Main JAR contains `META-INF/lib/` with dependencies; no disallowed libs present.
- Submitted `ai-intelligence-plugin-<version>.jar` and `ai-intelligence-plugin-<version>-sources.jar`.
- Java set to 17; Appian version documented as 25.2.
- OWASP Dependency-Check run with no CVSS ≥ 7 findings.
- README notes how to build submission vs shaded internal build.

### Open Questions (please confirm)
- What was the exact reviewer/feedback message? We can tailor the fix and the README changes precisely.
- Which Appian version/environment are you targeting (for SLF4J alignment)?
- Do you need to support multiple AWS regions/credentials in the same Appian environment concurrently? That will guide the provider caching strategy.
- Which JAR did you upload to Appian (`-shaded` vs non-shaded)? Any ClassNotFound or class version errors seen?
- What exact Bedrock model IDs/ARNs are you using? Do you rely on application inference profiles?
 - For packaging compliance: would you prefer we add a new Maven profile (e.g., `appmarket`) that builds a non-uber JAR with deps in `META-INF/lib/` and produces a `-sources.jar` for review, while keeping the current shaded build for internal use?

### Next Steps (upon your approval)
- Apply the changes above in small, reviewable edits, starting with README and Bedrock request headers, then provider caching and logging/SLF4J adjustments.


