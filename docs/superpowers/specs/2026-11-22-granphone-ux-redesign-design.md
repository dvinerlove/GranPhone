# GranPhone — UX/UI Redesign Design Spec

**Date:** 2026-11-22
**Status:** awaiting user review
**Evidence base:** `research/2026-11-22-granphone-ux-redesign/report.md` (parts I–IV, ~55 live screenshots, 123 verified facts)

## 1. Goal

Replace the inconsistent, dated UI and gesture-dependent user flow with a modern, coherent, senior-accessible design — without breaking operation on old devices (minSdk 21, Huawei 2017/EMUI 8 is the reference target) and without a risky full rewrite.

Audience: elderly users, low-vision users, users who have never used a smartphone, plus a "helper" (relative/caregiver) who configures the device.

## 2. Locked decisions

| # | Decision | Choice |
|---|---|---|
| 1 | Distribution | F-Droid / GitHub APK now; Google Play deferred to the end (targetSdk + declarations then) |
| 2 | Target devices | All major OEMs: Huawei/EMUI, Xiaomi/MIUI, Samsung, stock Android |
| 3 | Custom keyboard (IME) | **Keep** (large keys + speech-to-text are core audience value) + **add Russian/Cyrillic layout** |
| 4 | Home structure | Keep tile grid concept; single scrollable page, semantic colors, fixed bottom row |
| 5 | Settings model | Visible `Настройки` tile; user settings open, helper settings behind PIN |
| 6 | Stack | Java 8 + Views + XML; no Compose; MDC 1.13.0; ViewBinding; AGP 4.2.x |
| 7 | Refactor strategy | Incremental (strangler), no big-bang rewrite |

## 3. Design system

Extend the existing `bald_*` theme-attribute system rather than inventing a new one.

### Tokens

| Token | Light | Dark |
|---|---|---|
| `bald_surface` | #F2F4F7 | #101418 |
| `bald_card` | #FFFFFF | #1C2126 |
| `bald_card_border` | #667085 | #9AA4B2 |
| `bald_brand` | #19632D | #30D158 |
| `bald_danger` | #D32F2F | #E57373 |
| `bald_info` | #1565C0 | #64B5F6 |
| `bald_text_primary` | #14171A | #F2F4F7 |
| `bald_text_secondary` | #4A5560 | #B0B8C0 |

Contrast target: ≥4.5:1 for all text. Text on the light brand surface has a WCAG AAA target of ≥7:1; `#19632D` meets it at 7.33:1. Non-text UI — icons, control borders, and focus indicators — must be ≥3:1. Radius: cards/buttons 16dp, small chips 28dp. Spacing grid: 8dp.

### Typography

All sizes in `sp` (kill every `dp` text size — system font_scale must work). Scale: display (`giant`) 40sp, title 24sp, body 20sp, label 18sp, hero (clock) 64sp+.

### Components (built once, reused everywhere)

- **BigButton** — height 72–88dp, icon 28dp + text label 20sp, filled, pressed = 8% darken + optional vibration. Variants: `brand` (call/confirm), `danger` (SOS/hang up/delete), `neutral`, `info`. Critical-call exception: Answer and Hang-up are 120dp, Decline 96dp, and Speaker 88dp; this fixed 120/96/88dp call-action set is deliberate for time-critical calls, not BigButton scale drift.
- **Tile** — home grid cell: icon 32dp + label 18sp. This is the restored home-tile contract: no adaptive 14sp exception; the fixed bottom row uses component-default label sizing rather than a down-scaled override. `bald_card` background; semantic override only for SOS (danger) and Phone (brand).
- **TitleBar** — `title_bar_height` 64dp: title 24sp (marquee/ellipsize-safe, never clipped) + explicit `Назад` text-button; no lone icon affordances. Forced-LTR/handedness placement is intentionally unchanged pending RTL device evidence.
- **DialogScreen** — single dialog component, three modes:
  - `SELECT`: vertical option list (64dp rows, radio indicator, current selection highlighted), applies instantly on tap, `Назад` button at bottom. No OK/Cancel/X.
  - `CONFIRM`: one verb-labeled action (e.g. «Удалить контакт») + one `Отмена`. Irreversible actions get a hold/double-tap-confirm.
  - `INFO`: message + one `Понятно` button.
  - Max 2 buttons + option list. Corner `X` removed everywhere.
- **StatusBar** (home only) — keep the custom large-icon status bar; it's genuinely better than the system one for this audience.
- **TextSwitch** — keep the «Включить/Выключить» text-pill pattern already in alarms; promote to shared component.

### Icons

Filled Material Icons (vector XML), 28–32dp, always paired with a text label. Replace the siren clip-art (invisible on red) and mixed icon styles.

## 4. Information architecture & flow

### Home

- Single page, vertically scrollable if needed; **no hidden pages, no swipe navigation**.
- Layout: compact status bar → clock/date card → tile grid (3 columns, uniform card color) → **fixed bottom row: SOS | Программы | Настройки**.
- Tile visibility still configurable via editor (helper decision); all tiles reachable — nothing exists only behind a gesture.
- Clock card fixed so digits are never clipped (current bug).

### Navigation model

