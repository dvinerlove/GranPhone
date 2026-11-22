# Source 30 — M3 color roles + dynamic color

## Page A
- URL: https://m3.material.io/styles/color/roles
- Title: Color roles — Material Design 3
- Publisher: Material Design (Google) — L1 official
- Ownership: vendor-authored (Google design system docs)
- Fetched: 2026-11-22 via r.jina.ai

### Key verbatim quotes
- "Color roles are like the 'numbers' in a paint-by-number canvas. They're the connective tissue between elements of the UI and what color goes where."
- "You'll use these color roles whether you're using the static baseline scheme … or dynamic color."
- "The color system is built on accessible color pairings … These color pairs provide an accessible minimum 3:1 contrast."
- "To ensure accessible visual contrast in your app, apply colors only in the intended pairs or layering orders described in the following sections. Combining colors improperly may break contrast necessary for visual accessibility, particularly when colors are adjusted through dynamic color features such as user-controlled contrast."
- "Primary roles are for important actions and elements needing the most emphasis … Secondary roles are for elements that don't need immediate attention … Tertiary roles are for smaller elements that need special emphasis."
- "Error is an example of a static color (it doesn't change even in dynamic color schemes). Error color roles are made static by default with any dynamic color scheme."
- "There are also five surface container roles named based on their level of emphasis: Surface container lowest / low / (default) / high / highest."
- "The most common combination of surface roles uses surface for a background area and surface container for a navigation area."
- Outline roles: "Outline: Important boundaries, such as a text field outline. Outline variant: Decorative elements, such as dividers" — Don't use outline for dividers/cards.
- Diagram caption: "all 45 color roles including Primary, Secondary, Tertiary, Error, Surfaces, Inverse roles, Scrim and Shadow roles."

## Page B
- URL: https://m3.material.io/styles/color/dynamic-color/overview (final URL: /styles/color/dynamic/choosing-a-source)
- Title: Dynamic color — Material Design 3
- Publisher: Material Design (Google) — L1 official
- Fetched: 2026-11-22 via r.jina.ai

### Key verbatim quotes
- "Dynamic color can change a color palette to match user settings, like wallpapers, or in-product content."
- "There are two ways your product can get a source color: 1. User-generated color from a user's wallpaper 2. Content-based color from in-app content like a music album or book cover."
- "Both types of dynamic color are accessible and personalized."
- Site nav links a "Static: Baseline" page — baseline static scheme is the non-dynamic counterpart (see also Page A baseline note).

### Notes
- Dynamic color availability on Android is API 31+; MDC-Android's DynamicColors helper only applies on Android S+ (see source 37). On pre-12 devices the static baseline scheme is what renders.
