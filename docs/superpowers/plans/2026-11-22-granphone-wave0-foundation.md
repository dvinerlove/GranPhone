# GranPhone Wave 0 — Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the codebase safe to redesign — fix the launcher-crash bug, delete dead code, upgrade the build toolchain, and install the conventions/docs that every later wave depends on. Zero intentional visual change.

**Architecture:** Incremental hardening of the existing Java/XML app. No structural moves; renames and deletions only where they're mechanical and safe.

**Tech Stack:** Java 8, Android Views/XML, Gradle 5.4.1→6.7.1, AGP 3.5.3→4.2.2, Room 2.2.5, Espresso + Accessibility Test Framework.

**Spec:** `docs/superpowers/specs/2026-11-22-granphone-ux-redesign-design.md`

## Global Constraints

- minSdk 21 must remain; no Compose, no Kotlin, no new dependencies beyond this plan's list.
- MDC ceiling: `com.google.android.material:material:1.13.0` (only if touched; wave 0 doesn't require the bump).
- Every commit must leave the app installable and usable on the emulator (`emulator-5554`).
- Never rename/delete a `BPrefs` key or change a default value.
- Java package `com.bald.uriah.baldphone` stays (applicationId is already `com.granphone.launcher`); only file/class names change.
- Commit style per repo history: short imperative subject.

---

### Task 1: Fix `signalView` NPE crash

**Files:**
- Modify: `app/src/main/java/com/bald/uriah/baldphone/activities/HomeScreenActivity.java:144-155`

**Interfaces:**
- Consumes: `PhoneStateListener`, `SignalStrength`
- Produces: none (defensive fix only)

- [ ] **Step 1: Add the null guard**

In the `signalListener` anonymous class (`onSignalStrengthsChanged`), the callback can fire before `signalView` is bound (findViewById at line ~463 runs later, or returns null on a layout variant) or after the view hierarchy is gone. Guard it:

```java
        @Override
        public void onSignalStrengthsChanged(android.telephony.SignalStrength signalStrength) {
            if (signalStrength == null || signalView == null)
                return;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                signalView.setImageLevel(signalStrength.getLevel());
            else {
                final int asu = signalStrength.getGsmSignalStrength();
                signalView.setImageLevel(asu == 99 ? 0 : Math.min(4, asu / 6));
            }
        }
```

- [ ] **Step 2: Build + install**

```bash
./gradlew installFDroidDebug   # or the debug variant used by CI (see .github/workflows)
```
Expected: BUILD SUCCESSFUL, app still launches as home on `emulator-5554`.

- [ ] **Step 3: Regression check — rotate + force-restart**

```bash
ADB="C:/Users/adrew/AppData/Local/Android/Sdk/platform-tools/adb.exe"
"$ADB" -s emulator-5554 shell settings put system user_rotation 1
"$ADB" -s emulator-5554 shell am force-stop com.granphone.launcher
"$ADB" -s emulator-5554 shell "am start -a android.intent.action.MAIN -c android.intent.category.HOME"
"$ADB" -s emulator-5554 shell "logcat -d | grep -c 'FATAL EXCEPTION.*granphone'"
"$ADB" -s emulator-5554 shell settings put system user_rotation 0
```
Expected: no `FATAL EXCEPTION` for the package; home screen renders (screencap to eyeball).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bald/uriah/baldphone/activities/HomeScreenActivity.java
git commit -m "Fix NPE in signal strength listener crashing the home launcher"
```

---

### Task 2: Give `FakeLauncherActivity` a distinct label

**Files:**
- Modify: `app/src/main/AndroidManifest.xml:157-166`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ru/strings.xml` (add one string each)

**Interfaces:**
- Consumes: `FakeLauncherActivity` chooser trick
- Produces: `@string/fake_launcher_label`

- [ ] **Step 1: Add the label attribute + strings**

In the manifest entry for `.activities.FakeLauncherActivity` add `android:label="@string/fake_launcher_label"`. Add to `values/strings.xml`:

```xml
<string name="fake_launcher_label">GranPhone setup</string>
```

and to `values-ru/strings.xml`:

```xml
<string name="fake_launcher_label">GranPhone — настройка</string>
```

- [ ] **Step 2: Build + verify chooser shows a distinct name**

Reproduce via `S.java`/`SettingsActivity` path (`FakeLauncherActivity.resetPreferredLauncherAndOpenChooser`) or:
```bash
"$ADB" -s emulator-5554 shell "am start -a android.intent.action.MAIN -c android.intent.category.HOME" # after clearing default
```
Expected: chooser lists «GranPhone — настройка» separately from «GranPhone».

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/res/values-ru/strings.xml
git commit -m "Distinct label for the fake launcher chooser entry"
```

---

### Task 3: Delete dead code

**Files:**
- Delete: `app/src/main/java/com/bald/uriah/baldphone/activities/FeedbackActivity.java`
- Delete: `app/src/main/java/com/bald/uriah/baldphone/activities/PhoneActivity.java`
- Delete: their layouts (`app/src/main/res/layout/activity_feedback*.xml`, `activity_phone*.xml` — confirm names with `grep -rln "setContentView" in the two files first`)
- Modify: `app/src/main/AndroidManifest.xml` (remove both `<activity>` entries at ~line 251 and ~261)
- Modify: `app/src/main/res/values/styles.xml:62` (remove `bald_skin` style), `app/src/main/res/values/themes_values.xml:50-59+` (remove `*_skin` resources), `app/src/main/java/com/bald/uriah/baldphone/utils/BPrefs.java:163` (`THEMES` array: remove `-1` placeholder AND `R.style.bald_skin` → `{R.style.bald_light, R.style.bald_dark}`)

**Interfaces:**
- Consumes: nothing live (verified: zero code references to either activity)
- Produces: `Themes.THEMES = {R.style.bald_light, R.style.bald_dark}` — **index semantics change**: persisted `THEME_KEY` values 0/1=light, 2=dark. Since `THEME_DEFAULT_VALUE=LIGHT(0)` and index 1 was `-1`, mapping must keep old stored values valid: keep array length but map `2→bald_dark`. Safest minimal change: `THEMES = {R.style.bald_light, R.style.bald_light, R.style.bald_dark}` — index 2 stays dark, index 1 (never written) falls back to light instead of crashing on `-1`.

- [ ] **Step 1: Confirm zero references**

```bash
grep -rn "FeedbackActivity\|PhoneActivity" app/src/main --include="*.java" | grep -v "activities/FeedbackActivity.java\|activities/PhoneActivity.java"
grep -rn "bald_skin" app/src/main --include="*.java" | grep -v THEMES
```
Expected: no live references (manifest + the two files only; `THEMES` array is the only bald_skin consumer).

- [ ] **Step 2: Delete files, strip manifest entries, strip skin theme**

- [ ] **Step 3: Adjust `THEMES` array** to `{R.style.bald_light, R.style.bald_light, R.style.bald_dark}` in `BPrefs.java:163`.

- [ ] **Step 4: Build + launch**

Expected: BUILD SUCCESSFUL; theme picker still works (light/dark only).

- [ ] **Step 5: Commit**

```bash
git commit -am "Remove dead activities and the unused bald_skin theme"
```

---

### Task 4: Toolchain upgrade — Gradle 6.7.1 + AGP 4.2.2

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl=https\://services.gradle.org/distributions/gradle-6.7.1-all.zip`)
- Modify: `build.gradle` (`classpath 'com.android.tools.build:gradle:4.2.2'`)

