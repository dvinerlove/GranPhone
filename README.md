# GranPhone

A simple, offline-first Android launcher + dialer for elderly users — big buttons, minimal distractions, no account, no ads, no telemetry.

Forked from [BaldPhone](https://github.com/UriahShaulMandel/BaldPhone) (Apache 2.0) via [vophone](https://github.com/dhqdev/vophone).

## What it does

- Replaces the home screen with large tiles: Phone, Contacts, SOS, Messages, Photos, Alarms, Flashlight
- Acts as the default dialer: big-button incoming/outgoing call screen (`InCallService`)
- Optional kiosk mode (device owner) to lock the phone down to the essentials
- Full Russian localization, dark/light themes, system font scaling
- Works on old hardware — developed and tested on Android 6 / 1 GB RAM

## Build

```
./gradlew assembleFDroidDebug   # JDK 11, Android SDK 31
```

Output: `app/build/outputs/apk/fDroid/debug/app-fDroid-debug.apk`

Install, then set as default Home app and default Phone app on the device.

## License

Apache 2.0 — see [LICENSE](LICENSE). Credits to the original BaldPhone authors.
