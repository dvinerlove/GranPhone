# Source 43 — MDN: Use of color (WCAG 1.4.1)

- URL: https://developer.mozilla.org/en-US/docs/Web/Accessibility/Guides/Understanding_WCAG/Perceivable/Use_of_color
- Title: Use of color — Accessibility | MDN
- Publisher: MDN Web Docs (Mozilla; community-maintained docs; page last modified 2025-06-07)
- Tier: L2 (official/authoritative documentation summarizing WCAG)
- Ownership: independent-secondary (docs site explaining W3C norm)
- Fetch date: 2026-11-22 (via r.jina.ai full text)

## Key verbatim quotes

1. "WCAG guideline 1.4.1 on the use of color requires that 'color is not used as the only visual means of conveying information, indicating an action, prompting a response, or distinguishing a visual element.'"

2. "Use another factor in addition to color to convey information. For example, to indicate form validation errors, you can change the labels of the relevant fields by another font attribute as well as by color. Icons and symbols should differ by shape as well as by color."

3. "To accommodate users with color vision disorders ('color blindness'), be careful when using a 'stoplight' color metaphor, with green indicating 'good' values and red indicating 'bad' values. Users with red-green color blindness have trouble distinguishing red and green, and so may not be able to tell these values apart. Using an additional factor to signal 'good' vs. 'bad' is necessary."

4. "Avoid pure red and green in this situation; a reddish-orange and a bluish-green are distinguishable by color blind users, while still conveying the cultural 'good' and 'bad' meanings to users with normal color vision. Color contrast can be helpful in this context, as well, if one color is darker or lighter than the other."

## Notes
- Directly answers the "green=call / red=hang up" question: the convention is culturally strong but not a sufficient signal alone — WCAG 1.4.1 requires a redundant cue (icon shape, label, position). For a senior phone: keep green/red AND distinct handset-orientation glyphs AND text labels AND fixed positions.