**Interfaces:**
- Produces: build that supports `viewBinding` (AGP ≥3.6) and runs on JDK 8–15.

- [ ] **Step 1: Check local JDK** — `java -version`; AGP 4.2 runs on JDK 8–11 (and 15). If the environment only has JDK 17, hold at AGP 4.2 is still fine (it ignores newer APIs), but prefer JDK 11 if available; document whatever is used in `AGENTS.md`.

- [ ] **Step 2: Bump versions, then:**

```bash
./gradlew wrapper --gradle-version 6.7.1   # regenerates wrapper scripts if needed
./gradlew clean assembleFDroidDebug --stacktrace
```
Expected: BUILD SUCCESSFUL. Known friction: `jcenter()` repo still fine on AGP 4.2; if a dependency resolution fails, check `allprojects.repositories` in root `build.gradle` and keep `google()` + `jcenter()`.

- [ ] **Step 3: Install + smoke test on emulator** (home, dialer open, back).

- [ ] **Step 4: Commit**

```bash
git commit -am "Upgrade to Gradle 6.7.1 / AGP 4.2.2"
```

---

### Task 5: Enable ViewBinding

**Files:**
- Modify: `app/build.gradle` (inside `android {}`)

**Interfaces:**
- Produces: `ActivityXxxBinding` classes generated per layout, available to all later waves. Adoption is per-screen later; this task only flips the flag.

