# DESIGN.md — GranPhone design contract

Token/component contract for every UI change. Source spec (full context, waves, verification): `docs/superpowers/specs/2026-11-22-granphone-ux-redesign-design.md` §3–4. This file is the working copy you apply; if they disagree, the spec wins.

Rule: extend the existing `bald_*` theme-attribute system (attrs declared in `res/values/attrs.xml`, bound per theme in `res/values/styles.xml`, values in `res/values/themes_values.xml`; consumed via `?attr/…`). Do not invent a parallel palette.

## Color tokens

| Token | Light | Dark |
|---|---|---|
| `bald_surface` | #F2F4F7 | #101418 |
| `bald_card` | #FFFFFF | #1C2126 |
| `bald_card_border` | #33000000 (20% black) | #4Dcdd4db (30% mist) |
| `bald_brand` | #19632D | #30D158 |
| `bald_danger` | #D32F2F | #E57373 |
| `bald_info` | #1565C0 | #64B5F6 |
| `bald_text_primary` | #14171A | #F2F4F7 |
| `bald_text_secondary` | #4A5560 | #B0B8C0 |
| `bald_text_on_variant` | #FFFFFF | #14171A |

Contrast: ≥4.5:1 for all text. Text on the light brand surface has a WCAG AAA target of ≥7:1; `#19632D` meets it at 7.33:1. Non-text UI — icons, control borders, and focus indicators — must be ≥3:1. Default-theme borders are intentionally softer than this floor for the calm look; the measured high-contrast pair `#667085`/`#9AA4B2` (4.5–7.3:1) is reserved for a future optional contrast theme.

## Typography

All text sizes in `sp` (system `font_scale` must work). Scale: display (`giant`) 40sp, title 24sp, body 20sp, label 18sp, hero (clock) 64sp+.

## Radius & spacing

Radius (dimens): `radius_card` 16dp, `radius_button` 14dp, `radius_dialog` 20dp. Small chips stay 28dp.
Spacing (dimens): `spacing_xs` 4dp, `spacing_sm` 8dp, `spacing_md` 12dp, `spacing_lg` 16dp, `spacing_xl` 24dp.
Sizes (dimens): `touch_min` 64dp min target, `icon_sm` 24 / `icon_md` 32 / `icon_lg` 40 / `icon_xl` 48, `border_width` 1.5 / `border_width_selected` 2.5.
Component sizes (dimens): `avatar_sm` 44 / `avatar_md` 56 / `avatar_lg` 72 / `avatar_xl` 144, `list_row_height` 80 / `list_row_height_tall` 88, `list_header_height` 48, `title_bar_height` 64, `button_height_small` 48 / `button_height_default` 64 / `button_height_large` 80, `switch_height` 48, `dialog_button_height` 72 / `dialog_action_inset` 44 / `dialog_actions_reserve` 124 / `dialog_close_inset` 34, `progress_height` 16, `elevation_low` 5, `key_padding` 1 / `key_text_size` 30sp / `keyboard_height` 350, `dialer_display` 32sp.

## Components (build once, reuse everywhere)

Spec component → real implementation:

- **Scaffold** = `views/BaldScreen` — vertical root that prepends the TitleBar (`app:title`/`app:show_back` forward straight to it, brand background applied automatically) and stacks screen content below; content can never slide under the bar. Put `ScrollingHelper` inside for scrollable screens.
- **BigButton** = `views/BaldPictureTextButton` — height 72–88dp, icon 28dp + text label 20sp, filled, pressed = 8% darken + optional vibration. Variants via `app:variant` enum (`neutral|brand|danger|info|tinted`, default `neutral`): non-neutral variants tint the stateful background with `bald_brand`/`bald_danger`/`bald_info` and the icon+label with `bald_text_on_variant`; `tinted` uses `bald_brand_tint` background + `bald_text_primary` foreground (accent blocks like the dialer row). `brand` = call/confirm, `danger` = SOS/hang up/delete. Critical-call exception: Answer and Hang-up are 120dp, Decline 96dp, and Speaker 88dp; this fixed 120/96/88dp call-action set is deliberate for time-critical calls, not BigButton scale drift.
- **Card** = `BaldFrameLayoutButton` + `@drawable/bald_card_background` (or `bald_card_tinted_background` for accent blocks) — `radius_card`, `bald_card_border` stroke, `state_selected` = `bald_brand` 2.5dp border; set `android:selected` or `view.setSelected(true)`.
- **SettingRow** = `layout/settings_item` + `SettingsActivity.SettingsItem` — icon 40 + autosize title + optional `getSubtitle()` (current value, `bald_text_secondary`) + optional `bindTrailing(container)` slot (e.g. `BaldSwitch`); all vertically centered.
- **Tile** = `views/FirstPageAppIcon` (the home grid cell widget, `BaldFrameLayoutButton`); `views/HomeScreenAppView` is the helper binding a tile to its pinned app/contact. Icon 32dp + label 18sp. This is the restored home-tile contract: no adaptive 14sp exception; the fixed bottom row uses component-default label sizing rather than a down-scaled override. `bald_card` background; semantic override only for SOS (danger) and Phone (brand).
- **TitleBar** = `views/BaldTitleBar` — `title_bar_height` 64dp: title 24sp (`ellipsize=end`, `maxLines=1`, never clipped) + optional `app:show_back` (default off) rendering an explicit `Назад` text-button (`bald_title_back_text`, also ellipsized) that backs out of the host activity; the legacy icon back button stays when `show_back=false`. Forced-LTR/handedness placement is intentionally unchanged pending RTL device evidence.
- **DialogScreen** = `utils/DialogScreen` — `Dialog` subclass, one factory per mode:
  - `DialogScreen.select(ctx, title, selectedIndex, listener, options…)` — `SELECT`: vertical option list (64dp rows, check indicator + brand text on current selection), applies instantly on tap, `Назад` button at bottom. No OK/Cancel/X.
  - `DialogScreen.confirm(ctx, title, message, action, onAction)` — `CONFIRM`: one verb-labeled action (e.g. «Удалить контакт») + one `Отмена`. Irreversible actions get a hold/double-tap-confirm.
  - `DialogScreen.info(ctx, title, message)` — `INFO`: message + one `Понятно` button.
  - Max 2 buttons + option list. Corner `X` removed everywhere. Reference conversion: `SettingsActivity.showKioskDialog()` kiosk INFO branch.
- **GhostButton** = `@drawable/bald_ghost_button_background` — invisible until pressed/focused (icon-only controls, section headers, inline actions); replaces legacy `style_for_buttons_transparent`. Filled variant: `bald_button_background`.
- **StatusBar** (home only) = the custom home top bar — `home_screen_top_bar.xml` inside `home_screen.xml`, wired in `HomeScreenActivity` (signal/sound/battery/notifications large icons).
- **TextSwitch** = `views/BaldSwitch` — the «Включить/Выключить» text-pill pattern from alarms, promoted to shared component.

## Icons

Filled Material Icons (vector XML), 28–32dp, always paired with a text label.

## Banned patterns

- Hardcoded `#RRGGBB`/`#AARRGGBB` colors in layouts — everything through `?attr/bald_*`.
- `dp` in `android:textSize` — `sp` only.
- New dialogs/screens not built on `DialogScreen` modes (SELECT/CONFIRM/INFO) — no OK/Cancel, no corner `X`.
- Icons without a text label.
- Swipe/long-press as the *only* path to a feature — every screen needs a TitleBar `Назад` text button.
