# Source 12 — Android Developers: "Build more accessible apps"

- URL: https://developer.android.com/guide/topics/ui/accessibility/apps
- Title: Build more accessible apps | Android Developers
- Publisher: Google / Android Developers
- Tier: L1/L2 (official platform documentation)
- Ownership: vendor-authored (platform guidance)
- Fetch date: 2026-11-22 (via r.jina.ai, full read)

## Key verbatim quotes

1. "Your app's UI is easier to use if its controls are easier to see and tap. For touch interfaces, we recommend that each interactive UI element have a focusable area, or touch target size, of at least 48dpx48dp. Larger is even better."
2. Contrast thresholds: "If the text is smaller than 18sp, or if the text is bold and smaller than 14sp, use foreground and background colors that result in a color contrast ratio of at least 4.5:1." ; "For all other text, set the color contrast ratio to at least 3:1."
3. Section heading: "Use large, simple controls" — under which the 48dp recommendation sits.
4. On semantics: "For each UI element in your app, include a description that describes the element's purpose."
5. "For precise input (mouses and trackpads), the touch target can be smaller."

## Notes
- Android's concrete recommendation = 48×48dp minimum focusable touch area; "Larger is even better" — for a senior-first launcher, exceed it.
- Contrast thresholds mirror WCAG (4.5:1 small text, 3:1 large text) with sp-based size boundaries (<18sp regular, <14sp bold).