- [ ] **Step 1: Add the flag**

```gradle
android {
    ...
    buildFeatures {
        viewBinding true
    }
}
```

- [ ] **Step 2: `./gradlew assembleFDroidDebug`** — Expected: BUILD SUCCESSFUL; generated classes visible under `app/build/generated/`.

- [ ] **Step 3: Commit** — `git commit -am "Enable ViewBinding"`.

---

### Task 6: Room schema export

**Files:**
- Modify: `app/build.gradle` (annotationProcessor options inside `android.defaultConfig` or `android.javaCompileOptions`)
- Modify: `app/src/main/java/com/bald/uriah/baldphone/databases/alarms/AlarmsDatabase.java`
- Modify: `app/src/main/java/com/bald/uriah/baldphone/databases/apps/AppsDatabase.java`
- Modify: `app/src/main/java/com/bald/uriah/baldphone/databases/reminders/RemindersDatabase.java`

**Interfaces:**
- Produces: exported schema JSONs under `app/schemas/` (committed to VCS — migration safety net).

- [ ] **Step 1: Add schema location** to `app/build.gradle`:

```gradle
android {
    ...
    defaultConfig {
        ...
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = ["room.schemaLocation": "$projectDir/schemas".toString()]
            }
        }
    }
}
```

- [ ] **Step 2: Set `exportSchema = true`** in each `@Database` annotation:

```java
@Database(entities = {...}, version = N, exportSchema = true)
```

- [ ] **Step 3: Build** — Expected: `app/schemas/<pkg>.<Db>/N.json` files generated.

- [ ] **Step 4: Commit** — `git add app/schemas app/build.gradle <3 db files> && git commit -m "Export Room schemas"`.

---

### Task 7: Espresso accessibility checks

**Files:**
- Modify: `app/build.gradle` (`testInstrumentationRunner` + dep)
- Create: `app/src/androidTest/java/com/bald/uriah/baldphone/AccessibilityChecksTestRunner.java`

**Interfaces:**
- Produces: `AccessibilityChecksTestRunner` — every Espresso view action runs ATF checks, errors fail the test.

- [ ] **Step 1: Add dependency** to `app/build.gradle` androidTest deps:

```gradle
androidTestImplementation 'androidx.test.espresso:espresso-accessibility:3.2.0'
```

- [ ] **Step 2: Create the runner:**

```java
package com.bald.uriah.baldphone;

import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.runner.AndroidJUnitRunner;

public class AccessibilityChecksTestRunner extends AndroidJUnitRunner {
    {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }
}
```

- [ ] **Step 3: Point `testInstrumentationRunner` at it** in `app/build.gradle` defaultConfig.

- [ ] **Step 4: Run the existing suite** — `./gradlew connectedFDroidDebugAndroidTest`. Expected: existing tests run; accessibility errors will surface — **suppress known-issue noise for now**: if the suite floods with pre-existing violations, wrap the enable with `.setSuppressingResultMatcher(...)` or log-and-continue for wave 0; record the baseline list in `research/.../a11y-baseline.md` so waves 2–4 burn it down.

- [ ] **Step 5: Commit** — `git commit -am "Enable Accessibility Test Framework checks in Espresso"`.

---

### Task 8: Rename cryptic utility classes

**Files (all Java, mechanical renames):**
- `utils/S.java` → `utils/AppUtils.java`
- `utils/D.java` → `utils/Const.java`
- `fragments_and_dialogs/BDB.java` → `fragments_and_dialogs/DialogBuilder.java`
- `utils/BDialog.java` → `utils/DialogFlags.java`

**Interfaces:**
- Produces: same static APIs under readable names; every call site updated in the same commit.

- [ ] **Step 1: Perform renames via git mv + sed**

