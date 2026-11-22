# Source 37 — MDC-Android Color theming doc (dynamic color behavior)

- URL: https://raw.githubusercontent.com/material-components/material-components-android/master/docs/theming/Color.md (published at https://github.com/material-components/material-components-android/blob/master/docs/theming/Color.md)
- Publisher: material-components org (Google) — L1 project docs
- Fetched: 2026-11-22 direct curl

## Key verbatim quotes
- "Starting from Android S, the framework provides the ability to support dynamic colors in your UI based on the user's wallpaper or color choice on the device." (Android S = API 31 / Android 12)
- "To help in the application of dynamic colors, the Material 3 library provides 3 theme overlays to be used on the base Material 3 themes: ThemeOverlay.Material3.DynamicColors.Light / .Dark / .DayNight."
- "com.google.android.material.color.DynamicColors … DynamicColors.applyToActivitiesIfAvailable(this);" — "if the app is running on Android S+ it will attempt to apply the dynamic color theme overlay".
- "If the app is running on Android S+, dynamic colors will be applied to the activity."
- "DynamicColors.applyToActivityIfAvailable(this);" for per-activity use; `DynamicColors.wrapContextIfAvailable(context)` for fragment/view.
- Auto-generated mapping table: each role has "Light baseline" (e.g., Primary = primary40) vs "Light dynamic 31-33" (system_accent1_600) vs "Light dynamic 34+" (system_primary_light) — i.e., M3 baseline palette tokens exist for all roles and are the fallback when dynamic color is absent.
- "As of version 1.11.0-alpha02 and above, Material3 components will use the following tonal surface color roles by default (instead of elevation overlays): colorSurfaceContainer / Low / High / Lowest / Highest / colorSurfaceDim."

## Relevance
- Fixed vs dynamic color, concrete answer: with MDC-Android, one codebase can ship baseline M3 theme; DynamicColors no-ops below API 31 → pre-Android-12 devices automatically get the static baseline scheme. No separate theme needed.
