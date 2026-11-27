# Source 38 — Edge-to-edge in Views (developer.android.com)

- URL: https://developer.android.com/develop/ui/views/layout/edge-to-edge
- Title: Display content edge-to-edge in views
- Publisher: Android Developers (Google) — L1 official docs
- Fetched: 2026-11-22 via r.jina.ai

## Key verbatim quotes
- "Once you target SDK 35 or higher on a device running Android 15 or higher, your app is displayed edge-to-edge. The window spans the entire width and height of the display by drawing behind the system bars."
- "If your app targets SDK 35 or later, edge-to-edge is automatically enabled for Android 15 devices or later. To enable edge-to-edge on previous Android versions, manually call enableEdgeToEdge in onCreate of your Activity." (androidx.core `WindowCompat.enableEdgeToEdge(window)`; Java form `WindowCompat.enableEdgeToEdge(getWindow());`)
- "By default, enableEdgeToEdge() makes the system bars transparent, except on 3-button navigation mode where the navigation bar gets a translucent scrim. The colors of the system icons and the scrim are adjusted based on the system light or dark theme."
- Inset types: "System bars insets … Display cutout insets … System gesture insets." Recommended handling via `ViewCompat.setOnApplyWindowInsetsListener` + `WindowInsetsCompat.Type.systemBars()` (optionally `| displayCutout()`), applied as padding/margin; set `clipToPadding` on RecyclerView so items scroll behind bars.
- FAB/nav bar overlap shown as the classic failure when insets aren't handled.

## Graceful-degradation read (for facts)
- On Android 8 (API 26): edge-to-edge is purely opt-in; nothing is enforced. The same `enableEdgeToEdge()` call works via AndroidX compat, but the app can simply not call it on old devices — no forced change. System bars remain opaque by default.