```bash
git mv app/src/main/java/com/bald/uriah/baldphone/utils/S.java app/src/main/java/com/bald/uriah/baldphone/utils/AppUtils.java
git mv app/src/main/java/com/bald/uriah/baldphone/utils/D.java app/src/main/java/com/bald/uriah/baldphone/utils/Const.java
git mv app/src/main/java/com/bald/uriah/baldphone/fragments_and_dialogs/BDB.java app/src/main/java/com/bald/uriah/baldphone/fragments_and_dialogs/DialogBuilder.java
git mv app/src/main/java/com/bald/uriah/baldphone/utils/BDialog.java app/src/main/java/com/bald/uriah/baldphone/utils/DialogFlags.java
# then update class declarations + every call site:
grep -rln '\bS\.\|\bD\.\|BDB\|BDialog' app/src/main/java | xargs sed -i 's/\bS\./AppUtils./g; s/\bD\./Const./g; s/\bBDB\b/DialogBuilder/g; s/\bBDialog\b/DialogFlags/g'
# fix class names + imports inside the renamed files, and imports elsewhere:
grep -rln 'import com.bald.uriah.baldphone.utils.S;\|utils\.D;\|fragments_and_dialogs.BDB;\|utils.BDialog;' app/src/main/java | xargs sed -i 's/utils\.S;/utils.AppUtils;/g; s/utils\.D;/utils.Const;/g; s/fragments_and_dialogs\.BDB;/fragments_and_dialogs.DialogBuilder;/g; s/utils\.BDialog;/utils.DialogFlags;/g'
```
Careful: `S.`/`D.` regex must stay word-boundary anchored; `R.string.D` doesn't exist but `View.S`/other `S.` fields might — after sed, `grep -rn 'AppUtils\.' | grep -v 'utils.AppUtils\|\bAppUtils\b' | head` to eyeball false positives, and `./gradlew assembleFDroidDebug` is the real gate.

- [ ] **Step 2: Build** — Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit** — `git commit -am "Rename cryptic utility classes (S→AppUtils, D→Const, BDB→DialogBuilder, BDialog→DialogFlags)"`.

---

### Task 9: Docs — DESIGN.md, AGENTS.md, BPrefs inventory

**Files:**
- Create: `DESIGN.md` (repo root)
- Create: `AGENTS.md` (repo root)
- Create: `docs/prefs-contract.md`

**Interfaces:**
- Produces: the conventions every later wave + every future agent run reads first.

- [ ] **Step 1: `docs/prefs-contract.md`** — dump every constant in `BPrefs.java` into a table: key name, type, default, what it controls. Generated via `grep -n "public static final" app/src/main/java/com/bald/uriah/baldphone/utils/BPrefs.java`. Rule stated at top: **key names and defaults are frozen; new prefs need a new constant + row here**.

- [ ] **Step 2: `DESIGN.md`** — the token/component contract from spec §3 (tables copied verbatim: color roles light+dark, typography scale, radius/spacing, the six components with their variants, icon rules). Plus the banned patterns: hardcoded `#` colors in layouts, `dp` in `textSize`, new dialogs not built on `DialogScreen`, unlabeled icons.

- [ ] **Step 3: `AGENTS.md`** — build/test commands (`./gradlew assembleFDroidDebug`, `installFDroidDebug`, `connectedFDroidDebugAndroidTest`), where things live (activities/, views/, databases/, utils/), the rename map (S→AppUtils etc.), the «не ломать» list (InCallService null-binding, BOOT_COMPLETED→AlarmReceiver, SOS chain, `:remote` processes, FakeLauncher trick), pointer to `docs/superpowers/specs/...-design.md`.

- [ ] **Step 4: Commit** — `git commit -am "Add DESIGN.md, AGENTS.md, and the BPrefs contract"`.

---

### Task 10: Wave-0 verification tour

- [ ] **Step 1: Full emulator tour** — home, dialer, contacts, SOS, settings, alarms, pills, editor, tutorial, both themes; screencap each to `research/.../screenshots/v5-wave0-*.png`.
- [ ] **Step 2: Diff against golden-master** — visual check vs `v1–v4` set: wave 0 must produce **zero intentional visual differences**; document any accidental ones.
- [ ] **Step 3: Update `research/.../verification-notes.md`** with results; commit everything.

---

## Post-wave status

Wave 0 done = crash-free launcher, no dead code, modern toolchain, schema visibility, a11y checks running, conventions documented. Wave 1 plan (tokens + shared components) gets written from `spec §3` after this lands.
