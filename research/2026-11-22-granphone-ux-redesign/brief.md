# Brief — GranPhone UX/UI Redesign Research

## Question
How should GranPhone's UX/UI be redesigned to look modern and professional while
staying usable by its core audience: elderly people, visually impaired users, and
people who have never used a smartphone (author's stated goal — a phone for a
person who never held a phone)?

## Constraints (from user, 2026-11-22)
- Audience: elderly / visually impaired / disabled. Big targets, minimal steps,
  high contrast are FEATURES, not bugs. "Modern" = polished, consistent,
  professional — NOT denser or gesture-heavy.
- Full redesign allowed: flows, navigation, information architecture — everything.
- Technical ceiling: any (XML/Views now; Material Components or even Compose
  discussed as options).
- MUST work on old devices: target device class = Huawei 2017 (EMUI 8, Android 8),
  weak hardware, small/low-quality screens. No heavy effects, keep low minSdk,
  EMUI quirks (aggressive battery kill, OEM launcher behavior) must be considered.
- Code: Java + XML Views forked from BaldPhone; applicationId already renamed to
  com.granphone.launcher; versionName 14.0.0.

## Deliverable
One report (Russian): what to change and how — design system proposal, per-screen
redesign directions, user-flow changes, competitor patterns worth borrowing,
technical path (Views+Material3 vs Compose), prioritized roadmap.

## Classification
Decision / design-research hybrid. Time-sensitivity: medium (Android design
guidelines evolve, competitor apps are stable). Sources ≤2y preferred for
guidelines; competitor analysis is evergreen.
