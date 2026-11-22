# GranPhone Wave 2 — P0 Surgery

Spec: `docs/superpowers/specs/2026-11-22-granphone-ux-redesign-design.md` §5–6, report §17–22. Base: end of wave 1 (`ec68808`, branch `redesign/wave-1-system` → new branch `redesign/wave-2-p0`).

Wave 2 fixes the P0 visual/functional defects found in research — all confirmed live on emulator. Unlike waves 0–1, **visual change is intended** here. Every task must screenshot before/after on emulator-5554 and the reviewer must actually view the PNGs.

Rules: Java 8, XML, minSdk 21, JDK 11, `installFDroidDebug` (`adb -s emulator-5554 install` fallback — dead physical device attached), no new deps, BPrefs frozen, smallest complete diff, `?attr/bald_*` tokens for any touched color, sp for any touched text size.

## Task 1 — Text clipping + word-splitting pass

Known sites (research screenshots): title bars truncate («Изменить главный эк…», «Специальные возмо…»); press-type/accessibility-level screen splits words («Высоки й Урове нь», «Обычн ый уро вень»); font-size settings labels clip («Малень…»); keyboard-settings screen clips («Трудно использов…»).

- `BaldTitleBar` title: it has `ellipsize=end` — check why it still truncates early (weight/width constraints vs back/help views). Fix so titles get more room; if genuinely too long, marquee.
- Buttons that split mid-word: find the layouts (accessibility level picker in tutorial/settings — grep `accessib`/`уров`/`level`), the cause is usually fixed-width text inside weighted buttons. Fix: allow 2 lines or autosize/wider layout — full words must never break mid-word.
- Font-size + keyboard settings screens: same clipping class of bug.

Method: grep the layouts, reproduce each site on device, fix, screenshot after. Where a fix needs a judgment call between ellipsize and reflow, prefer reflow for buttons (they're short strings) and marquee/ellipsize for titles.

Verify: every listed site re-screenshotted unbroken; dark theme spot-check; no FATAL.

## Task 2 — dp→sp text pass

Find every `android:textSize="NNdp"` in `app/src/main/res/layout*/` and any `setTextSize(TypedValue.COMPLEX_UNIT_DIP` in Java. Convert dp→sp (same numeric value). This is what makes system font_scale work — P0 for low-vision users.

Verify: `grep -rn 'textSize="[0-9.]*dp"' app/src/main/res/` = 0; build; set `adb shell settings put system font_scale 1.3` → home/settings text visibly grows; reset to 1.0.

## Task 3 — AddAlarm + form touch targets

`activity_add_alarm.xml` (and same-class bugs in `activity_add_timer.xml`, pill time setter): day-of-week checkboxes ~20px → ≥56dp touch target (increase view or use `padding`/`minWidth`+`minHeight`, keep visual chip size if it looks better — target is *touch* area). Fix overlapping radios «Каждый день/Только один раз» — vertical list or proper spacing. Fix clipped field titles.

Verify: screenshot form before/after; tap-test the smallest targets.

## Task 4 — Home: visible Settings entry

Spec direction: settings must be reachable without gestures (full bottom-row redesign is wave 3). Minimal wave-2 version: add a visible «Настройки» tile to the home tile grid (uses existing tile machinery — check how tiles are defined, `HomePage1`/`BaldPrefsUtils` tile lists). Don't redesign the pager yet.

Verify: fresh-ish launch shows the tile; tap opens SettingsActivity; screenshot.

## Task 5 — Tutorial without swipe requirement

`TutorialFragment*.java`: add explicit «Далее»/«Пропустить» text buttons per spec (keep swipe as optional secondary). Check the last page's button — it currently resets default launcher mid-flow (research finding); ensure it returns to home cleanly. Fix typo «кочестве» in strings.

Verify: walk the tutorial on device via buttons only, no swipes; screenshots each page.

## Task 6 — Dialer number validation

`DialerActivity`/`dialer.xml` flow: before `ACTION_CALL`, validate the number — block empty/`#`/`*`-only input (research: `###` triggered a system MMI dialog in foreign UI). Minimum: non-empty, at least 2-3 digits, no `#` (special codes get blocked with a friendly message — but don't break SOS/112 which go through their own path). Show a BaldToast/DialogScreen INFO on invalid instead of calling.

Verify: dial `###` → in-app message, no system dialog; dial `5554` → CallActivity; dial `112` → CallActivity.

## Task 7 — Deferred one-liners batch

- `AppUtils.getTheme`: clamp `THEME_KEY` to `0..THEMES.length-1` (unreachable-in-normal-flow crash for corrupt prefs; prefs-contract documents the hazard).
- `kiosk_not_device_owner_subtext` string: still names `com.bald.uriah.baldphone` in the `dpm` command — update to `com.granphone.launcher` (values/ + values-ru/; check other locales that carry the string).
- `understood` string: add to high-coverage locales where the file exists (de/es/it/iw/zh at least) — machine-translate is fine, it's one word («Понятно»/«Verstanden»/«Entendido»/«Capito»/«הבנתי»/«明白了»).

Verify: grep confirm; build.

## Task 8 — Visual verification + critic pass

Full screenshot tour (home/dialer/call/settings/alarms/pills/contacts/apps/tutorial/dialog × light+dark × font_scale 1.0/1.3). View EVERY PNG. Then a critic pass: hunt for remaining visual defects — clipped text, low contrast, misalignment, off-brand colors — deliver a defect list with severity for wave 3/4 input. Do not fix in this task.

## Process

SDD: batches A(T1) B(T2+T7) C(T3) D(T4+T5) E(T6) F(T8-critic), implementer→reviewer per batch, fix loops on Critical/Important, final whole-branch review. Reviewers MUST open the PNG artifacts.
