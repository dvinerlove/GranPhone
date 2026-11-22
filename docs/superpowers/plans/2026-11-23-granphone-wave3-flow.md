# GranPhone Wave 3 — Flow

Spec: `docs/superpowers/specs/2026-11-22-granphone-ux-redesign-design.md` §4–6. Base: end of wave 2 (`3eddc6a`, branch `redesign/wave-2-p0` → new branch `redesign/wave-3-flow`).

Wave 3 changes flows and structure — the biggest visual wave so far. Verify all API claims in code before trusting this plan; verified facts from external docs:

- Caller name/number: `Call.getDetails().getHandle().getSchemeSpecificPart()` (number) and `getContactDisplayName()` (contact name, async via `Call.Callback.onDetailsChanged`, needs READ_CONTACTS) — API 23+, guard for minSdk 21–22.
- Heads-up overlap: `BaldInCallService.java:104-119` posts OUR notification with `IMPORTANCE_HIGH` + `PRIORITY_MAX` — that's the P1 covering caller ID. Fix = lower importance; CallActivity is already full-screen.
- MDC reality check: 1.12 needs minSdk 19, 1.14 needs 23 — spec said "1.13.0 last for 21", corrected: any ≤1.13 works, still not needed this wave.
- Per-app locale: `LocaleManager` is API 33+; for 21–28 the pattern is `attachBaseContext` + `createConfigurationContext` — defer any in-app picker, low priority.

Rules: Java 8, XML, minSdk 21, JDK 11, `adb -s emulator-5554`, no new deps, BPrefs keys frozen (new keys only additive), `?attr/bald_*` + sp, smallest complete diff, screenshots before/after committed to `research/.../screenshots/w3-*.png`, reviewers MUST view PNGs.

## Task 1 — New home: single page + fixed bottom row

Spec §4: single vertically-scrollable page, no ViewPager/swipes, fixed bottom row `SOS | Программы | Настройки`.

