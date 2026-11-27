# Source 33 — M3 motion system + reduced motion

## Page A
- URL: https://m3.material.io/styles/motion/overview (final: /styles/motion/overview/how-it-works)
- Title: Motion — Material Design 3
- Publisher: Material Design (Google) — L1 official
- Fetched: 2026-11-22 via r.jina.ai

### Key verbatim quotes
- "May 2025 — Material introduced the motion physics system with M3 Expressive. This new physics-based system makes interactions and transitions feel more alive, fluid, and natural … The physics system is replacing the previous system based on easing and duration."
- "The physics system has two preset motion schemes: expressive and standard."
- "Expressive is Material's opinionated motion scheme, and should be used for most situations … Standard feels more functional with minimal bounce, and should be used for utilitarian products."
- "On Jetpack Compose, 21 Material components use the motion physics system by default. Android Views (MDC-Android) support is coming soon."
- Availability table: "Android Views (MDC-Android): Available. Not added to components." / "Jetpack Compose: Available" / "Flutter: Unavailable".
- "Motion schemes use springs. A spring is a combination of three attributes … stiffness, damping, and initial velocity."

## Page B
- URL: https://m3.material.io/styles/motion/transitions/applying-transitions
- Title: Transitions — Material Design 3
- Publisher: Material Design (Google) — L1 official
- Fetched: 2026-11-22 via r.jina.ai

### Key verbatim quotes (reduced motion)
- "Most platforms have a reduced animation setting to help users with a sensitivity to motion. If that setting is on, transitions should: Use subtle fades instead of intense sliding or scaling animations; Disable decorative effects like parallax or shape morphing."
- "Jump cuts should generally be avoided as a default setting since they can be disorienting."
- "Fully fade out content before fading new content in."
- Note on page: "M3 transitions use the legacy easing and duration system. They'll eventually be updated to use the motion physics system."

### Relevance
- Reduced-motion support is a stated M3 requirement; on weak 2017 hardware, honoring the OS "remove animations" setting also saves GPU work (interpretation, flagged in facts).
