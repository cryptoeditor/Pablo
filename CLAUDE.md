# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

**Pablo** is a single-module Android app (**Kotlin + Jetpack Compose**) that is a **control GUI for a Software Defined Radio (SDR)** — radio hardware. The app connects to the radio, adjusts it, sends data, and visualizes nearby transmitters.

The repository owner is **new to programming** — prefer clear explanations over terse expert shorthand, and avoid introducing advanced patterns (DI frameworks, multi-module splits, etc.) unless asked.

**Current state:** the full three-screen UI is built and runs, but it is **not connected to real hardware** — signal data is **simulated** (`sampleNearbyRadios()` in `SignalRadar.kt`). It is being built UI-first so hardware plugs in later. The radio is intended to be **passive with data** (nothing stored at rest); only connection settings are meant to be persisted (persistence not yet implemented).

`README.md` IS now real project documentation (it was previously a generic Android learning roadmap — that's gone).

## Commands

This is a Windows environment; use the `gradlew.bat` wrapper (PowerShell). No separate lint/format toolchain beyond Gradle/Android.

```powershell
.\gradlew.bat assembleDebug          # Build the debug APK
.\gradlew.bat installDebug           # Build + install on a connected device/emulator
.\gradlew.bat compileDebugKotlin     # Fast compile check (used to verify edits)
.\gradlew.bat test                   # Run JVM unit tests (app/src/test)
.\gradlew.bat connectedAndroidTest   # Instrumented tests (needs a running emulator/device)
.\gradlew.bat lint                   # Android Lint -> report at app/build/reports/lint-results-debug.html
.\gradlew.bat clean                  # Delete build outputs
```

Run a single unit test class or method:

```powershell
.\gradlew.bat test --tests "com.example.pablo.ExampleUnitTest"
.\gradlew.bat test --tests "com.example.pablo.ExampleUnitTest.addition_isCorrect"
```

Most day-to-day building/running is from **Android Studio** (green ▶ Run). Driving the emulator from the CLI (boot, install, tap, screenshot) is done with the SDK tools under `%LOCALAPPDATA%\Android\Sdk` (`emulator\emulator.exe`, `platform-tools\adb.exe`); the AVD used in this project is `Pixel_10_Pro_XL`. The emulator tends to shut down between sessions — re-boot it before `installDebug`.

## Architecture

Everything is in module **`app`** under package `com.example.pablo`. There is **no ViewModel or persistence layer yet** — shared state is hoisted into the root composable.

- **Entry point / app shell:** [MainActivity.kt](app/src/main/java/com/example/pablo/MainActivity.kt). Holds the activity, the root `PabloApp()` composable, the `AppDestinations` enum, the **hoisted shared state** (`radioAddress`, `isConnected`, `scanIntervalSeconds`, `contacts`), and a **`LaunchedEffect` sampling loop** that calls `sampleNearbyRadios()` every `scanIntervalSeconds` while connected.
- **Navigation:** Material 3 adaptive `NavigationSuiteScaffold` driven by the `AppDestinations` enum (**CONTROL / MONITOR / SETTINGS**, each with a drawable icon in `res/drawable/`). The selected destination is a single `rememberSaveable` var; the `Scaffold` body `when`-branches on it to render the matching screen. There's a `TopAppBar` with an always-visible `ConnectionPill` (Online/Offline). **To add a screen:** add an enum entry + branch in the `when`.
- **Screens:** [RadioScreens.kt](app/src/main/java/com/example/pablo/RadioScreens.kt) — `ControlScreen`, `MonitorScreen`, `SettingsScreen`, plus shared `ConnectionPill` / `ScreenTitle` / `StatusRow` helpers. Screens receive state + callbacks (state hoisting); they own no shared state.
- **3D RF map (the centerpiece):** the Monitor screen renders nearby transmitters as irregular translucent 3D "propagation field" blobs via **Three.js (WebGL)**, NOT native Compose/Filament. [RfFieldView.kt](app/src/main/java/com/example/pablo/RfFieldView.kt) is a Compose `AndroidView` wrapping a `WebView` that loads [assets/rf_scene.html](app/src/main/assets/rf_scene.html), which uses bundled **[assets/three.min.js](app/src/main/assets/three.min.js)** (Three.js r128, pinned, offline — so no INTERNET permission). A `window.setFields(json)` JS hook is stubbed for a future Kotlin→JS data bridge but **not yet wired** to `contacts`.
- **Legacy:** [SignalRadar.kt](app/src/main/java/com/example/pablo/SignalRadar.kt) is an earlier 2D/2.5D Canvas radar, **no longer rendered** (Monitor now uses `RfFieldView`). It still holds the shared data types (`RadioContact`) and the simulated `sampleNearbyRadios()` source, so don't delete it wholesale.
- **Theming:** [ui/theme/](app/src/main/java/com/example/pablo/ui/theme/) — `PabloTheme` wraps `MaterialTheme`, defaulting to **dynamic color on Android 12+** (wallpaper-based), falling back to the `Purple/Pink` schemes in Color.kt.

### Known gotchas
- **WebView 0-height trap:** the Three.js canvas must be sized from a per-frame `clientWidth/clientHeight` check (done in `rf_scene.html`); reading size only once at startup gives a blank (0-height) canvas.
- **WebGL on emulators** can render blank; it works on this project's emulator now, but a real device is the reliable test.
- Bundled `three.min.js` is a vetted, pinned vendor file — note the owner's global rule against pulling code from public repos; keep any future JS libs pinned and bundled the same way.

### Roadmap direction (decided this project)
3D engine = **Three.js in WebView** (chosen over SceneView/Filament because the look is irregular shader-driven fields, and Three.js runs on the emulator). Planned map background = **MapLibre GL JS** (free/open, vs Mapbox's proprietary license + uncapped billing), to be added under the fields via a custom layer. Next steps: wire real data into `setFields()`, then the MapLibre map, then settings persistence, then hardware link (USB / network / Bluetooth — undecided).

## Dependencies & versioning

Gradle **version catalog** at [gradle/libs.versions.toml](gradle/libs.versions.toml) (not inline in `build.gradle.kts`). To add a library: declare version + alias in `libs.versions.toml`, reference as `libs.some.alias` in [app/build.gradle.kts](app/build.gradle.kts). Note: the 3D stack adds **no Gradle dependency** — Three.js is a bundled asset and `WebView` is part of the Android framework.

Pins **very new / bleeding-edge versions** (AGP 9.2.1, Kotlin 2.2.10, `compileSdk`/`targetSdk` 36, Compose BOM 2025.12). Some `build.gradle.kts` syntax is newer than most online examples — e.g. block-style `compileSdk { version = release(36) { ... } }` and `buildTypes { release { optimization { enable = false } } }`. Match the existing style rather than rewriting to older forms. `minSdk` is 24.
