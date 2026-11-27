# Source 42 — Understanding WCAG SC 1.4.11 Non-text Contrast

- URL: https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast
- Title: Understanding Success Criterion 1.4.11: Non-text Contrast (WCAG 2.2)
- Publisher: W3C / Web Accessibility Initiative (WAI)
- Tier: L1 (official standard documentation)
- Ownership: independent-primary (standards body)
- Fetch date: 2026-11-22 (via r.jina.ai full text)

## Key verbatim quotes

1. Normative text: "The visual presentation of the following have a contrast ratio of at least 3:1 against adjacent color(s): User Interface Components: Visual information required to identify user interface components and states... Graphical Objects: Parts of graphics required to understand the content, except when a particular presentation of graphics is essential to the information being conveyed."

2. "The intent of this success criterion is to ensure that user interface components (i.e., controls) and meaningful graphics are distinguishable by people with moderately low vision."

3. "The term 'graphical object' applies to stand-alone icons such as a print icon (with no text), and the important parts of a more complex diagram such as each line in a graph. For simple graphics such as single-color icons the entire image is a graphical object."

4. "People with low vision often have difficulty perceiving graphics that have insufficient contrast. This can be exacerbated if the person has a color vision deficiency that lowers the contrast even further. Providing a relative luminance (lightness difference) of 3:1 or greater can make these items more distinguishable when the person does not see a full range of colors."

5. "If a control has visible content (such as text or a sufficiently contrasting icon), which helps users identify the presence of the control, then a border or other indication of the overall boundary of the hit area is not required." — i.e., WCAG does NOT require a visible button boundary if the content itself is identifiable.

6. Worked example: "The phone icon is a simple shape within the orange (#E3660E) circle. The meaning can be understood from that icon alone, the background behind the circle is irrelevant. The orange background and the white icon have a contrast ratio greater than 3:1, which passes."

7. "Because authors do not have control over user settings for font smoothing and anti-aliasing... Due to anti-aliasing, particularly thin lines and shapes of non-text elements may be rendered with reduced contrast." — implication: thin-outline icons are at risk; bold/filled strokes are safer.

## Notes
- Applies to web content but routinely adopted as the mobile-app contrast benchmark (Android a11y guidance references WCAG). For GranPhone: any icon stroke/glyph conveying meaning must hit 3:1 vs its background; UI-component identification also 3:1.