- Every screen gets TitleBar with a real `Назад` text button. Android back works too, but is never the only way.
- No swipe/long-press as the *only* path to anything. Editor access: helper mode only.

### Settings (two layers)

- **User settings** — 5–8 safe rows (volume, brightness, font size, press type, vibration, flashlight): immediate-apply toggles/sliders.
- **Helper settings** — PIN-gated section at the bottom: tiles editor, app whitelist, SOS contacts, permissions, kiosk, keyboard, danger zone (reset).
- Flat list, ≤2 levels deep; every row shows current value; no OK/Cancel dialogs.

### Onboarding (helper-driven)

Replace swipe tutorial with 3–4 static screens (image + one sentence + `Дальше` button): welcome → helper setup (PIN, contacts, tiles) → OEM battery instructions (per-brand photo steps) → done. The press-type picker moves to settings; nothing in onboarding resets the default launcher mid-flow.

## 5. Per-screen changes

Full specs per screen are in `report.md` §22. Headline changes:

- **CallActivity** — add caller photo (96dp) + name (36sp) + number (24sp); content pushed below the system notification area; keep the excellent ПРИНЯТЬ/ОТКЛОНИТЬ/ЗАВЕРШИТЬ button pattern.
- **Dialer** — bigger number field, T9 hint as «Имя — номер» chip; bottom row: Стереть / **Позвонить** (green, wider) / ⌫; validate number before `ACTION_CALL` (kills the system MMI dialog on `###`).
- **Contacts** — photo avatars (initials fallback), larger letter index, text search field, `+ Добавить контакт` button.
- **SOS** — countdown-press pattern (hold 5s → SMS with location → call), photo cards for SOS contacts, replace siren icon.
- **AddAlarm** — large +/- time picker (like AddTimer), vertical radio list for repeat, 56dp day-of-week chips.
- **Alarms/Pills lists** — uniform cards with text-switch; two explicit `+` buttons at bottom.
- **Editor** — tile tap → dedicated select screen (not the 6-affordance modal).
- **Keyboard (IME)** — add `RussianKeyboard` (~70 lines, clone `EnglishKeyboard` pattern + layout); audit `screenOrientation="landscape"` on the service.
- **Alarm ring screen** — `Отложить` (neutral) + `Выключить` (danger), both 88dp; remove corner X.

## 6. Engineering plan (waves)

Bug fixes land inside waves, not before them — P0s go first. Each wave gets its own implementation plan written when the previous one lands; this spec is the contract they all follow.

**Wave 0 — foundation, zero visual change:**
- Fix `signalView` NPE (null-guard + unregister listener; `HomeScreenActivity.java:150`) — crashes wipe the default-launcher preference.
- Give `FakeLauncherActivity` a distinct label.
- Delete dead code: FeedbackActivity, PhoneActivity dead path, `bald_skin`, hidden page-2 rows, TutorialFragment3 leftovers.
- AGP 3.5.3→4.2.x, Gradle→6.7.x, JDK 11; enable ViewBinding; enable Room `exportSchema`.
- `AccessibilityChecks.enable()` in the Espresso runner.
- Write `DESIGN.md` + `AGENTS.md` (conventions, token rules, how-to-add-a-screen recipe).
- Rename cryptic utils: `S→AppUtils`, `D→Const`, `BDB→DialogBuilder`, `BDialog→DialogFlags`.
- Inventory BPrefs keys → contract table (names/defaults never change).

**Wave 1 — system:** semantic color roles in `bald_*` themes (light + dark), BigButton/Tile/TitleBar/DialogScreen/TextSwitch components, recolor all screens through tokens.

**Wave 2 — P0 surgery:** text clipping pass, sp sizes, AddAlarm targets, broken press-type screen, home path to settings/SOS/apps, tutorial without swipes, dialer number validation.

**Wave 3 — flow:** new home (single page + fixed bottom row), `Назад` everywhere, PIN-gated helper settings, new onboarding, CallActivity caller block.

**Wave 4 — screens:** contacts/photos → SOS → alarms/pills → notifications/apps → empty states; Russian keyboard layout; per-screen screenshot regression via `BaseScreenshotTakerTest`.

**Wave 5 — polish:** icon consistency, animations ≤300ms, RU copy review, localization pass (ru/en complete; de/iw stress test), real Huawei verification, OEM battery onboarding step.

**Deferred to "Play phase":** targetSdk upgrade, Permissions Declaration, privacy policy, dark-theme parity audit.

## 7. Verification

- Golden-master: current `screenshots/` set is the "before" baseline; `BaseScreenshotTakerTest` extended per migrated screen.
- `AccessibilityChecks` in Espresso from wave 0 onward.
- Emulator matrix: API 29 (current) + API 26 (EMUI-8 era), light+dark, RU+EN (+de/iw stress in wave 5), `font_scale` 0.85/1.0/1.3, forced-RTL.
- Real Huawei 2017 before release (battery-killer behavior can't be emulated).
- Update-path test: old APK → seeded data → upgrade → verify (wave 3+).

## 8. Out of scope

Compose/Kotlin migration · new dependencies for aesthetics · widgets/AppWidgetHost · voice commands as primary input · full RTL mirroring (text-level RTL only) · Play Store compliance work (deferred).
