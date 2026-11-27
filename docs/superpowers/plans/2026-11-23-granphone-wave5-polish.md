# Wave 5 — Polish: единый язык, оставшиеся экраны, локализация

Spec `2026-11-22-granphone-ux-redesign-design.md` §6 wave 5: icon consistency, animations ≤300ms, RU copy review, localization pass (ru/en complete; de/iw stress), real Huawei, OEM battery onboarding (done in wave 3). Plus wave-4 reviewer's missed-screens list — the remaining amateur seams.

Aesthetic bar unchanged: `bald_*` tokens, card pattern, worded labels, no corner-X, no mid-word breaks, ≥56dp targets. Beauty > minimal diff within the token system.

## Task 1 — DialogBuilder → DialogScreen migration (the big consistency win)

37 `DialogBuilder.from` call sites in ~15 files remain — old square OK/Cancel dialogs next to the new DialogScreen language. Convert EVERY site where the pattern fits:
- options+OK dialogs → `DialogScreen.select` (instant apply, current highlighted, «Назад»)
- yes/no → `DialogScreen.confirm` (verb-labeled action + Отмена)
- info → `DialogScreen.info`
- Weird/hybrid sites (custom views inside dialog, EditText input, video embeds) — audit; keep DialogBuilder only where DialogScreen genuinely can't carry the payload, and document why.
- If `DialogBuilder`/`DialogFlags` end up fully unused → delete them (big dead-code win). If a few legit sites remain → leave + comment.

## Task 2 — Settings visual polish

SettingsActivity list rows still look like the old app (hairline dividers, mixed icon treatment). Bring to wave-4 language: consistent row heights ≥56dp, `bald_card` or clean flat rows matching contacts/apps lists, section headers if useful. Keep all preference wiring untouched — presentation only. Also fix «сделать 5551234?» callback-dialog grammar if it's in the converted dialog paths.

## Task 3 — Tutorial screens

5 fragments on blank background, no title bar. Add title bar + «Назад» where reachable post-onboarding (KeyboardChanger reuses a tutorial fragment — preserve that entry), consistent typography/padding. Static onboarding from wave 3 stays.

## Task 4 — Help + media screens

`activity_help`, `help_item`, `fragment_video/image`, `youtube_container`, `activity_media_scrolling`, `single_media_activity`, `media_item` — same pass: cards/rows per wave-4 language, readable sizes.

## Task 5 — Copy + localization + motion

- `no_alarms`/`no_pills` in ~33 locales still reference old button names («Add Alarm»/«Add Timer»/«Pill») — update to match new labels (translate mechanically per locale, RU/EN authoritative).
- Icon consistency audit: same action = same icon across screens (call/sms/delete/back/add).
- Animations audit: any transition >300ms → clamp (spec rule).
- `russian` string in remaining locales (32 files have english/hebrew) — mechanical add where trivially translatable ("Русский"/"Russian" per locale is fine to leave EN fallback for obscure ones — judge).

## Task 6 — Stress tour + final critic

- Locales de + iw (RTL stress), font_scale 1.3, light+dark — screenshot tour of ALL rewritten screens (home, dialer, contacts, SOS, alarms, pills, apps, notifications, recent, settings, permission, editor).
- `connectedFDroidDebugAndroidTest` must at least compile (wave-4 fix); run whatever tests pass in the environment.
- Final whole-branch review = READY gate; debt ledger for the deferred "Play phase".

## SDD

Batches A(T1) B(T2) C(T3+T4) D(T5) E(T6), implementer→reviewer per batch, fix loops on Important+, reviewers MUST open PNGs. Real Huawei verification stays outstanding — documented, not blockable.
