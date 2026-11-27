# Source 34 — Material Components for Android: maintenance mode

- URL: https://github.com/material-components/material-components-android (README)
- Publisher: material-components org (Google-maintained) on GitHub — L1 (project's own canonical statement)
- Ownership: vendor-authored
- Fetched: 2026-11-22 via r.jina.ai

## Key verbatim quotes
- Repo title/about: "[MAINTENANCE MODE] Modular and customizable Material Design UI components for Android"
- "At Google I/O 2026, Material Android announced that we're 'all-in' on Compose, alongside the official Android Compose-first announcement, marking a new chapter in Android development."
- "This means that the Views-based Material Components for Android library (MDC-Android) is now in maintenance mode."
- "There are no more planned feature releases for Views, so all projects using the Views library should begin or continue migrating to Compose to get the latest Material Design and Android platform updates."
- Pointers given by Google: Compose-first blog (android-developers.googleblog.com 2026/05), m3.material.io blog "Material Android is Compose-first", Compose Material 3 docs, "Migrate XML Views to Jetpack Compose" guide.

## Companion (vendor-authored L2) — Compose-first blog
- URL: https://android-developers.googleblog.com/2026/05/android-ui-development-is-compose-first.html
- Title: "Android UI Development is Compose First", posted by Nick Butcher, 19 May 2026
- Publisher: Android Developers Blog (Google) — L2 official blog
- Fetched: 2026-11-22 direct curl
- Quotes:
  - "Compose has matured into the standard for Android UI development—we believe that all Android UI should be built with Compose; we call this going Compose First. From today, we'll provide all APIs, libraries, tools and guidance in Compose."
  - "We now consider the View components that Compose replaces (components in the android.widget package) to be in maintenance mode. We have no plans to deprecate or remove View components and will continue to support them with critical bug fixes, but they will receive no new features."
  - "Highly performant: Out of the box, Compose offers native performance."
