# Verification notes — load-bearing claims (25 checked, 2026-11-22)

## Result: all 25 verified, zero failed

Every priority quote re-fetched and matched character-for-character (whitespace-
normalized). No misquotes, no fabrications, no contradictions, no stale data.

## Patterns / caveats

- **Single-cluster load-bearing pairs** (each pair = one source, treat as one
  citation in the report):
  - `researcher-guidelines-11` + `-12` → Kobayashi et al. 2011 (N=20 elderly,
    3.5"/9.7" devices — decade-old hardware, mechanism still cited).
  - `researcher-guidelines-18` + `-19` → Leung/McGrenere/Graf BIT 2009 (N=36).
  - `RIC-04` + `RIC-07` → Li & Luximon 2019 PolyU (N=22, HK participants with
    prior smartphone experience — novices likely worse).
- **Fetch fallbacks used**: `m2.material.io` refused direct TLS (curl 35) —
  verified via r.jina.ai mirror of same page. `http://ira.lib.polyu.edu.hk`
  dead (curl 52) — `https://` same-document URL works; cited URL should be
  upgraded to https.
- **Minor embellishments (kept verified, flagged)**:
  - `RC-002`: claim says "GPS location"; BIG site says only "their location".
  - `RC-010`: page confirms separate phone/messages app downloads but does not
    literally name them "Grand Phone"/"Grand SMS".
  - `researcher-material-15`: "1.13.0 is last minSdk-21 line" is an inference
    from release ordering (1.13.0 notes say minSdk 21, 1.14.0 says 23) — the
    inference is sound but is not a literal statement.
- **`researcher-material-14` precision**: claim's compileSdk 36.1 / AGP 8.13 /
  AS Narwhal 3 FD 2025.1.3 are real *consumer* requirements from
  getting-started.md, confirmed; the library itself was built with
  compileSdk 35 / AGP 8.11.1 / Gradle 8.13 (per release notes). Both true,
  different subjects — not an error.
- **Tier audit**: all L1/L3 assignments check out. No blog-citing-L1
  laundering found in the verified set.
- **Second-source spot checks (iron rule)**: Kobayashi 39%/13.6% confirmed by
  an independent ACM paper citing it; MDC 1.14.0 minSdk 23 confirmed by
  CodePath guide + repo build.gradle; Compose API-21 floor confirmed by
  official Compose basics codelab.

## Wave-0 emulator tour (task 10, 2026-11-22)

Full tour on `emulator-5554` (API 29, 480x800, ru locale), screenshots
`screenshots/v5-wave0-*.png`, detailed report at
`.superpowers/sdd/2026-11-22-granphone-wave0-foundation/task-10-report.md`.

- **Visual diff vs v1–v4 goldens: zero regressions.** Single-row home on fresh
  install matches `01-home.png`; all-rows state matches `v2-01`; add-pill hint
  clipping is identical to `v3-15` (pre-existing).
- **2 FATAL crashes — one root cause, P0.** Picking a language in the
  GranPhone IME (`BaldInputMethodService.changeLanguage` →
  `BaldKeyboard.<init>` → `BaldConstraintLayoutButton.setOnTouchListener:132`)
  NPEs because shipped defaults are `longPresses=false` +
  `longPressesShorter=true` (`BPrefs.java:50,56`, commit `19ba955`) — a state
  where `baldButtonTouchListener` is never initialized. English AND Hebrew
  verified. Every text-field + language-pick kills the process.
- signalView NPE fix holds through rotation; home + dialer landscape OK;
  force-stop + relaunch OK; incoming + outgoing calls OK via CallActivity once
  `android.app.role.DIALER` is granted.