- `HomeScreenActivity` layout currently hosts `ViewPagerHolder` + `HomePage1` fragment. Replace the pager with a direct scroll container hosting the home content (keep `HomePage1` fragment hosted directly if simplest — read its structure first; it builds tile rows from `BaldPrefsUtils` lists).
- Bottom row: 3 fixed buttons using `BaldPictureTextButton` (+ `variant` where appropriate — SOS gets `danger`, others neutral) wired to `SOSActivity`, `AppsActivity`, `SettingsActivity`. The separate «Настройки» tile added in wave 2 stays or merges into the bottom row — remove the duplicate tile if bottom row covers it (check `CUSTOM_SETTINGS_KEY` handling — keep pref consistent, don't strand the key).
- Fix the home clock clip (critic P1): clock digits clipped at container top, worse at font_scale 1.3 — the clock card needs wrap_content/padding accommodating scaled text.
- Kill the page dots (`use_circles`/indicator) — no pager, no indicator.
- Keep: custom status bar (spec says it's genuinely better), notification red dot mechanism (`HomePage1#notificationReceiver`), all tile-hide/editor machinery.

This is the biggest task — split sensibly, keep `HomePage1` internals working.

Verify: home scrolls as one page, bottom row always visible, all 3 buttons work, clock unclipped at font 1.3, dark theme OK, screenshots before/after.

## Task 2 — «Назад» text button everywhere

Enable `app:show_back="true"` on `BaldTitleBar` across the app's screens (30 layouts use it). Do it mechanically: grep layouts with `BaldTitleBar`, add `app:show_back="true"` where the screen is a child (not root/home). Skip screens where a text «Назад» would fight the layout (tutorial pages, dialogs) — use judgment, list skipped screens + reason in report.

Watch the wave-1 reviewer flag: external `getBt_back().setOnClickListener` callsites (SettingsActivity:127, PermissionActivity:86, AddContactActivity:129, LetterChooserDialog:63) attach to the ICON view which is uninflated when show_back=true — either keep show_back=false on those 4 screens OR move their listener to the text back. Check each.

Verify: spot-check 5+ screens — «Назад» visible, returns correctly, title not overlapped. Screenshots.

## Task 3 — Settings: two layers + PIN gate

Spec §4: user settings (safe rows) + helper settings behind PIN. There's already a PIN concept in `KioskManager` — check for existing PIN storage; reuse, don't create a second PIN.

- Restructure `SettingsActivity` list into two sections: верх = user settings (current value in row, immediate apply — mostly already true); низ = «Настройки помощника» row → PIN check → the rest (tiles editor, app whitelist, SOS contacts, permissions, kiosk, keyboard, danger).
- PIN: if kiosk PIN exists reuse it; else simple 4-digit PIN set-on-first-entry stored in BPrefs (NEW key — additive allowed). PIN dialog via DialogScreen or a small dedicated layout.
- Remove the now-lying «adaptive» theme option properly: the picker's options drive `setOptionsStartingIndex` + `putInt` — safest minimal fix that doesn't remap stored values: keep index 2=dark, replace index 1 «adaptive» label with «Светлая» (light) or just leave both light — actually correct approach: rename option 1 to light and keep indices. Read SettingsActivity:280-295 first and pick the minimal honest fix.
- Also fix corrupt-theme `setOptionsStartingIndex` path (same clamp as AppUtils) — SettingsActivity:292.

Verify: PIN flow works (wrong PIN rejected, correct opens helper section), theme dialog honest, screenshots.

## Task 4 — CallActivity: caller block + heads-up fix

- `CallActivity` must show: caller photo (96dp, initials fallback — check existing contact-photo helpers, `SquaredCircleImageView` exists), name (36sp) via `getContactDisplayName()`, number (24sp) via `getHandle().getSchemeSpecificPart()`. Handle name arriving async: register `Call.Callback.onDetailsChanged` and update fields. API 23+ guard; on 21–22 degrade to number-only/empty gracefully.
- Fix heads-up overlap (P1): `BaldInCallService.java:104-119` — our notification uses IMPORTANCE_HIGH+PRIORITY_MAX. Lower to `IMPORTANCE_DEFAULT`/`PRIORITY_DEFAULT` for the ongoing-call notification; if a separate incoming-call notification exists keep enough importance for ringtone/heads-up ONLY when CallActivity isn't shown — read the code first; the simplest correct fix may be: ongoing notification importance LOW (it's just "return to call"), incoming keeps HIGH. Judge in code.
- Push content below notification area (spec: caller block must not hide under system heads-up).

Verify: `adb emu gsm call 5554` → screenshot: caller number visible, no overlap; answer → active state shows name/number; dark theme.

## Task 5 — Onboarding replacement (static screens)

Spec §4: replace swipe tutorial with 3–4 static screens: welcome → helper setup (PIN/contacts/tiles hint) → OEM battery instructions → done. Each = image + one sentence + «Дальше» button. Keep `TutorialActivity` shell, replace pager content — OR reuse existing fragments with button nav (wave 2 already added buttons; this task makes content spec-compliant: fix copy, add battery-OEM step, ensure nothing resets default launcher mid-flow — wave 2 fixed exit already).

OEM battery step: per-brand instruction cards (Huawei: Настройки→Батарея→Запуск приложений→GranPhone→Управление вручную; Xiaomi: Автозапуск+нет ограничений; Samsung: Фоновые ограничения). Detect brand via `Build.MANUFACTURER` lowercase contains huawei/xiaomi/samsung → show matching card + generic fallback.

Verify: full onboarding walk on device, screenshots each screen.

## Task 6 — Deferred P1 visual fixes (cheap ones)

From critic list, fix what's small here (bigger items land in wave 4 per-screen):
- `RecentActivity` light-theme contrast (pastel-blue on light-gray — apply semantic tokens).
- SOS red button dark text in dark theme (use `bald_text_on_variant` for the SOS button text — it's exactly why the token exists).
- Dialer action icons washed-out in dark (tint to `?attr/bald_text_on_variant` or text_primary — pick per actual look).
- AddPill «Подтвердить» bottom clip — structural fix at `add_pill_activity.xml:33` (inner weighted LinearLayout match_parent under wrap_content titlebar → `0dp`+weight or `wrap_content`; same check in `add_alarm_activity.xml:33` — critic flagged both).
- Tutorial dots clipped at bottom (if tutorial pager still shows dots after T5 — remove/adjust).

Verify each with before/after PNGs, light+dark.

## Task 7 — Visual verification tour + critic pass

Same discipline as wave 2 T8: full tour × light/dark × font 1.0/1.3, VIEW every PNG, adversarial defect list → feeds wave 4.

## Process

SDD: batches A(T1) B(T2+T6-cheap) C(T3) D(T4) E(T5) F(T7-critic), implementer→reviewer per batch, fix loops on Critical/Important, final whole-branch review. Reviewers MUST open PNGs.
