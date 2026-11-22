# GranPhone Wave 1 — Design System

Spec: `docs/superpowers/specs/2026-11-22-granphone-ux-redesign-design.md` (§3 tokens/components). Base: end of wave 0 (`b2970b2`, branch `redesign/wave-0-foundation` → new branch `redesign/wave-1-system`).

Wave 1 puts the design *system* in place. Existing infrastructure is better than the spec assumed: `bald_*` theme attrs already cover ~22 roles, `BaldTitleBar` (30 layouts), `BaldSwitch` (= spec's TextSwitch), `BaldButton`/`BaldPictureTextButton` family, and 55 of 103 layouts already consume `?attr/bald_*`. So this wave is *additive*: new semantic tokens + one new component (DialogScreen) + small extensions. **No screen-by-screen recolor** — per-screen adoption happens in waves 2–4. Target: zero visual regression in light theme; dark may improve where raw colors were hardcoded.

Rules for every task: Java 8, XML views, minSdk 21, JDK 11 (`JAVA_HOME="C:/Users/adrew/jdk11/jdk-11.0.33+10"`), variant `installFDroidDebug`, no new dependencies, BPrefs keys frozen, smallest complete diff, existing components extended rather than duplicated.

## Task 1 — Semantic tokens

Files: `app/src/main/res/values/attrs.xml`, `app/src/main/res/values/themes_values.xml`, `app/src/main/res/values/styles.xml`.

Add the 8 spec §3 semantic attrs to attrs.xml (format `reference|color`, same as existing):

`bald_surface`, `bald_card`, `bald_card_border`, `bald_brand`, `bald_danger`, `bald_info`, `bald_text_primary`, `bald_text_secondary`.

In themes_values.xml add the spec hex values as `bald_<name>_light` / `bald_<name>_dark` colors (spec table: surface #F2F4F7/#101418, card #FFFFFF/#1C2126, card_border #E2E5E9/#2E3439, brand #2E7D32/#4CAF50, danger #D32F2F/#E57373, info #1565C0/#64B5F6, text_primary #14171A/#F2F4F7, text_secondary #4A5560/#B0B8C0).

Bind all 8 in BOTH `bald_light` and `bald_dark` styles in styles.xml (same pattern as existing `<item name="bald_button">…`).

Do NOT rebind existing tokens and do NOT migrate `bald_button`→`bald_card` anywhere — old tokens stay valid, semantics layer on top.

Verify: `assembleFDroidDebug` builds; `git grep -c "bald_brand" app/src/main/res/values/styles.xml` = 2.

## Task 2 — Kill raw colors in layouts

Files: whichever of the 6 raw-color sites in `app/src/main/res/layout*/` (`#FFFFFF` ×3, `#DD000000` ×2, `#00000000` ×1 — find with `grep -rn "#[0-9a-fA-F]\{6,\}" app/src/main/res/layout*/`).

Replace each with the matching semantic attr: `#FFFFFF` → `?attr/bald_card` (or `bald_button` — read context, pick the semantic fit), `#DD000000` → `?attr/bald_text_primary`, `#00000000` → `@android:color/transparent`.

Do NOT touch `@color/color_1` (home tile row tint — wave 3), do NOT touch pill colors (`yellow/green/red/gray/blue` — semantic pill indicators, not theme colors). Do NOT touch drawable XML files — layout scope only, unless a raw color sits in a drawable used as a component background and replacing it is a one-line fix; if unsure, skip and report.

Verify: grep returns zero raw hex in layout*/; build + install + screenshot home/dialer — visually identical to goldens.

## Task 3 — BigButton variant attr

Files: `app/src/main/java/com/bald/uriah/baldphone/views/BaldPictureTextButton.java`, `attrs.xml` (its `declare-styleable`), `styles.xml` if a drawable is needed.

Spec component BigButton = filled button, icon 28dp + label 20sp, variants `brand`/`danger`/`neutral`/`info`. `BaldPictureTextButton` already is the icon+text filled button used on home tiles. Extend it: add `app:variant` enum attr (`brand|danger|neutral|info`, default `neutral`) that maps background/text colors to `?attr/bald_brand`/`bald_danger`/`bald_button`/`bald_info` (+ white text on brand/danger/info).

Read the class first — if it draws background via theme attr already, the variant is a small tint swap in code; keep it under ~40 lines of change. No call-site changes (default `neutral` = current look).

Verify: build; attrs compile; existing tiles unchanged (variant defaults to current styling).

## Task 4 — TitleBar back affordance

Files: `app/src/main/java/com/bald/uriah/baldphone/views/BaldTitleBar.java`, its layout (find via `R.layout` in the class), `attrs.xml` `BaldTitleBar` styleable.

Spec: TitleBar gets an explicit `Назад` text button (never icon-only), title must never clip (ellipsize/marquee). Current BaldTitleBar is in 30 layouts — extend, don't replace:

- Add optional attr `show_back` (boolean, default false). When true, the bar renders a text `Назад` button that calls `activity.onBackPressed()`/finishes — use `Context` to find the host activity (existing code does this pattern elsewhere — grep for `((Activity)` in views/).
- Ensure the title TextView has `ellipsize=end` + `singleLine` or marquee so long titles degrade gracefully (this may already exist — check first; if it clips today on e.g. «Специальные возмо…», fix the layout attr, that's in-spec).

Default `show_back=false` = zero visual change. Per-screen enabling is wave 2+.

Verify: build; grep that `show_back` exists in attrs; no layout changes required in call sites.

## Task 5 — DialogScreen component

Files: new `app/src/main/java/com/bald/uriah/baldphone/views/DialogScreen.java` (or `fragments_and_dialogs/` — check where dialog helpers live; `DialogBuilder`/`DialogFlags` are in utils/) + one layout `dialog_screen.xml`.

Spec's single dialog component, three modes:

- `SELECT`: vertical option list (64dp rows, radio/checkmark indicator on current selection), instant apply on tap, `Назад` text button bottom. No OK/Cancel/X.
- `CONFIRM`: message + one verb action + one `Отмена`.
- `INFO`: message + one `Понятно`.

Implement as a `Dialog` subclass or a builder producing one — match how existing dialogs are constructed (grep `DialogBuilder`/`BaldDialogBox` usage for the pattern). Styling via `?attr/bald_*` tokens: card background, 16dp radius, title 24sp, body 20sp, BigButton-style actions. Keep the public API minimal: mode + title/message + options list + callbacks.

Runnable check required (rules): wire it into ONE existing simple dialog site as proof — pick the simplest current INFO-style dialog (grep for `new AlertDialog` / `BaldDialogBox` with single button; SettingsActivity has some) OR if no site is cleanly swappable, add a temporary self-check Activity path — prefer the swap, report which site was converted. This also gives wave 2/3 the reference usage.

Verify: build + install; trigger the converted dialog on emulator, screenshot, confirm spec layout (no X, bottom text button).

## Task 6 — Docs + verification tour

Files: `DESIGN.md` (component inventory update: which spec components already existed as Bald* and their mapping; DialogScreen API), `AGENTS.md` only if conventions changed.

Verification (emulator-5554, same tour discipline as wave 0 T10): install → home → dialer → settings → alarms → one dialog site → dark theme → landscape home → force-stop/relaunch → `logcat -d | grep FATAL` must be empty. Screenshot-compare vs wave-0 goldens in `research/2026-11-22-granphone-ux-redesign/` — expect identical light theme; flag any visual diff.

Record unverified paths in the report.

## Process

Same SDD loop as wave 0: one implementer per task (T1+T2 may batch, T3+T4 may batch), task briefs in `.superpowers/sdd/2026-11-22-granphone-wave1-system/`, review package + reviewer per task, fix loops on Critical/Important, final whole-branch review at the end.
