# GranPhone (BaldPhone fork) — UI/UX Code Audit
Date: 2026-11-22. Scope: `app/src/main`. All paths relative to `app/src/main/` unless noted.

## 1. Screen map

App entry: `AndroidManifest.xml:62-73` — `HomeScreenActivity` is MAIN/LAUNCHER/HOME (singleTask). App theme `@style/bald_light` (manifest:58); every `BaldActivity` re-applies `S.getTheme(this)` in `BaldActivity.java:225-226`.

| Activity | Layout | Reached via | Notes |
|---|---|---|---|
| `activities/HomeScreenActivity.java:93` | `layout/home_screen.xml` (+`layout-land/`) | Launcher/HOME | ViewPager of Views (not fragments) via `adapters/BaldPagerAdapter.java:71-95`: pos -1 `NotesView` (notes_fragment.xml, only if `NOTE_VISIBLE_KEY`), pos 0 `HomePage2` (fragment_home_page2.xml), pos 1 `HomePage1` (fragment_home_page1.xml = start page), pos ≥2 `HomeViewFactory` pinned-app pages (home_factory.xml + home_screen_app_view.xml). Top bar `home_screen_top_bar.xml`: signal / BatteryView / ringer-mode dropdown / notifications bell. Immersive sticky nav-bar hiding (HomeScreenActivity:367-373). |
| `activities/DialerActivity.java:59` | `layout/dialer.xml` (+land) + `layout/dial_pad.xml` | Home tile `bt_dialer` (HomePage1:205); `tel:`/`ACTION_DIAL` intent filters (manifest:74-94); `PhoneActivity` | Contact-filter RecyclerView above pad; DTMF tones; static `call()` overloads (DialerActivity:80-140) used app-wide. |
| `activities/CallActivity.java:61` | `layout/activity_call.xml` | `services/BaldInCallService` (manifest:286-300); HomeScreenActivity:326-327 when call ongoing | **Extends `AppCompatActivity`, not BaldActivity**; own taskAffinity `.call`, portrait, excludeFromRecents. Applies `setTheme(S.getTheme)` itself (line 94). SIM-picker BDB dialog (221-267). |
| `activities/contacts/ContactsActivity.java:34` | `layout/contacts_activity.xml` | Home tile `bt_contacts` (HomePage1:206); SOSActivity:77 (MODE_SOS); PhoneActivity | Extends `BaseContactsActivity`. |
| `contacts/SingleContactActivity.java:71` | `layout/single_contact_activity.xml` | contact rows (ContactRecyclerViewAdapter:273), calls rows (CallsRecyclerViewAdapter:273), pinned home contacts (HomeScreenAppView:54) | → `AddContactActivity` (edit). |
| `contacts/AddContactActivity.java:69` | `layout/add__edit_conatct_activity.xml` (+land) | ContactsActivity:48, SingleContactActivity:373, CallsRecyclerViewAdapter:289 | Photo via `PhotosActivity` GET_CONTENT (AddContactActivity:121). |
| `contacts/ShareActivity.java:49` | `layout/activity_share.xml` | `S.java:87` (share flows) | Contains a `BaldSwitch`. |
| `activities/RecentActivity.java:29` | `layout/activity_recent.xml` | Home tile `bt_recent` (HomePage1:204) | `enabled="${recentCalls}"` manifest flag. |
| `activities/SOSActivity.java:48` | `layout/activity_sos.xml` | Home tile `bt_whatsapp` → SOS (HomePage1:207 — repurposed slot) | 2 pinned contacts + red `ec_real` button calling "112" (line 91). |
| `activities/alarms/AlarmsActivity.java:54` | `layout/alarm_activity.xml` | Home tile `bt_clock` (HomePage1:250) | → `AddAlarmActivity` (add_alarm_activity.xml), `AddTimerActivity` (add_alarm_quick.xml). |
| `alarms/AlarmScreenActivity.java:49` | `layout/alarm_screen.xml` | `broadcast_receivers/AlarmReceiver.java:49` (:remote process) | Full-screen show-when-locked `TimedBaldActivity`; cancel + snooze only. |
| `activities/pills/PillsActivity.java:51` | `layout/activity_reminders.xml` | Home tile `bt_reminders` (HomePage1:243) | → `AddPillActivity` (add_pill_activity.xml), `PillTimeSetterActivity`. |
| `pills/PillScreenActivity.java:44` | `layout/reminder_screen.xml` | `broadcast_receivers/ReminderReceiver.java:47` | "Took"/"Snooze" full screen. |
| `pills/PillTimeSetterActivity.java:31` | `layout/activity_reminder_time_setter.xml` + `hour_and_minute_choosers.xml` | PillsActivity:98 and Settings>personalization>"time_changer" (SettingsActivity:254) | Morning/afternoon/evening times via `BaldMultipleSelection` + `BaldNumberChooser`. |
| `activities/media/PhotosActivity.java:45`, `VideosActivity.java:43` | `layout/activity_media_scrolling.xml` | Home tiles `bt_photos`/`bt_videos` (HomePage1:234,242); Videos also `GET_CONTENT video/*` (manifest:181-185) | Base `MediaScrollingActivity`; → `SinglePhotoActivity`/`SingleVideoActivity` (`single_media_activity.xml`, fragments `fragment_image`/`fragment_video`). |
| `activities/AppsActivity.java:48` | `layout/activity_apps.xml` | Home tile `bt_apps` (HomePage1:244-249, deferred until app DB loads); also `CHOOSE_MODE` picker for tile customization (HomePage1:333) | Grid 3/6 cols; dropdown per-app actions; uninstall flow. |
| `activities/NotificationsActivity.java:44` | `layout/activity_notifications.xml` | Top-bar bell (HomeScreenActivity:250-253, slide-down anim) | `PERMISSION_NOTIFICATION_LISTENER`; "clear all" bottom bar. |
| `activities/SettingsActivity.java:84` | `layout/activity_settings.xml` + `settings_item.xml` | HomePage2 `bt_settings` (HomePage2:83-84) | 4 sub-categories: connection / accessibility / display / personalization (SettingsActivity:89-95). Items built programmatically. |
| `activities/TutorialActivity.java:34` | `layout/tutorial_activity.xml` + `tutorial_fragment_1/2/4.xml` | First run only (HomeScreenActivity:209-212, `AFTER_TUTORIAL_KEY`) | `TutorialFragment3` NOT in adapter — only reused inside `KeyboardChangerActivity` (activity_keyboard_changer.xml:35). |
| `activities/KeyboardChangerActivity.java:28` | `layout/activity_keyboard_changer.xml` | Settings>accessibility>set_keyboard (SettingsActivity:249) | Hosts `TutorialFragment3` (keyboard enable/pick walkthrough). |
| `activities/FontChangerActivity.java:37` | `layout/activity_font.xml` | Settings>accessibility>font_size (SettingsActivity:399) | FONT_SIZES scales 0.8–1.7 (SettingsActivity:86). |
| `activities/AccessibilityLevelChangerActivity.java:28` | `layout/activity_accesibilty_changer.xml` | Settings>accessibility (SettingsActivity:257) | — |
| `activities/Page1EditorActivity.java:28` | `layout/activity_page_1_editor.xml` | Settings>personalization>edit_home_screen (SettingsActivity:255) | Reuses `HomePage1` in edit mode (HomePage1:311-353: default/custom/hide dialog → `AppsActivity` CHOOSE_MODE picker). |
| `activities/PermissionActivity.java:60` | `layout/activity_permission.xml` + `permission_item.xml` | Auto: `BaldActivity.onCreate:211-217` redirects when `requiredPermissions()` unmet; also Settings "grant_all_permissions" (SettingsActivity:440-445) | List of permission items; re-launches original intent (EXTRA_INTENT). |
| `activities/TechnicalInfoActivity.java:38` | `layout/activity_tech_info.xml` | Settings>technical_information (SettingsActivity:436-438) | Hardcoded `#FFFFFF` text (activity_tech_info.xml:72). |
| `activities/UpdatesActivity.java:55` | `layout/activity_update.xml` | `UpdatingUtil.checkForUpdates` → only `baldUpdates` flavor | — |
| `activities/CrashActivity.java:29` | `layout/activity_crash.xml` | `BaldUncaughtExceptionHandler:53` | Restart → HomeScreenActivity. |
| `activities/YoutubeActivity.java:41` | `layout/youtube_container.xml` | `BaldActivity.setupYoutube` help buttons; `HelpRecyclerViewAdapter:77` | Help button forced INVISIBLE in `BaldTitleBar.java:120-121` → effectively unreachable. Video IDs in `values/youtube.xml` (en/he tutorials). |
| `activities/VideoTutorialsActivity.java:30` | `layout/activity_help.xml` + `help_item.xml` | HomePage2 `bt_help` (HomePage2:96-97) — **button is `android:visibility="gone"`** (fragment_home_page2.xml:125) | Dead end. |
| `activities/FeedbackActivity.java:29` | `layout/activity_feedback.xml` | **Nothing launches it** | Dead code (mailto feedback form, subject "GranPhone feedback"). |
| `activities/PhoneActivity.java:29` | `layout/activity_phone.xml` | Only via `S.getPhoneIntent` in gPlay flavor branch (HomePage1:299); getPhoneIntent returns `ACTION_DIAL` (S.java:367-369) | Hub: contacts/dialer/recent. Dead in this flavor. |
| `activities/FakeLauncherActivity.java:29` | — | `resetPreferredLauncherAndOpenChooser` (default-launcher prompts) | `enabled="false"` manifest trick. |

