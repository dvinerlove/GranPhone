# Installing GranPhone

Step-by-step guide to turn a whole phone into a simple big-button device:
home screen, dialer with huge buttons, optional kiosk mode.
Nothing here flashes a new system — everything is reversible.

## What you need

- The phone, Android 6 or newer (kiosk mode is more complete on Android 9+)
- A USB cable and a computer
- `adb` on the computer: download [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)
- The GranPhone APK from [Releases](https://github.com/dvinerlove/GranPhone/releases) or `app/build/outputs/apk/fDroid/debug/app-fDroid-debug.apk`

## 1. Prepare the phone

1. Do the basic setup first: Wi-Fi, max ring volume, brightness,
   font size (Settings > Display > Font size — pick the largest).
2. Enable Developer options: Settings > About phone > tap "Build number" 7 times.
3. In Developer options enable **USB debugging**.
4. Plug the cable, accept "Allow USB debugging?" on the phone.
5. Check it shows up: `adb devices`

## 2. Install and set as default

```
adb install -r app-fDroid-debug.apk
```

On the phone:

1. Press HOME → pick **GranPhone** → **Always** (default launcher)
2. Settings > Apps > Default apps > Phone app → **GranPhone** (big-button call screen)
3. Optional: GranPhone Settings > "Set home screen" if the resolver keeps appearing

## 3. Optional: kiosk mode (lock down)

Grants device-owner so the user cannot leave GranPhone or open system settings.
Run once over adb (the phone must have no accounts configured):

```
adb shell dpm set-device-owner com.bald.uriah.baldphone/.broadcast_receivers.BaldDeviceAdminReceiver
```

Then enable Kiosk mode in GranPhone settings and set a PIN.
To undo: GranPhone settings → disable kiosk, or factory reset.

## Русская версия

- Нужны: телефон на Android 6+, кабель, `adb` (Platform-Tools) на компьютере, APK из Releases.
- Включите «Отладку по USB» в настройках разработчика, подключите кабель, разрешите отладку.
- `adb install -r app-fDroid-debug.apk`
- На телефоне: HOME → GranPhone → «Всегда»; в настройках по умолчанию выберите GranPhone как «Телефон».
- Киоск-режим (блокировка): команда `dpm set-device-owner` выше, затем включите «Режим киоска» в настройках приложения и задайте PIN.
