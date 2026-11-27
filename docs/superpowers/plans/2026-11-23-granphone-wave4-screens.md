# GranPhone Wave 4 — Screens

Spec: `docs/superpowers/specs/2026-11-22-granphone-ux-redesign-design.md` §5 + `report.md` §22 per-screen table. Base: end of wave 3 (`c895e39`, branch `redesign/wave-3-flow` → `redesign/wave-4-screens`).

Wave 4 is the **beauty wave**: user authorized full rewrites — "можно переписывать всё и делать максимально красиво". Rewrite layouts/screens completely when that beats patching. Constraints that still hold (they enable beauty, not limit it): Java 8, Views/XML, minSdk 21, JDK 11, no new deps (Material Icons are font/drawable resources — reuse existing or add vector XML drawables), BPrefs frozen, `?attr/bald_*` tokens only, sp for text, touch targets ≥56dp, every label a word not an icon, pressed/feedback states, dark theme always verified.

Aesthetic bar per §22: cards `bald_card` + 16dp radius + 1dp `bald_card_border`, semantic color only where it means something (brand=call/confirm, danger=SOS/delete, info=secondary actions), uniform card color elsewhere, generous whitespace, no mid-word breaks ever, no clipped text ever, no corner-X (DialogScreen pattern).

## Task 1 — Dialer

§22: keys 72dp card-style, digit 28sp; number field 32sp; T9 hint as «Имя — номер» chip under field; bottom row: Стереть (neutral, text+icon) / **Позвонить** (brand, 80dp, wider) / ⌫ (neutral + text). Kill the red trash (not semantic). Verify T9 chip against real contacts; `###` still blocked (wave-2 guard must survive).

## Task 2 — Contacts + contact detail + add/edit

- Contacts list: rows 80dp, avatar 56dp (photo or initials on hash-colored bg), name 22sp + number 16sp second line, bigger letter index, real search field «Поиск» on top, «+ Добавить контакт» bottom button.
- Detail: photo 96dp + name 28sp left-aligned; «Позвонить» brand 88dp full-width; action rows 72dp (SMS, Изменить neutral, Удалить danger separated); SOS contact = TextSwitch.
- Add/Edit: labels above fields (18sp), fields 64dp; Сохранить (brand) + Отмена (neutral text); no X; inline validation.

## Task 3 — SOS

§22: explanation line, big «SOS — Вызвать помощь» danger button with hold-to-confirm countdown (5s → SMS with location → call — reuse existing SOS machinery `SOSActivity`/`HomeScreenPinHelper`, don't rebuild the SOS logic, rebuild the UI+interaction), photo cards for SOS contacts + «+ Добавить». Siren icon already fixed (wave 3) — keep coherent.

## Task 4 — Alarms/Pills lists + forms

- Lists: uniform cards (time 40sp + repeat + TextSwitch); two bottom buttons «+ Будильник» / «+ Таймер» (pills: «+ Таблетка»).
- AddAlarm/AddPill: big +/- time picker (like AddTimer), repeat = vertical radio list («Один раз/Каждый день/По дням»), day chips 56dp capsules, label-above-field name, Сохранить brand bottom. Wave-2 targets were the patch; this is the proper redesign — rewrite the layouts if cleaner.
- AddTimer: keep pattern + preset chips 1/5/10 мин.

## Task 5 — Apps + empty states

- Apps: full-screen list 64dp rows (icon 40dp + name), search field top, no phantom rows (the 4th-row bug from research), «Добавить ярлык» properly placed.
- Empty states everywhere a list can be empty (contacts/alarms/pills/notifications/calls): centered icon + one sentence + relevant action — build ONE reusable empty-view pattern (or reuse `empty_view` scaffolding — ScrollingHelper already has one).

## Task 6 — Editor select screen

Tile tap in home editor → dedicated full-screen select (not the 6-affordance modal): vertical option list (По умолчанию/SOS/Своё приложение/Скрыть), current highlighted, instant apply, «Назад» — per §22. Reuse DialogScreen.SELECT as a full-screen activity or keep DialogScreen as the vehicle (it already does instant-apply lists — but it must ALSO replace other multi-option modals where the pattern fits; judge per case, don't force).

## Task 7 — Alarm ring screen + permissions

- Ring screen: «Отложить на 5 мин» neutral 88dp + «Выключить» danger 88dp; remove corner X.
- Permissions: each permission a card (what it is + why + big «Разрешить») + «шаг N из M» progress.

## Task 8 — Russian keyboard

Add `RussianKeyboard` (~70 lines cloning `EnglishKeyboard` pattern + RU layout ЙЦУКЕН…); audit `screenOrientation="landscape"` on the IME service (suspicious manifest line — check if it's intentional or a bug); Cyrillic must type into contacts/search fields. Verify EN/HE/RU switching.

## Task 9 — Visual tour + critic pass

Same discipline: full tour × light/dark × font 1.0/1.3, view every PNG, adversarial defect list → wave 5 polish input.

## Process

SDD: batches A(T1) B(T2) C(T3) D(T4) E(T5+T7) F(T6) G(T8) H(T9-critic), implementer→reviewer per batch, fix loops on Critical/Important, final whole-branch review. Reviewers MUST open PNGs. Beauty > minimal diff this wave — but still: tokens only, no dead code left behind, don't break call/SOS/kiosk machinery.