Services: `keyboard/BaldInputMethodService` (custom IME, layouts `us_keyboard_layout`/`he_keyboard_layout`/`numbers_keyboard_layout` via `keyboard/{English,Hebrew,Number}Keyboard.java`), `services/BaldInCallService`, `services/NotificationListenerService`. Receivers incl. `AlarmReceiver`/`ReminderReceiver` in `:remote` process, `KioskAdbReceiver` (DUMP-permission kiosk escape), `BaldDeviceAdminReceiver`.

## 2. Design-token reality

**Theme attrs** (`res/values/attrs.xml:18-41`): 24 `bald_*` attrs — `bald_background`, `bald_background_transparent`, `bald_button`, `bald_button_gold`, `bald_text_on_gold`, `bald_button_clicked`, `bald_button_corners_radius`, `bald_button_disabled`, `bald_button_pill_corners_radius`, `bald_decoration_on_{background,button,key}`, `bald_hint_on_{background,button}`, `bald_keyboard_key_{background,pressed_background}`, `bald_selected_on_background`, `bald_text_on_selected`, `bald_stroke_{color,size}`, `bald_text_on_{background,button,key}`, `bald_key_margin`.

**Styles** (`res/values/styles.xml`):
- Base `Theme.AppCompat.Light.NoActionBar.FullScreen` (:19) — note **Light parent for all themes incl. dark**.
- `bald_light` :33, `bald_skin` :62 (comment "looks bad"; **unreachable in UI** — `BPrefs.Themes.THEMES = {light, -1, dark, skin}` at `utils/BPrefs.java:163`, but settings dialog offers only light/adaptive/dark at SettingsActivity:289), `bald_dark` :88.
- Text styles `BackgroundText` (.Tiny/.Medium/.Large/.Huge/.Auto) :126-149 and `ButtonsText` (same variants) :151-175; `ScrollStyle` :117; `EditText` :122; `BaldActionBar` :177; `CallButton` :182-192 (36sp, #FFFFFF).
- Type scale `res/values/dimens.xml:18-22`: huge 28sp / large 21sp / medium 17sp / small 13sp / tiny 8sp.
- Concrete theme values in `res/values/themes_values.xml` (light :24-46, skin :50-70, dark :74-95). Button corners: light/dark 16dp, skin 17dp; pill radius 500dp.
- `res/values/colors.xml`: fixed palette — pill colors :19-23, gold :26-27, toast colors :28-33, `battery_low` :35, call-log tint colors :38-41, **`color_1..4` semi-transparent row tints :44-47** (`#5064ef5b`, `#501de8b5`, `#5080cbc4`, `#50bcaaa4`).

**Theme application**: `utils/S.java:164-172` `getTheme()` (adaptive = light 07:00–19:00 else dark); `BaldActivity.java:225-226` `setTheme(themeIndex)` + recreate on change (:254); `CallActivity.java:94` sets it manually.

**Layout theme coverage**: 57 of 105 `res/layout/*.xml` reference `?bald_*`/`?attr/bald_*` directly (grep `\?(attr/)?bald_`); the rest get tokens indirectly via `@drawable/btn_*`/`style_for_buttons*` selectors (e.g. `drawable/btn_enabled.xml:19-26` uses `?attr/bald_button*` + stroke attrs) and the `BackgroundText`/`ButtonsText` styles. Most activity roots set `android:background="?bald_background"`.

**Hardcoded colors in layouts — 6 total**:
- `layout/activity_tech_info.xml:72` `#FFFFFF`
- `layout/new_bald_dialog_box.xml:21` `#00000000`
- `layout/contact_row_item.xml:68`, `layout/calls_item.xml:74` `civ_border_color="#DD000000"`
- `layout/add__edit_conatct_activity.xml:159`, `layout/activity_sos.xml:112` `#FFFFFF`

**Hardcoded colors in drawables — 14**: `drawable/call_button_{green,red,gray,blue}.xml` (e.g. `#2e7d32`/`#1b5e20`, `call_button_green.xml:5,11`), `drawable/star_gold.xml:24`, `drawable/ic_{call,call_end,volume_up}_white.xml:8` `#FFFFFFFF`, `drawable/battery_full_on_background.xml:23` `#00c853`, `drawable/transparent.xml:19`.
Plus **title-bar tint**: `android:backgroundTint="@color/color_1"` on `BaldTitleBar` in **31 layouts** (e.g. `dialer.xml:34`, `activity_settings.xml:28`, `activity_sos.xml:15`) — a fixed translucent green over every screen, all themes.
Plus home row tints `drawable/home_item_{1..4}.xml:20` → `@color/color_{1..4}`.

**Hardcoded textSize — 123 occurrences in 16 files** (`android:textSize="\d`):
- Keyboard layouts dominate: `us_keyboard_layout.xml` 34× (mostly `30sp`, first at :42), `he_keyboard_layout.xml` 34×, `numbers_keyboard_layout.xml` 33×.
- `activity_call.xml` 4× (:44,56,67,117; 26–30sp).
- `fragment_home_page1.xml` 2× (:47 `80sp` clock, :59 `22sp` date).
- Others: `single_contact_activity.xml:74`, `contact_number.xml:47`, `contact_row_item.xml:77`, `calls_item.xml:83`, `app_icon.xml:64`, `storage_status.xml` 4×, `large_alarm_item.xml:30`, `add_alarm_quick.xml:57`, `reminder_screen.xml:32`.
- **dp used as textSize** (bug): `tutorial_fragment_1.xml:32,43` (`60dp`/`40dp`), `alarm_screen.xml:33` (`80dp`, `tools:ignore="SpUsage"`), `reminder_screen.xml:32` (`80dp` on an ImageView — dead attribute).
- Also in styles: `CallButton` 36sp (styles.xml:190).

## 3. Component inventory

**Custom views** (`java/.../views/`):
- Button family: `BaldButton`, `BaldImageButton`, `BaldLinearLayoutButton`, `BaldFrameLayoutButton`, `BaldConstraintLayoutButton`, `BaldEditText` — each duplicates the same vibrate/long-press logic (~8 `vibrationFeedback` blocks per file).
- `BaldTitleBar` (`views/BaldTitleBar.java:39`): inflates `bald_title_{back_button,title,help}.xml`; back left/right by `RIGHT_HANDED_KEY` (:116-119); help button set `INVISIBLE` (:120-121, GranPhone change — still occupies space to keep title centered); `setGold` (:92-102) uses `gold`/`on_gold`; base bg `btn_enabled_rectangle` + per-layout `color_1` tint.
- `BaldSwitch` (`views/BaldSwitch.java:37`): two TextViews inflated from `bald_switch_yes.xml`/`bald_switch_no.xml`; used only in `large_alarm_item.xml:74` and `activity_share.xml:65`.
- `BaldPictureTextButton` (+`bald_picture_text_button_{text,picture}.xml`), `BaldMultipleSelection` (+`multiple_vertical_selection_view_item`, `bald_dialog_box_multiple_selection_view`), `BaldNumberChooser` (+`number_chooser.xml`, `hour_and_minute_choosers.xml`), `ScrollingHelper` (up/down arrow overlays `scrolling_helper_{up,down}.xml`, `empty_text` attr), `ViewPagerHolder` (+`circles.xml`, `view_pager_holder_{arrows,hint}.xml`), `NonSwipeableViewPager`, `ModularRecyclerView`/`ModularScrollView` (`Modular` interface), `BatteryView` (battery_*_on_background vectors), `FirstPageAppIcon` (`first_page_app_icon.xml` — icon + autosize label + `notifications_pill` badge dot), `HomeScreenAppView` (`home_screen_app_view.xml`), `SquaredCircleImageView`, `BaldKeyboardView` (android KeyboardView subclass), `MediumBaldButtonForTutorial`, `LetterChooserDialog` (fragments_and_dialogs, `full_screen_recycler_view_dialog.xml` + `letter_chooser_item.xml`).
- Home: `views/home/HomeView` (base), `HomePage1`, `HomePage2`, `NotesView` (`notes_fragment.xml`), `HomeViewFactory` (`home_factory.xml` — pinned-app pages).

**Dialogs**: `utils/BDialog` builder → `new_bald_dialog_box.xml` (title / divider / scroll content / optional EditText / custom frame / big yes-no buttons); `bald_dialog_box_close_button.xml`; dropdown via `drop_down_recycler_view.xml` (`S.showDropDownPopup`). Toasts: `toast_layout.xml` + `BaldToast` (always wraps in `bald_light` — `BaldToast.java:79` — so toasts look light even in dark theme).

**dial_pad.xml**: ConstraintLayout, 12 char `BaldButton`s (`style_for_buttons`) + bottom row `b_clear` (red `style_for_buttons_red`), `b_call` (green), `b_backspace` (blue) — `dial_pad.xml:213,230,247`.

**Home structure**: `fragment_home_page1.xml` — top clock/date card (`style_for_buttons`, 80sp/22sp hardcoded), then 4 equal-weight rows (`phone_container` 110dp fixed height at :71 vs others `layout_weight=1` — inconsistent) × 3 `FirstPageAppIcon`s, each row backed by `home_item_1..4` tinted drawables, `ll_divider_as_horizontal_layout` separators. `fragment_home_page2.xml` — 4 `BaldLinearLayoutButton` rows (settings / internet / calculator / tutorials), rows 2 & 4 `gone` (:59,125); trailing Space weight 3.

## 4. User-flow map

- **Home**: `home_screen_top_bar` → notifications (bell), ringer-mode dropdown, battery % toast; Page1 tiles: Recent, Dialer, Contacts | SOS, Flashlight (torch toggle, HomePage1:208-211), Messages (system default SMS pkg :213-233) | Photos, Camera (system camera intent :173-187), Videos | Pills, Apps, Alarms. All tiles re-mappable/hideable via Page1EditorActivity (`CUSTOM_*_KEY` prefs, `HIDDEN_TILE`, seeded at HomePage1:189-199). Page2 (swipe left): Settings, Calculator (hardcoded pkg `com.android.calculator2`, HomePage2:88-94); internet+tutorial rows hidden. Page further left: Notes (speech recognizer, `NotesView.RecognizerManager`). Pages right: pinned apps/contacts.
- **Call**: Dialer/`tel:` → `DialerActivity.call()` → dual-SIM dialog (DialerActivity:105-132) → system `ACTION_CALL` → `BaldInCallService` → `CallActivity` (ring/active/hold/SIM-pick/end). Back press blocked during call (CallActivity:151-155).
- **Contacts**: tile → list (BaseContactsActivity) → `SingleContactActivity` (call/WhatsApp/mail/address rows `contact_*.xml`) → `AddContactActivity` edit; add from dialer/recents; share → `ShareActivity`.
- **SOS**: tile → SOSActivity → pinned-contact call or 112; picking contact → ContactsActivity MODE_SOS.
- **Alarms**: tile → list (`large_alarm_item` + BaldSwitch) → AddAlarmActivity / AddTimerActivity → AlarmReceiver (:remote) → AlarmScreenActivity.
- **Pills**: tile → list (`large_reminder_item`) → AddPillActivity → ReminderReceiver → PillScreenActivity; times via PillTimeSetterActivity (also reachable from Settings>Personalization).
- **Media**: tiles → grid (MediaScrollingActivity) → SinglePhoto/SingleVideo.
- **Notifications**: top-bar bell → list; prompts system listener settings if revoked (NotificationsActivity:85-98).
- **Apps**: tile → grid → per-app dropdown (open/uninstall/info).
- **Settings**: Page2 → SettingsActivity → category list → sub-categories → BDB option dialogs or sub-activities (FontChanger, AccessibilityLevelChanger, KeyboardChanger, Page1Editor, PillTimeSetter, TechnicalInfo, PermissionActivity) or system settings intents (wifi/bt/nfc/location/locale/display/airplane).
- **Keyboard**: Settings>accessibility>set_keyboard → KeyboardChangerActivity → TutorialFragment3 → system IME picker; IME itself = `BaldInputMethodService` + `keyboard/*Keyboard.java`.
- **Tutorial**: auto on first run (HomeScreenActivity:209) → TutorialActivity 3 pages; sets AFTER_TUTORIAL_KEY immediately (TutorialActivity:40).
- **Permissions**: any `BaldActivity` with unmet `requiredPermissions()` → PermissionActivity → returns to original intent; "grant all" also in Settings.
- **Kiosk**: Settings → PIN dialog (SettingsActivity:155-199); escape via ADB-only `KioskAdbReceiver` (manifest:323-332).
- **Dead ends**: VideoTutorialsActivity & YoutubeActivity (entry hidden — BaldTitleBar:121, fragment_home_page2.xml:125), FeedbackActivity (no callers), PhoneActivity (non-gPlay), `bald_skin` theme (not in settings options), `TutorialFragment3` inside tutorial pager.

## 5. Visual inconsistencies visible in code

- **CallActivity is outside the BaldActivity family** (`CallActivity.java:61` extends AppCompatActivity): no accidental-touch guard, no status-bar handling, no auto-dismiss dialogs; themed only via its own `setTheme(S.getTheme)` (:94). Buttons: fixed green/red/gray/blue drawables with **28dp radius vs theme 16dp** (`call_button_green.xml:6,12` vs `themes_values.xml:28,78`), fixed 120/96/88dp heights, `CallButton` style 36sp white text (`styles.xml:182-192`) — does not scale with `FONT_SIZES`.
- **Title bar tint hardcoded**: `@color/color_1` (`#5064ef5b`) on `BaldTitleBar` in all 31 title-bar layouts — same translucent green on dark theme; contrast varies with theme.
- **Colored home rows**: `home_item_1..4` use fixed `color_1..4` tints (drawable/home_item_1.xml:20 etc.) regardless of theme; `colorful` pref only toggles colored rows in contacts/calls adapters (CallsRecyclerViewAdapter:157, ContactRecyclerViewAdapter:184) — not the home tiles.
- **Corner radii mix**: 16dp (theme), 17dp (skin), 28dp (call buttons), 500dp pill (`bald_button_pill_corners_radius`), 7dp `key_radius` (dimens.xml:31), 13dp `pin_radius` (:25).
- **Icons**: virtually all **vector XML** drawables (~160 files) tinted via `?bald_decoration_on_*` (e.g. `phone_on_background.xml:23`); naming convention `*_on_background` vs `*_on_button` encodes intended surface. Raster only: `drawable/frame_30k.png`, `mipmap-*/ic_launcher.png`. `ic_*_white` call icons are fixed `#FFFFFFFF`. Signal levels `signal_0..4` + `signal_level` level-list; battery vectors `battery_*`.
- **Hardcoded text colors**: `#FFFFFF` in SOS emergency row (activity_sos.xml:112), tech info (:72), add-contact (:159); `CallButton` #FFFFFF (styles.xml:189).
- **Toast always light-themed** (`BaldToast.java:79` `R.style.bald_light` ContextThemeWrapper).
- **textSize in dp** instead of sp: tutorial_fragment_1.xml:32,43; alarm_screen.xml:33; reminder_screen.xml:32.
- **Base theme is `AppCompat.Light`** even for `bald_dark` (styles.xml:19,88) — framework widgets (e.g. platform `Button` in activity_call.xml, `Button`/`ImageButton` in alarm_screen.xml:57,71) inherit light-theme defaults unless overridden.
- Page-1 phone row fixed 110dp height while sibling rows are weight-based (fragment_home_page1.xml:71 vs :120).
- `AndroidManifest.xml` `android:theme="@style/bald_light"` (:58) is only the pre-onCreate default; actual theme always re-set in code.

## 6. Localization

`res/values-*/` locales (37): ar, bg, ca, cs, da, de, el, es, et-rEE, eu, fi, fi-rFI, fr, ha, hi, hr, hu, id, it, iw, ka, ko, nl, nl-rNL, pl, pt, pt-rBR, ro, ru, sl, sr, sr-rRS, sv, tr, zh, zh-rTW. `supportsRtl="true"` (manifest:57); `drawable-ldrtl/` mirrors 3 directional drawables.

`<string`-tag counts (includes string-array tags): default `values/strings.xml` **369**; ru 368, pt-rBR 363, iw 331, de 329, es 329, fr 318, **ar 140** (largely untranslated). Non-translated strings live in `values/ntstrings.xml` (app_name="GranPhone" :19, authorities, test numbers). GranPhone-added strings (e.g. `kiosk_enabled`, `use_granphone_for_calls`) exist only in default, `values-ru` (:428,438), `values-pt-rBR` (:418,425) — all other locales fall back to English for newer features (kiosk, in-call UI, signal, etc.). `values/youtube.xml` holds translatable YouTube tutorial IDs (en/he content).

## Key files for a redesign pass
- Token definitions: `res/values/{attrs,styles,dimens,colors,themes_values}.xml`
- Theme switch: `utils/BPrefs.java:163`, `utils/S.java:164-172`, `activities/BaldActivity.java:225`
- Buttons: `res/drawable/btn_*.xml`, `style_for_buttons*.xml`, `home_item_*.xml`, `call_button_*.xml`
- Chrome: `views/BaldTitleBar.java`, `layout/bald_title_*.xml` (+`backgroundTint` in 31 layouts)
- Home: `layout/{home_screen,home_screen_top_bar,fragment_home_page1,fragment_home_page2,first_page_app_icon,home_screen_app_view,home_factory}.xml`, `views/home/*`, `adapters/BaldPagerAdapter.java`
