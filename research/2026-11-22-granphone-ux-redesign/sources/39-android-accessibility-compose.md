# Source 39 — Android accessibility guidance + Compose pages (developer.android.com bundle)

All pages publisher: Android Developers (Google), L1 official docs. Fetched 2026-11-22 via r.jina.ai.

## Page A — Make apps more accessible
- URL: https://developer.android.com/guide/topics/ui/accessibility/apps (last updated 2026-11-10)
- Quotes:
  - "For touch interfaces, we recommend that each interactive UI element have a focusable area, or touch target size, of at least 48dpx48dp. Larger is even better."
  - "In Jetpack Compose, many built-in Material components like Button, IconButton, and ListItem already enforce this minimum size."
  - "If the text is smaller than 18sp, or if the text is bold and smaller than 14sp, use foreground and background colors that result in a color contrast ratio of at least 4.5:1. For all other text, set the color contrast ratio to at least 3:1."
  - contentDescription required for meaningful graphic elements; "Android accessibility services (like TalkBack) automatically announce the text itself" for Text.

## Page B — Accessibility in Jetpack Compose (hub, updated 2026-06-13)
- URL: https://developer.android.com/develop/ui/compose/accessibility
- Quote: "Compose provides a foundation for building more accessible UIs with its declarative APIs." Key topics listed: API defaults, Semantics, traversal order, "Support user-scalable content", merging/clearing semantics, testing.

## Page C — Compose accessibility API defaults
- URL: https://developer.android.com/develop/ui/compose/accessibility/api-defaults
- Quotes:
  - "Any on-screen element that someone can click, touch, or interact with must be large enough for reliable interaction. When sizing these elements, make sure to set the minimum size to 48dp to correctly follow the Material Design accessibility guidelines."
  - "Material components—like Checkbox, RadioButton, Switch, Slider, and Surface—set this minimum size internally, but only when the component can receive user actions."
  - "When the size of a clickable composable is smaller than the minimum touch target size, Compose still increases the touch target size. It does so by expanding the touch target size outside of the boundaries of the composable."
  - "`minimumInteractiveComponentSize` … reserves at least 48 dp of layout space in each dimension and centers the element in that space."
  - "The accessibility label … contentDescription parameter describes a graphic element. Use a localized string." Decorative images: set contentDescription to null.

## Page D — Jetpack Compose performance (updated 2026-11-13)
- URL: https://developer.android.com/develop/ui/compose/performance
- Quotes:
  - "Build in Release Mode with R8 … Debug mode is useful for spotting many problems, but it imposes a performance cost."
  - "Use Baseline Profiles: Baseline Profiles improve performance by precompiling code for critical user journeys. Compose includes a default profile, but ideally, you should create an app-specific one as well."
  - Best practices: use `remember` to cache expensive calculations; phases/stability docs.

## Page E — Compose quick start / setup
- URL: https://developer.android.com/develop/ui/compose/setup
- Quotes:
  - "In the Language drop-down menu, Kotlin is the only available option because Jetpack Compose works only with classes written in Kotlin."
  - "In the Minimum API level drop-down menu, select API level 21 or higher."
