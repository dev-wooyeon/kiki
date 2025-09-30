# Repository Guidelines

## Project Structure & Module Organization
- Source: `src/main/kotlin/com/kiki/...` (Spring Boot app entry: `src/main/kotlin/com/kiki/KikiApplication.kt`).
- Resources: `src/main/resources/application.yml` (prod); create `application-local.yml` for local overrides.
- Tests: `src/test/kotlin/...` with `*Test.kt` and `*IntegrationTest.kt`; test resources in `src/test/resources`.
- Build & Ops: `build.gradle.kts`, `gradlew`, `settings.gradle.kts`, `Dockerfile`, `fly.toml`.

## Build, Test, and Development Commands
- `./gradlew build` — Compile and run tests; artifacts in `build/libs`.
- `./gradlew test` — Run JUnit 5 tests (MockK, Spring Boot Test, H2).
- `./gradlew bootRun -Dspring.profiles.active=local` — Run locally with the `local` profile (use H2 and dev mail settings in `application-local.yml`).
- `./gradlew clean` — Remove build outputs.

## Coding Style & Naming Conventions
- Language: Kotlin 1.9 on JVM 17; Spring Boot 3.
- Follow Kotlin official style: 4-space indent, meaningful names, 120-col soft limit.
- Packages: lowercase dot-separated (e.g., `com.kiki.service`). Classes: PascalCase; methods/vars: camelCase; constants: UPPER_SNAKE_CASE.
- One primary top-level type per file; keep files under the matching package path.
- No formatter plugin in Gradle; use IDE “Reformat Code” with Kotlin style.

## Testing Guidelines
- Frameworks: JUnit 5, Spring Boot Test, MockK, H2. Enable platform via Gradle (already configured).
- Naming: `SomethingServiceTest.kt` for unit tests; `SomethingIntegrationTest.kt` for Spring context/integration.
- Keep tests deterministic; prefer H2 and `application-test.yml` defaults. Mock external HTTP where feasible.
- Run with `./gradlew test`; add edge cases for scrapers, scheduling, and email formatting.

## Commit & Pull Request Guidelines
- Commits: imperative mood, concise subject (<72 chars), body explains motivation and impact.
- Prefer Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`) for clarity and history scanning.
- PRs: include a clear description, linked issues, test plan (commands run), and API examples (e.g., `curl` for new/changed endpoints). Note any config/env changes.

## Security & Configuration Tips
- Do not commit secrets; use environment variables or Fly.io secrets. Keep `OPENAI_API_KEY`, DB, and mail creds out of VCS.
- For local work, add `src/main/resources/application-local.yml` (not committed) and run with `-Dspring.profiles.active=local`.
- Set `ALLOWED_ORIGINS` appropriately; avoid wildcards in production.
