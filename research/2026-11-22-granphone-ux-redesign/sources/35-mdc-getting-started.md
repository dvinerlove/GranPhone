# Source 35 — MDC-Android getting started: SDK/build requirements

- URL: https://github.com/material-components/material-components-android/blob/master/docs/getting-started.md
- Title: Getting started with Material Components for Android
- Publisher: material-components org (Google) on GitHub — L1
- Fetched: 2026-11-22 via r.jina.ai

## Key verbatim quotes
- "In order to use the new Material3 themes and component styles, you should depend on version 1.5.0 or later."
- "In order to use the new Material3Expressive themes and component styles, you should depend on version 1.14.0 or later."
- "In order to use the latest versions of Material Components for Android and the AndroidX Jetpack libraries … `compileSdk` -> 36.1 or later … `minSdk` -> 23 or later."
- Build toolchain minimums: "Gradle version 8.13 … Android Gradle Plugin (AGP) version 8.13.0 … Android Studio Narwhal 3 Feature Drop, version 2025.1.3."
- "Use AppCompatActivity to ensure that all the components work correctly. If you are unable to extend from AppCompatActivity, update your activities to use AppCompatDelegate."
- "Starting with version 1.13.0-alpha12, the Material library is built with AGP 8.7.3 (or later) and android.nonTransitiveRClass=true."
- Dependency coordinates: `implementation 'com.google.android.material:material:<version>'` from `google()` Maven repo.

## Implication for GranPhone (noted as inference in facts)
- minSdk 21/22 (Android 5.x) devices are NOT supported by latest MDC (1.14.0); last minSdk-21 release line is 1.13.0 (see source 36). If target fleet starts at API 23 (Android 6), 1.14.0 incl. Expressive is usable; Android 8 Huawei (API 26) is unaffected either way.
