# Accessibility baseline — wave 0 (2026-11-22)

Captured by running `connectedFDroidDebugAndroidTest` on emulator-5554 (API 29)
with `AccessibilityChecksTestRunner` enforcing ATF checks. Wave 0 runs in
log-and-continue mode (`setThrowExceptionForErrors(false)`); the list below is
what strict mode reported plus what the validator logged.

## Strict-mode failures (throwing run, before suppression)

`PhoneActivityTest.phoneActivityTest` and `SOSActivityTest.sOSActivityTest` each
aborted with `AccessibilityViewCheckException: There were 6 accessibility errors`
on the very first Espresso action. Identical set on both — it's the shared
bottom-bar / quick-dial strip:

| View (res-name) | Class | Violation |
| --- | --- | --- |
| `flash` | BaldImageButton | View is missing speakable text needed for a screen reader |
| `sound` | BaldImageButton | View is missing speakable text needed for a screen reader |
| `notifications` | BaldImageButton | View is missing speakable text needed for a screen reader |
| `sos` | BaldImageButton | View is missing speakable text needed for a screen reader |
| `left_arrow` | BaldImageButton | View is missing speakable text needed for a screen reader |
| `battery` | BatteryView | View is missing speakable text needed for a screen reader |

## Logged-only findings (log-and-continue run, logcat `AccessibilityValidator`)

Home screen (`HomeScreenActivity`), one check pass:

- `SpeakableTextPresentCheck` errors — same six views as above
  (`flash`, `sound`, `notifications`, `sos`, `left_arrow`, `battery`).
- `DuplicateSpeakableTextCheck` warnings — clickable views whose speakable text
  is identical to another view's. Includes several `GONE` first-page icons and
  duplicated entries:
  - `bt_dialer`, `bt_contacts`, `bt_recent`, `bt_settings`, `bt_maps`,
    `bt_messages`, `bt_photos`, `bt_reminders`, `bt_clock`, `bt_apps`,
    `bt_whatsapp`, `bt_assistant`
  - (Texts observed in the emulator's Russian locale, e.g. "Телефон" on
    `bt_dialer` matching two other views.)

## Notes for waves 2–4

- All six hard errors are "missing speakable text" on icon-only `BaldImageButton`
  / `BatteryView` — likely one shared fix (contentDescription on the button
  class or the shared bottom-bar layout) burns down most of the list.
- `DuplicateSpeakableTextCheck` fires on `GONE` views too; the home screen has
  several identical-label icons (e.g. two "SOS"-labelled views).
- Unrelated pre-existing test-suite failures, NOT a11y: the `screenshots/*`
  tests fail on this emulator (`LocaleRule`/`BaseScreenshotTakerTest`
  ArrayIndexOutOfBounds on a 1-locale environment, `/sdcard/Pictures` write
  ENOENT, and `ContactsSaver.insertContacts` NPE that crashes the whole
  instrumentation run). `PhoneActivityTest`/`SOSActivityTest` also have
  `NoMatchingViewException` steps deeper in — stale espresso recordings vs
  current layout/app state.
