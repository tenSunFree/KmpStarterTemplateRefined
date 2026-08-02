# luma-lang-kmp

[![KMP CI](https://github.com/tenSunFree/luma-lang-kmp/actions/workflows/ci.yml/badge.svg)](https://github.com/tenSunFree/luma-lang-kmp/actions/workflows/ci.yml)
[![Codecov](https://codecov.io/gh/tenSunFree/luma-lang-kmp/graph/badge.svg)](https://codecov.io/gh/tenSunFree/luma-lang-kmp)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.1-4285F4?logo=jetpackcompose&logoColor=white)](https://kotlinlang.org/compose-multiplatform)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Modular-4CAF50)](#architecture)
[![State](https://img.shields.io/badge/State-MVI%20%2B%20StateFlow-1565C0)](#architecture)
[![DI](https://img.shields.io/badge/DI-Koin-FF7043)](https://insert-koin.io)
[![Networking](https://img.shields.io/badge/Networking-Ktor-087CFA?logo=ktor&logoColor=white)](https://ktor.io)
[![CodeRabbit Reviews](https://img.shields.io/badge/Code%20Review-CodeRabbit-FF6B35)](https://coderabbit.ai)

---

## Introduction

If you're interested in Kotlin Multiplatform (Compose Multiplatform) with Clean Architecture, feel free to take a look.

Kmp-Starter-Template  
https://github.com/DevAtrii/Kmp-Starter-Template

This project is for learning and technical practice.

It can also be paired with my Go backend boilerplate to demonstrate a full-stack mobile application architecture.

---

## Related Backend

This project can be used together with my Go backend boilerplate:

- [luma-lang-go](https://github.com/tenSunFree/luma-lang-go)

The backend project provides a RESTful API foundation built with Go, Gin, PostgreSQL, sqlx, Redis, JWT, Docker, and Clean Architecture.

It can serve as the server-side foundation for authentication, user management, API development, and backend infrastructure practice.

---

## Preview

<p align="left">
  <img src="https://i.postimg.cc/yxdwSqY7/Screenshot-20260503-223226.png" width="160"/>
  <img src="https://i.postimg.cc/zvB9RmXN/Screenshot-20260503-223231.png" width="160"/>
  <img src="https://i.postimg.cc/CLtDy23W/Screenshot-20260512-004606.png" width="160"/>
  <img src="https://i.postimg.cc/0jQLJg5T/Screenshot-20260503-223240.png" width="160"/>
  <img src="https://i.postimg.cc/5y0ZFcN1/Screenshot-20260503-223243.png" width="160"/>
</p> 
<p align="left">
  <img src="https://i.postimg.cc/xdzn1Qqm/Screenshot-20260522-011118.png" width="160"/>
  <img src="https://i.postimg.cc/6QvtpKTG/Screenshot-20260522-011125.png" width="160"/>
  <img src="https://i.postimg.cc/JhkMz8tZ/Screenshot-20260522-011129.png" width="160"/>
  <img src="https://i.postimg.cc/tgxp4b7F/Screenshot-20260522-011138.png" width="160"/>
  <img src="https://i.postimg.cc/nLDphxMv/Screenshot-20260522-011142.png" width="160"/>
</p> 
<p align="left">
  <img src="https://i.postimg.cc/bvtpwPdx/Screenshot-20260522-011228.png" width="160"/>
  <img src="https://i.postimg.cc/P5cdHJfz/Screenshot-20260522-011306.png" width="160"/>
  <img src="https://i.postimg.cc/j2BcbSzw/Screenshot-20260603-020043.png" width="160"/>
  <img src="https://i.postimg.cc/65GzhCXC/Screenshot-20260603-020117.png" width="160"/>
</p> 
<p align="left">
  <img src="https://i.postimg.cc/7ZQxjJ3v/Screenshot-20260618-070333.png" width="160"/>
  <img src="https://i.postimg.cc/MTxdpRYD/Screenshot-20260703-025741.png" width="160"/>
</p> 
<p align="left">
  <img src="https://i.postimg.cc/pVcgJTSC/Screenshot-20260619-033509.png" width="160"/>
</p> 

---

## Features

### Architecture

- Kotlin Multiplatform with `commonMain` shared business logic, UI foundation, and `expect`/`actual` platform contracts
- Modular Clean Architecture: feature-isolated Gradle modules with independent data, domain, and presentation layers
- State management with a typed `MviViewModel<STATE, ACTIONS, EVENTS>` and `StateFlow`-based immutable state transitions
- Type-safe Navigation3 back stack with `@Serializable` NavKey routes and ResultStore for screen-to-screen results
- Dependency injection with Koin, applied consistently across modules via reusable Gradle convention plugins

### Secure Storage

- Android: Secure session storage using DataStore + Tink AEAD AES256-GCM backed by Android Keystore
- iOS: Secure credential storage using Keychain (`kSecClassGenericPassword`) with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`

### Lesson Player

- YouTube video playback via `android-youtube-player:core:13.0.0`
- Custom playback controls (play / pause / seek) with YouTube native controls hidden
- Time-synced bilingual captions (English + Traditional Chinese) driven by API response (`startMs` / `endMs`)
- Auto-scroll caption list to highlight the current caption in real time
- Vocabulary teaching items linked to specific caption timestamps
- Playback progress timer managed by ViewModel with periodic progress updates
- Seek-only sync to YouTube player via `seekToMs`, preventing frame drops caused by continuous seek calls

### Live Streaming

- Real-time live video streaming via Agora RTC SDK
- Audience role with automatic subscription to remote audio and video streams
- Remote video rendering through native `SurfaceView` embedded in Compose via `AndroidView`
- Stable video rendering across Compose recomposition and tab switching
- Handles early-join and late-join scenarios via Agora video state callbacks
- Teacher PiP overlay with visibility toggle
- Chat / Participants tab panel with message list and input bar

### Local Pre-Push Checks

- Git pre-push hook (`scripts/pre-push`) that mirrors CI checks locally before every push
- Runs Android Lint, unit tests, and debug build via `scripts/check.sh`
- Blocks the push automatically if lint, test, or build fails
- One-time setup per clone: `cp scripts/pre-push .git/hooks/pre-push && chmod +x .git/hooks/pre-push scripts/check.sh`

### Testing & Coverage

- Code coverage collected via [Kover](https://github.com/Kotlin/kotlinx-kover), aggregated at the root project across all modules
- Coverage reports uploaded to [Codecov](https://codecov.io/gh/tenSunFree/luma-lang-kmp) on every CI run
- Patch coverage on new code enforced at 70%; overall project coverage may not drop more than 1% per PR
- Generated code (Room `_Impl`, `BuildConfig`, Compose `@Preview` functions) excluded from coverage metrics
- Run locally: `./gradlew :koverXmlReport` — outputs to `build/reports/kover/report.xml` and `build/reports/kover/html/index.html`

---

## Tech Stack

- Kotlin Multiplatform  
  Shared Android / iOS application foundation (Uses commonMain for shared business logic, UI foundation, persistence abstractions, and expect/actual contracts to isolate platform-specific providers such as DataStore, database setup, intents, screen utilities, and native integrations)
- Modular Clean Architecture  
  Feature-isolated layered Gradle module graph (Organizes features into independent data, domain, and presentation modules, where domain owns repository contracts and use-case logic, data provides concrete implementations, and presentation depends inward on domain APIs instead of reaching data directly)
- MVI ViewModel  
  Unidirectional state management with typed UI contracts (Uses MviViewModel<STATE, ACTIONS, EVENTS> to model screen state, user actions, and one-time events, forcing UI interactions through a centralized onAction() entry point while updating state through StateFlow-based immutable state transitions)
- Navigation3 with ResultStore  
  Type-safe Navigation3 back stack infrastructure (Defines routes as @Serializable NavKey screens, registers screen subclasses for saveable back stack restoration, renders destinations through NavDisplay with Koin entry providers, and wraps navigation operations inside StarterNavigator with ResultStore support for screen-to-screen result passing)
- Build Logic Convention Plugins  
  Programmatic Gradle module standardization (Implements reusable Plugin<Project> convention plugins such as CommonPlugin, ComposeMultiplatformPlugin, KoinPlugin, and KoinComposePlugin to apply shared plugins, dependencies, and Compose/Koin configuration consistently across feature modules)
- Secure Storage  
  Cross-platform credential protection with platform-native security backends (Android encrypts session data with Tink AEAD AES256-GCM backed by Android Keystore; iOS persists credentials in the system Keychain using `kSecClassGenericPassword` with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`)
- android-youtube-player Integration  
  Native Android YouTube embedding via `YouTubePlayerView` inside Compose using `android-youtube-player:core:13.0.0`. Uses `IFramePlayerOptions` to hide native controls, blocks direct interaction on the video surface with a transparent overlay, and synchronizes play / pause / seek commands from Compose state.
- Agora RTC Integration  
  Real-time live video streaming via Agora RTC SDK with Compose lifecycle management.
  Implements expect/actual `LiveRtcVideoView` for platform-specific rendering, manages
  `RtcEngine` lifecycle with `DisposableEffect`, embeds native `SurfaceView` through
  `AndroidView`, and handles remote stream arrival via Agora callbacks to support both early-join and late-join scenarios.
- Kover + Codecov  
  Multi-module code coverage aggregation and reporting. Applies the Kover Gradle plugin
  across all subprojects via a root-level aggregation module, filters out generated code
  (Room `_Impl`, `BuildConfig`, Compose `@Preview` functions) from coverage metrics, and
  uploads merged XML reports to Codecov on every CI run with patch-level (70%) and
  project-level (auto, ±1%) coverage gates.

---

## Environment

- Kotlin: `2.3.10`
- Compose Multiplatform: `1.10.1`
- Ktor: `3.4.0`
- Koin: `4.2.0-alpha3`
- Android Gradle Plugin: `9.0.1`
- Android compileSdk: `36`
- Android minSdk: `24`
- Android targetSdk: `36`

---

## Credits

This project is created for independent learning and demonstration purposes.
Special thanks to the original author for their open-source contribution.

---

## Notes

Image resources are for learning and purposes only. Please do not use them for commercial purposes.

If there is any infringement, please contact me for removal. Thank you.

---

## License & Disclaimer

This repository is intended for learning, demonstration, and portfolio purposes.

Unless otherwise specified, the source code in this repository is either authored by the repository owner, adapted from properly licensed open-source templates, or built upon third-party open-source libraries according to their respective license terms.

If a `LICENSE` file is included in this repository, the source code is licensed under the terms specified in that file. If no `LICENSE` file is provided, all rights are reserved by default — please contact the repository owner before reusing, modifying, or distributing any code.

Any third-party assets, APIs, fonts, icons, images, videos, audio files, trademarks, product names, company names, open-source templates, libraries, or other materials used in this project belong to their respective owners and are subject to their original licenses, terms, and usage restrictions.

This project may follow common product patterns, UI patterns, or implementation approaches for educational and demonstrative purposes. It is not intended to replicate, replace, or compete with any commercial product. This repository is not affiliated with, endorsed by, or sponsored by any third-party company, product, service, or brand, unless explicitly stated.

If you have any concerns regarding copyright, trademark, license compliance, or third-party material usage, please open an issue or contact the repository owner — relevant content will be reviewed and addressed promptly.

---

## Project Structure

```
luma-lang-kmp/
├── androidApp/                 # Android entry point (application module)
├── composeApp/                 # Compose Multiplatform shared UI entry point
├── build-logic/                # Gradle convention plugins (CommonPlugin, KoinPlugin, ComposeMultiplatformPlugin, KoinComposePlugin)
├── starter/                    # Shared foundation modules
│   ├── core/                   # Core shared utilities and contracts
│   ├── utils/                  # General-purpose utilities
│   ├── native/bindings/        # Native (iOS) interop bindings
│   ├── ui/utils/                # UI-related utilities
│   ├── ui/components/           # Shared Compose UI components
│   ├── ui/layouts/               # Shared Compose layouts
│   └── resources/               # Shared multiplatform resources
└── features/
    ├── auth/                   # data / domain / presentation
    ├── lessons/                 # data / domain / presentation
    ├── live/                     # data / domain / presentation
    ├── purchases/                # data / domain / presentation
    ├── remote_config/            # data / domain / presentation
    ├── notifications/            # core / local / push
    ├── analytics/                 # data / domain
    ├── database/                   # Room (KSP) database module
    ├── navigation/                  # Navigation3 routing infrastructure
    └── core/                        # data / domain / presentation (shared core feature)
```

Each `features/<name>` module follows a consistent layering:

- `domain` — repository contracts, use-case logic, platform-agnostic models
- `data` — concrete implementations, remote/local data sources, DTOs
- `presentation` — ViewModels, Compose screens, UI state

---