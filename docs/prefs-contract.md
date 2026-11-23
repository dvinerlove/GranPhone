# BPrefs contract

BPrefs keys live in the `SharedPreferences` file **`baldPrefs`** (`BPrefs.KEY`, same as `Const.BALD_PREFS`), `MODE_PRIVATE`.

A **second prefs file** exists outside BPrefs: **`PINNED_CONTACTS_KEY`** (`HomeScreenPinHelper.PinnedContactPreferences.KEY`, `HomeScreenPinHelper.java:127`) holding `PINNED_CONTACTS_SET_KEY` (home-screen pinned contacts) and `PINNED_CONTACTS_SET_KEY_SOS` (SOS pinned contacts) as `StringSet`s — the SOS chain reads these.

**Rule: key names and defaults are frozen.** They are already written to users' devices — renaming a key or changing a default silently changes behavior on upgrade. A new pref = a new `public static final` constant in `BPrefs.java` + a row in this table.

Source of truth: `app/src/main/java/com/bald/uriah/baldphone/utils/BPrefs.java`.

## Keys

| Constant | Type | Default | Controls |
|---|---|---|---|
| `KEY` | — | `"baldPrefs"` | The prefs file name itself. Not a pref. |
| `TOUCH_NOT_HARD_KEY` | boolean | `false` | Accidental-touch guard strictness (enables touch in `ModularRecyclerView`/`ModularScrollView`). |
| `RIGHT_HANDED_KEY` | boolean | `true` | Handedness; mirrors layouts (`BaldTitleBar`). |
| `CUSTOM_APP_KEY` | String | none | Custom app pinned to home. Marked backward-compatible; `TechnicalInfoActivity` can `remove()` it. |
| `CUSTOM_RECENTS_KEY` | String | none | Replacement app for Recents tile (component name). |
| `CUSTOM_DIALER_KEY` | String | none | Replacement for Dialer. |
| `CUSTOM_CONTACTS_KEY` | String | none | Replacement for Contacts. |
| `CUSTOM_ASSISTANT_KEY` | String | none | Replacement for Assistant. |
| `CUSTOM_MESSAGES_KEY` | String | none | Replacement for Messages. |
| `CUSTOM_PHOTOS_KEY` | String | none | Replacement for Photos. |
| `CUSTOM_CAMERA_KEY` | String | none | Replacement for Camera. |
| `CUSTOM_VIDEOS_KEY` | String | none | Replacement for Videos. |
| `CUSTOM_PILLS_KEY` | String | none | Replacement for Pills. |
| `CUSTOM_APPS_KEY` | String | none | Replacement for Apps. |
| `CUSTOM_ALARMS_KEY` | String | none | Replacement for Alarms. |
| `LONG_PRESSES_KEY` | boolean | `false` | Long-press-to-activate. GranPhone default: simple tap opens things; accidental-touch guard stays on separately. |
| `TEST_KEY` | boolean | `false` | Test-mode flag read by `BaldActivity.testing` (skips tutorial gate etc.). |
| `LONG_PRESSES_SHORTER_KEY` | boolean | `true` | Shorter long-press duration. |
| `STATUS_BAR_KEY` | int | `0` | Custom status bar mode; `1` enables it (`BaldActivity`/`HomeScreenActivity`). |
| `VIBRATION_FEEDBACK_KEY` | boolean | `true` | Haptic feedback on presses. |
| `THEME_KEY` | int | `0` (`Themes.LIGHT`) | Index into `Themes.THEMES` — see theme semantics below. |
| `NOTE_VISIBLE_KEY` | boolean | `false` | Show the home-screen note (`NotesView`). |
| `EMERGENCY_BUTTON_VISIBLE_KEY` | boolean | `true` | Show the SOS/emergency button. |
| `LOW_BATTERY_ALERT_KEY` | boolean | `true` | Low-battery alert (`BatteryView`). |
| `DIALER_SOUNDS_KEY` | boolean | `true` | Dial-pad tones. |
| `DUAL_SIM_KEY` | boolean | `false` | `true` = offer SIM choice when calling. |
| `NOTE_KEY` | String | none | The home-screen note text. |
| `AFTER_TUTORIAL_KEY` | boolean | `false` | Tutorial completed; written by `TutorialActivity`/`TutorialFragment4`, read by `HomeScreenActivity`. |
| `PAGE_TRANSFORMERS_KEY` | int | `PageTransformers.PLAIN` | Home pager transition style. Stored name is camelCase: `"pageTransformersKey"`. |
| `USE_ACCIDENTAL_GUARD_KEY` | boolean | `true` | Master switch for the accidental-touch guard. |
| `CRASH_REPORTS_KEY` | boolean | `true` | Declared, currently unused — never read (sole occurrence `BPrefs.java:93`). |
| `APPS_ONE_GRID_KEY` | boolean | `false` | Apps screen: one tall grid vs paginated. |
| `COLORFUL_KEY` | boolean | `false` | Colorful app icons vs uniform. |
| `HIDE_BOTTOM_ROW_KEY` | boolean | `false` | Home screen: hide the fixed SOS/Apps/Settings row. Set via Settings → helper layer. |
| `LANGUAGE_KEY` | String | `""` | Per-app UI locale (`""` = follow system). BCP-47-ish tag matching shipped `values-*` dirs (e.g. `ru`, `pt-rBR`). Applied in `BaldActivity.attachBaseContext`; activities recreate on change. |
| `LAST_CRASH_KEY` | long | `-1` | Timestamp of last crash (`BaldUncaughtExceptionHandler`). Within `LAST_CRASH_TIME_OK` (12s, `12 * Const.SECOND`) = crash loop → `System.exit`. Not a user pref. |
| `LAST_APK_VERSION_KEY` | int | `-1` | Version code of last downloaded APK (`UpdatesActivity`). |
| `LAST_UPDATE_ASKED_VERSION_KEY` | long | `0` | Timestamp when update prompt last shown (`UpdatingUtil`; read at `HomeScreenActivity.java:312`). |
| `UUID_KEY` | String | — | Declared, currently unused. |
| `LAST_DOWNLOAD_MANAGER_REQUEST_ID` | long | `-1` | In-flight `DownloadManager` request id (`UpdatesActivity`/`DownloadManagerReceiver`). |
| `LAST_DOWNLOAD_MANAGER_REQUEST_VERSION_NUMBER` | int | `-1` | Version that request downloads. Both `*_REQUEST_*` keys present = download in progress. |
| `ALARM_VOLUME_KEY` | int | `4` | Alarm volume level; applied as `(value + 6) / 10` × `STREAM_ALARM` max (`AlarmScreenActivity.java:76`), edited by slider (`SettingsActivity.java:502,519`). |
| `HOUR_KEY_` / `MINUTE_KEY_` | int | per slot | Key **prefixes**: real key is prefix + `Reminder.Time` slot. Pills reminder times; defaults come from `PILLS_HOUR_DEFAULTS`/`PILLS_MINUTE_DEFAULTS`: MORNING 7:30, AFTERNOON 12:30, EVENING 19:30. Read via `BPrefs.getHour/getMinute`. |

## Theme index semantics (`BPrefs.Themes`)

`THEME_KEY` stores an **array index**, not a flag:

```java
LIGHT = 0; ADAPTIVE = 1; DARK = 2;
THEMES = { R.style.bald_light, R.style.bald_light, R.style.bald_dark };
```

- Index 1 (ADAPTIVE) resolves to `bald_light` — since `dc52646` removed `bald_skin`, slot 1 is a deliberate duplicate so old installs with `THEME_KEY=1` still open. `AppUtils.getTheme` also has a now-dead `-1` adaptive branch.
- **`THEME_KEY ≥ 3` (e.g. old `bald_skin`=3) crashes** — `THEMES[index]` in `AppUtils.getTheme` is not bounds-checked (`AppUtils.java:165`). Any code writing `THEME_KEY` must clamp to `0..2`.
- Access theme via `AppUtils.getTheme(context)`; never index `THEMES` elsewhere.
