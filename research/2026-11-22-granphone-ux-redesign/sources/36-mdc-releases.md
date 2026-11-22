# Source 36 — MDC-Android releases: versions & minSdk history

- URL: https://github.com/material-components/material-components-android/releases
- Publisher: material-components org (Google) on GitHub — L1
- Fetched: 2026-11-22 via r.jina.ai

## Key verbatim quotes
- Latest stable heading on page: "## 1.14.0 — New in 1.14.0!" listing Expressive Themes, Expressive Lists, Expressive Button/Icon Button/Button Group/FAB/Top App Bar/Navigation Bar/Navigation Rail/Search/Progress Indicator/Slider styles, Emphasized Typescale.
- 1.14.0 "Important": "Required `minSdkVersion` is now 23 or higher, for Material and AndroidX." / "Library is now built with Android Gradle Plugin (AGP) 8.11.1 and Gradle 8.13."
- 1.14.0-alpha09 changelog: "Update minSdkVersion to 23" (commit cd3f114) — the bump happened in the 1.14.0 pre-release line.
- 1.13.0 "Important": "Required `minSdkVersion` is now 21 or higher, for Material and AndroidX." — i.e., 1.13.0 still supports API 21.
- 1.13.0 note: "Material 3 Expressive has moved to the 1.14.0 versions of the library."
- 1.13.0 added: DockedToolbarLayout, FloatingToolbarLayout, LoadingIndicator, MaterialSplitButton, MaterialButtonGroup.
- 1.13.0 a11y fixes include "Prevent hide on scroll when Talkback is on".

## Maven metadata (L1, Google Maven)
- URL: https://dl.google.com/dl/android/maven2/com/google/android/material/material/maven-metadata.xml
- `<latest>1.14.0</latest>`, `<release>1.14.0</release>`, `<lastUpdated>20260513150004</lastUpdated>` — stable 1.14.0 shipped ~2026-05.
- androidx.compose.ui:ui metadata: `<release>1.14.0-alpha01</release>`; latest stable listed: 1.13.0.
