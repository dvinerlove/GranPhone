# DESIGN.md — GranPhone design contract

Token/component contract for every UI change. Source spec (full context, waves, verification): `docs/superpowers/specs/2026-11-22-granphone-ux-redesign-design.md` §3–4. This file is the working copy you apply; if they disagree, the spec wins.

Rule: extend the existing `bald_*` theme-attribute system (`R.attr.bald_*` in `res/values/themes_values.xml`, consumed via `?attr/…`). Do not invent a parallel palette.

## Color tokens

| Token | Light | Dark |
|---|---|---|
| `bald_surface` | #F2F4F7 | #101418 |
| `bald_card` | #FFFFFF | #1C2126 |
| `bald_card_border` | #E2E5E9 | #2E3439 |
| `bald_brand` | #2E7D32 | #4CAF50 |
| `bald_danger` | #D32F2F | #E57373 |
| `bald_info` | #1565C0 | #64B5F6 |
| `bald_text_primary` | #14171A | #F2F4F7 |
| `bald_text_secondary` | #4A5560 | #B0B8C0 |

Contrast: ≥4.5:1 for all text, ≥3:1 for icons/borders.

## Typography

All text sizes in `sp` (system `font_scale` must work). Scale: title 24sp, body 20sp, label 18sp, hero (clock) 64sp+.

## Radius & spacing

Radius: cards/buttons 16dp, small chips 28dp. Spacing grid: 8dp.

## Components (build once, reuse everywhere)

- **BigButton** — height 72–88dp, icon 28dp + text label 20sp, filled, pressed = 8% darken + optional vibration. Variants: `brand` (call/confirm), `danger` (SOS/hang up/delete), `neutral`, `info`.
- **Tile** — home grid cell: icon 32dp + label 18sp, `bald_card` background; semantic override only for SOS (danger) and Phone (brand).
- **TitleBar** — 64dp: title 24sp (marquee/ellipsize-safe, never clipped) + explicit `Назад` text-button; no lone icon affordances.
- **DialogScreen** — the single dialog component, three modes:
  - `SELECT`: vertical option list (64dp rows, radio indicator, current selection highlighted), applies instantly on tap, `Назад` button at bottom. No OK/Cancel/X.
  - `CONFIRM`: one verb-labeled action (e.g. «Удалить контакт») + one `Отмена`. Irreversible actions get a hold/double-tap-confirm.
  - `INFO`: message + one `Понятно` button.
  - Max 2 buttons + option list. Corner `X` removed everywhere.
- **StatusBar** (home only) — keep the custom large-icon status bar.
- **TextSwitch** — the «Включить/Выключить» text-pill pattern from alarms, promoted to shared component.

## Icons

Filled Material Icons (vector XML), 28–32dp, always paired with a text label.

## Banned patterns

- Hardcoded `#RRGGBB`/`#AARRGGBB` colors in layouts — everything through `?attr/bald_*`.
- `dp` in `android:textSize` — `sp` only.
- New dialogs/screens not built on `DialogScreen` modes (SELECT/CONFIRM/INFO) — no OK/Cancel, no corner `X`.
- Icons without a text label.
- Swipe/long-press as the *only* path to a feature — every screen needs a TitleBar `Назад` text button.
