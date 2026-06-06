# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

**Pablo** is a single-module Android app built with **Kotlin + Jetpack Compose**. It is currently very close to the Android Studio "Empty Activity" template: a starting skeleton, not a feature-complete app. The repository owner is **new to programming** — prefer clear explanations over terse expert shorthand, and avoid introducing advanced patterns (DI frameworks, multi-module splits, etc.) unless asked.

`README.md` is **not** project documentation — it is a beginner's roadmap for building Android apps in general. Use it to understand the owner's intended learning path (UI → ViewModel → Room/DataStore), not the current state of the code.

## Commands

This is a Windows environment; use the `gradlew.bat` wrapper (PowerShell). There is no separate lint/format toolchain configured beyond what Gradle/Android provide.

```powershell
.\gradlew.bat assembleDebug          # Build the debug APK
.\gradlew.bat installDebug           # Build + install on a connected device/emulator
.\gradlew.bat test                   # Run JVM unit tests (app/src/test)
.\gradlew.bat connectedAndroidTest   # Run instrumented tests (needs a running emulator/device)
.\gradlew.bat lint                   # Android Lint -> report at app/build/reports/lint-results-debug.html
.\gradlew.bat clean                  # Delete build outputs
```

Run a single unit test class or method:

```powershell
.\gradlew.bat test --tests "com.example.pablo.ExampleUnitTest"
.\gradlew.bat test --tests "com.example.pablo.ExampleUnitTest.addition_isCorrect"
```

Most day-to-day building and running is done from **Android Studio** (green ▶ Run button), which is the primary workflow here.

## Architecture

Everything is in module **`app`** under package `com.example.pablo`. There is currently **no ViewModel and no data/persistence layer** — all logic is UI-only.

- **Entry point:** [MainActivity.kt](app/src/main/java/com/example/pablo/MainActivity.kt). `MainActivity.onCreate` calls `setContent { PabloTheme { PabloApp() } }`. This one file holds the activity, the root composable, the navigation enum, and the screen content.
- **Navigation pattern (the one non-obvious piece):** `PabloApp()` uses Material3's adaptive `NavigationSuiteScaffold` driven by the `AppDestinations` enum (HOME / FAVORITES / PROFILE). The selected destination is held in a single `rememberSaveable` state var (`currentDestination`). **To add a screen:** add an entry to the `AppDestinations` enum (label + a drawable icon in `res/drawable/`), then branch on `currentDestination` inside the `Scaffold` body to render that screen's content. Right now the body always renders `Greeting` regardless of destination — wiring per-destination content is the natural next step.
- **Theming:** [ui/theme/](app/src/main/java/com/example/pablo/ui/theme/) — `PabloTheme` (Theme.kt) wraps `MaterialTheme`. It defaults to **dynamic color on Android 12+** (pulls colors from the device wallpaper), falling back to the hardcoded `Purple/Pink` schemes from Color.kt on older devices.

## Dependencies & versioning

Dependencies are managed with a **Gradle version catalog** at [gradle/libs.versions.toml](gradle/libs.versions.toml), not inline in `build.gradle.kts`. To add a library: declare the version + library alias in `libs.versions.toml`, then reference it as `libs.some.alias` in [app/build.gradle.kts](app/build.gradle.kts).

This project pins **very new / bleeding-edge versions** (AGP 9.2.1, Kotlin 2.2.10, `compileSdk`/`targetSdk` 36, Compose BOM 2025.12). Be aware that some `build.gradle.kts` syntax here is newer than most online examples — e.g. the block-style `compileSdk { version = release(36) { ... } }` and `buildTypes { release { optimization { enable = false } } }`. Match the existing style rather than rewriting to older forms. `minSdk` is 24.
