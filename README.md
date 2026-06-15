# Pablo SDR

**Pablo** is an Android app that acts as a **control interface (GUI) for a Software Defined Radio (SDR)** — a piece of radio hardware. The app lets you connect to the radio, adjust it, send data, and visualize the radio environment around you.

> **Status: early work-in-progress.** The full UI is built and runnable, but it is **not yet connected to real radio hardware** — the signal data is currently **simulated**. The app is being built UI-first, so the hardware can be plugged in later without redesigning the screens.

Built with **Kotlin + Jetpack Compose**.

---

## What the app does

The app has three screens, reachable from the bottom navigation bar:

| Screen | Purpose |
|--------|---------|
| **Control** | Adjust the radio (frequency), type a message to send, and set how often the radio environment is scanned (a slider). |
| **Monitor** | A read-only dashboard: connection status, signal strength, and a **3D RF propagation map** (see below). |
| **Settings** | The radio's network address, an auto-connect toggle, and the Connect / Disconnect button. |

A status badge in the top bar (**Online / Offline**) is always visible, on every screen.

### The RF propagation map (the centerpiece)

On the **Monitor** screen, once connected, the app shows a **3D map of nearby transmitters**. You sit at the center; each transmitter is drawn as an **irregular, translucent, colored "propagation field"** — a blob whose shape represents how its signal spreads through space (RF propagation is lobed and uneven, not a clean sphere). You can **drag to orbit** the scene.

This 3D view is rendered with **Three.js (WebGL)** running inside a `WebView`. The 3D library is **bundled into the app**, so it works fully offline.

> The propagation shapes are **stylized** for now. The long-term intent: signal **strength** drives each field's size/intensity, and **direction-finding** (when the hardware supports it) drives the shape of the lobes.

### Important design constraint

The radio is **passive with respect to data** — it passes data while operating but **stores nothing at rest**. The only thing the app persists is **connection settings** (e.g. the radio address). *(Persistence is on the roadmap, not yet implemented.)*

---

## Tech stack

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Navigation | Material 3 adaptive `NavigationSuiteScaffold` |
| 3D graphics | Three.js (WebGL) in a `WebView`, bundled offline |
| Planned map background | MapLibre GL JS (free/open-source) |
| Build system | Gradle (version catalog) |
| Min / target Android | minSdk 24 / targetSdk 36 |

---

## Project structure

Everything lives in the `app` module under `com.example.pablo`.

```
app/src/main/
├── java/com/example/pablo/
│   ├── MainActivity.kt    App shell: the activity, the 3-tab navigation,
│   │                      the shared state, and the background scan loop.
│   ├── RadioScreens.kt    The three screens (Control / Monitor / Settings)
│   │                      plus the Online/Offline status badge.
│   ├── RfFieldView.kt     The WebView that hosts the 3D RF field scene.
│   ├── SignalRadar.kt     An older 2D/2.5D radar (no longer used; kept for
│   │                      reference). Also holds the simulated-data source.
│   └── ui/theme/          App theme (colors, typography).
└── assets/
    ├── rf_scene.html      The Three.js 3D scene (the RF propagation fields).
    └── three.min.js       Bundled Three.js library (r128, pinned).
```

**How the data flows:** `MainActivity` holds the shared state (connection, address, scan interval, the list of detected radios). While connected, a background loop calls `sampleNearbyRadios()` every few seconds to refresh the data. The screens just *display* that state and report user actions back up (this pattern is called *state hoisting*).

---

## Building & running

This is a Windows project — use the `gradlew.bat` wrapper from PowerShell. Most of the time you'll just press the green **▶ Run** button in **Android Studio**.

```powershell
.\gradlew.bat assembleDebug     # Build the debug APK
.\gradlew.bat installDebug      # Build + install on a connected device/emulator
.\gradlew.bat test              # Run JVM unit tests
.\gradlew.bat lint              # Android Lint
.\gradlew.bat clean             # Delete build outputs
```

**To see the 3D RF map:** run the app, go to **Settings → Connect**, then open the **Monitor** tab. The 3D fields appear once connected.

> Note: the 3D view needs WebGL. It works on most emulators and on real devices; if an emulator shows a blank map, test on a physical phone.

---

## Roadmap

- [x] Three-screen UI (Control / Monitor / Settings) with shared state
- [x] Connection status wired across screens
- [x] 3D RF propagation fields rendering in-app (Three.js / WebView)
- [ ] Feed the *real* sampled signal data into the 3D scene (bridge Kotlin → JavaScript)
- [ ] Add a real geographic map under the fields (MapLibre)
- [ ] Persist connection settings between launches
- [ ] Connect to actual SDR hardware (link method — USB / network / Bluetooth — still to be decided)
- [ ] Voice input on the Control screen
