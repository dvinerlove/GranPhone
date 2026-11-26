# Agent notes

Launcher for elderly/low-vision users (fork of BaldPhone). Java + XML Views, no Compose, no Kotlin. minSdk 21. Read `DESIGN.md` before touching UI; `docs/prefs-contract.md` before touching prefs.

## Toolchain

- Gradle 8.13 (wrapper), AGP 8.13.2, compileSdk/targetSdk 35.
- Builds require **JDK 17** — set `JAVA_HOME` to a JDK 17 before every `./gradlew` call if the default `java` is not 17.
- AGP 8.13's bundled aapt2 handles compileSdk 35; do not use machine-local `android.aapt2FromMavenOverride` entries. The former AGP 7.4.2/JDK 11 rule and the user-home aapt2 workaround are superseded.
- Flavors (`updating_mechanism` dimension): `baldUpdates`, `fDroid`, `gPlay` (`.gp` suffix, no recents), `gPlayRecentCalls` (`.gprc`, reduced feature set via `${fullApp}` manifest placeholders). Work in `fDroid` unless the task says otherwise.
- ViewBinding is enabled; Room schemas export to `app/schemas/` (commit it — schema diffs are the migration contract).

## Commands

```bash
./gradlew assembleFDroidDebug              # build
./gradlew installFDroidDebug               # build + install on connected device
./gradlew connectedFDroidDebugAndroidTest  # instrumented tests (Espresso; AccessibilityChecksTestRunner enables ATF checks)
```

## Where things live

`app/src/main/java/com/bald/uriah/baldphone/`:

- `activities/` — screens (incl. `alarms/`, `contacts/`, `media/`, `pills/` subpackages)
- `views/` — custom widgets (`Bald*Button`, `BaldTitleBar`, `views/home/` home pages)
- `databases/` — Room DBs (`alarms`, `apps`, `calls`, `contacts`, `home_screen_pins`, `reminders`)
- `utils/` — `BPrefs` (all SharedPreferences keys), `AppUtils`, `Const`, `DialogScreen` (the only dialog component — `info`/`confirm`/`select`/`pin` factories), `BaldPrefsUtils` (change-detection snapshot), `KioskManager`, `CallManager`
- `adapters/`, `broadcast_receivers/`, `services/`, `keyboard/` (IME), `fragments_and_dialogs/`, `content_providers/`
- `BaldPhone.java` — Application class

## Rename map (commit 6315312)

Old → new, all in `utils/`: **`S` → `AppUtils`**, **`D` → `Const`**, **`BDB`/`BDialog` → deleted** (legacy dialogs; use `DialogScreen`). Use only the new names in code and docs.

## Не ломать (do-not-break list)

- **`BaldInCallService`** — system binds/unbinds it; the `notificationManager == null` / `call == null` guards are deliberate. It launches `CallActivity` from the background — that path is the whole call UI.
- **Boot chain** — `BOOT_COMPLETED` → `BootUpBroadcastReceiver` exists only to guarantee `BaldPhone.onCreate()` runs (which reschedules alarms/reminders). Don't "optimize" the empty-looking receiver away.
- **SOS chain** — `SOSActivity` reads pinned contacts from `HomeScreenPinHelper` prefs (max 2) and calls them via `DialerActivity.call`; the real emergency button calls `"112"`. Spec §5 will redesign this; don't break the current path meanwhile.
- **`:remote` processes** — `AlarmReceiver` and `ReminderReceiver` run in `android:process=":remote"`. `BaldPhone.onCreate()` and all static state are per-process; never pass objects between the main and remote processes.
- **`FakeLauncherActivity` trick** — `resetPreferredLauncherAndOpenChooser()` toggles the component enabled to force the "choose home" dialog without permissions. It needs its distinct label (commit `2b68a09`) so the chooser isn't misleading. Don't delete or rename it.

## Specs

- Design contract: `docs/superpowers/specs/2026-11-22-granphone-ux-redesign-design.md` (§3–4 is binding; copied into `DESIGN.md`)
- Evidence base: `research/2026-11-22-granphone-ux-redesign/report.md`
- Prefs: `docs/prefs-contract.md` — key names/defaults frozen
