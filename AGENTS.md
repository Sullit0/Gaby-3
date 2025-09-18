# Repository Guidelines

## Project Structure & Module Organization
PsychNotes is a Kotlin Multiplatform project targeting Compose Desktop. Core domain and data live in `shared/`. `shared/src/commonMain/kotlin` contains view models, repositories, and Sqldelight adapters; `shared/src/commonMain/sqldelight` keeps SQL schema, triggers, and migrations. Desktop-specific services reside in `shared/src/desktopMain`. The UI shell is under `desktopApp/src/jvmMain/kotlin` with `Main.kt` launching the Compose window. Build logic sits in each module’s `build.gradle.kts`, while the root script centralizes plugin versions and repository configuration.

## Build, Test, and Development Commands
Ensure JDK 17 is active (``export JAVA_HOME=$(/usr/libexec/java_home -v 17)``). Use the wrapper for everything: `./gradlew build` compiles all targets and runs verification. `./gradlew desktopRun` launches the desktop client with hot reload support. Database artifacts rely on Sqldelight—run `./gradlew :shared:generateCommonMainPsychNotesDbSchema` after SQL changes, then `./gradlew :shared:verifyCommonMainPsychNotesDbMigration` to validate migrations. Use `./gradlew clean` when switching branches to avoid stale Compose caches.

## Coding Style & Naming Conventions
Follow JetBrains Kotlin style: 4-space indentation, braces on the same line, and trailing commas where helpful. Public APIs use `PascalCase` for classes and `camelCase` for functions/vals; SQL identifiers mirror the snake_case tables already in `*.sq`. Lean on immutable state, state hoisting in Compose, and dependency injection through the Koin modules in `shared/src/commonMain/kotlin/com/clinica/data`. Run IDE autoformat before committing; adopt `ktlint` if introduced later for consistent formatting.

## Testing Guidelines
Unit tests belong in `shared/src/commonTest` (create the directory if it is missing) and should use Kotlin test with Turbine for coroutine flows. Execute `./gradlew allTests` for the complete suite or scope with `./gradlew :shared:test`. Every database migration must pair with schema regeneration (`generate...Schema`) so `verify...Migration` remains green. When adding UI flows, prefer Compose preview functions or lightweight screenshot comparisons to document expected states.

## Commit & Pull Request Guidelines
No commit history ships with this snapshot, so follow Conventional Commits (e.g., `feat: support session export`) to keep future changelogs predictable. Keep commits focused on a single concern and mention any schema or configuration side effects. Pull requests should include: a concise summary, linked issue/ticket, testing notes (`./gradlew build`, targeted commands above), and screenshots or GIFs for UI work. Flag follow-up tasks and ensure Sqldelight verification passes before requesting review.
