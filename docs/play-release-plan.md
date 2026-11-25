# GranPhone → Google Play release plan

Собрано из официальных источников (developer.android.com, Play Console Help) + аудит кода, 24.11.2025.

## Жёсткие факты из ресёрча

| Требование | Статус | Источник |
|---|---|---|
| Новые приложения: `targetSdk 35` с 31.08.2025; с 31.08.2026 — target 36 | ближайшее окно ~9 мес | developer.android.com/google/play/requirements/target-sdk |
| Личные аккаунты (после 13.11.2023): **12 тестеров × 14 дней** закрытого теста, непрерывно; потом заявка на production | календарный минимум, не сократить | support.google.com …/14151465 |
| Edge-to-edge принудительно на target 35 (Android 15+); `windowOptOutEdgeToEdgeEnforcement` — временный opt-out, **отключён на target 36** | нужна инсет-стратегия | developer.android.com/about/versions/16 |
| 16KB pages: Java-only приложения совместимы автоматически | ✅ у нас 0 `.so` в APK | developer.android.com/guide/practices/page-sizes |
| `READ/WRITE_CALL_LOG` — только default Phone/Assistant handler | мы лаунчер, не дайлер → **в gPlay уже вырезано** (`tools:node="remove"` + `recentCalls:false`) | support.google.com …/10208820 |
| `REQUEST_INSTALL_PACKAGES` — только если core = установка пакетов | self-update не квалифицируется → **в gPlay уже вырезано** | support.google.com …/12085295 |
| `USE_FULL_SCREEN_INTENT` — декларация; alarm/dialer-приложения квалифицируются | наши alarm-экраны = основание | support.google.com …/13392821 |
| F-Droid: только FOSS-зависимости, сборка из исходников их тулчейном, fastlane-метаданные в репо | gms/firebase/ads в коде нет ✅ | f-droid.org/docs/Inclusion_Policy |

## Аудит кода: что реально блокирует

| # | Проблема | Эффект на target 35 | Файлы |
|---|---|---|---|
| B1 | **26 intent-filter'ов, `android:exported` только у 4** | `INSTALL_PARSE_FAILED` — приложение не ставится на API 31+ | AndroidManifest.xml, всё |
| B2 | `PendingIntent`: 12 вызовов, mutability-флаг только в 2 | `IllegalArgumentException` при создании на API 31+ | AlarmScheduler, ReminderScheduler, MissedNotifications, BaldInCallService, BaldUncaughtExceptionHandler |
| B3 | `POST_NOTIFICATIONS` отсутствует + нет запроса | на API 33+ уведомления молчат (SOS/missed calls/alarms!) | manifest + место первого показа |
| B4 | Edge-to-edge → контент под статусбаром/навбаром | визуал ломается на Android 15+ | все экраны; см. стратегию ниже |
| B5 | `BLUETOOTH_ADMIN` legacy на API 31+ | надо `BLUETOOTH_CONNECT` рантайм | manifest + места использования |
| B6 | `gradle.properties` закоммичен с машинным `aapt2FromMavenOverride` путём | ломает сборку на любой другой машине/F-Droid | gradle.properties |
| B7 | NotificationListenerService (`BIND_NOTIFICATION_LISTENER`) | sensitive perm → декларация в Play Console | manifest |
| B8 | Foreground service types (target 34+) | если есть startForeground → тип обязателен | services audit |
|  | `setAlarmClock` — НЕ требует exact-alarm пермишена | ✅ ничего делать не надо | AlarmScheduler, ReminderScheduler |
|  | Notification channels уже есть (calls, missed) | ✅ | BaldInCallService, MissedNotifications |
|  | `android:exported` у BaldInCallService проставлен | ✅ образец | manifest |

## Фазы

### Фаза 0 — гигиена (0.5 дня)
1. Коммит или откат недописанного `pin_page` в `activity_settings.xml` (сейчас дерево сломано для релизной сборки)
2. Решить некоммиченный `Switch→BaldSwitch` в HomeScreenActivity
3. B6: вынести `aapt2FromMavenOverride` из закоммиченного `gradle.properties` в `local.properties` (и документировать в AGENTS.md — там уже есть упоминание)
4. Тегнуть последний рабочий коммит `pre-play-migration`

### Фаза 1 — target 29→35 (основная работа)
Стратегия: `targetSdkVersion` поднимаем **глобально** (не только gPlay) — один код, один таргет; fDroid-билд от этого не страдает (он собирается тем же APK). Edge-to-edge — через opt-out сначала, инсеты потом инкрементально (opt-out работает до target 36, т.е. до ~августа 2026).

1. **B1 exported-аудит**: каждому компоненту с intent-filter явный `android:exported`. Правило: компоненты с системными/внешними фильтрами (HOME, DIALER, BOOT, notification listener) → `true`; внутренние → `false`. Проверка: установка на api34-эмулятор не должна падать.
2. **B2 PendingIntent**: пройти 12 вызовов, добавить `FLAG_IMMUTABLE` (alarm/reminder = immutable; если где-то intent модифицируется — `FLAG_MUTABLE`).
3. **B3 POST_NOTIFICATIONS**: манифест + запрос при первом входе (PermissionActivity flow — там уже есть визард разрешений, вписать туда). Fallback: если отказано — тихо работать (не блокировать).
4. **B4 Edge-to-edge — путь opt-out**: в темах добавить `windowOptOutEdgeToEdgeEnforcement=true`. Потом инкрементально, по экранам: home → settings → dialogs (свайп-анимации у нас уже свои, проверить PageDrag под инсетами). **Не откладывать до 2026 — флаг умрёт на target 36.**
5. **B5 Bluetooth**: `BLUETOOTH_CONNECT` + `usesPermissionFlags="neverForLocation"` + версионный гард в коде.
6. **B7/B8**: BaldInCallService → `foregroundServiceType="phoneCall"` если стартует FGS; NotificationListenerService — декларация «notification badge/count на доме».
7. targetSdkVersion 29→35 в `defaultConfig`; прогон визуального свипа на api23/34 заново (edge-to-edge виден только на 15+ — нужен api35-эмулятор для полной проверки!).

### Фаза 2 — Play-совместимость (параллельно фазе 1)
1. `gPlay`-манифест: уже режет CALL_LOG+INSTALL_PACKAGES ✅ — проверить, что код Recent/CallLog фичей под `recentCalls:false` не падает (PermissionActivity, HomePage1, KioskManager используют CallLog)
2. Permissions Declaration Form (появится в консоли при загрузке AAB):
   - `USE_FULL_SCREEN_INTENT` → «alarm clock & incoming call UI»
   - `SYSTEM_ALERT_WINDOW` → «call/alarm overlays» — проверить, нужен ли вообще в gPlay (можно вырезать, если без overlay'ев живём)
   - `WRITE_SETTINGS` → «brightness control — core feature»
   - Notification access → «missed-notification badges on home»
3. Privacy policy: простая страница (GitHub Pages на том же репо): какие данные, куда (никуда — всё локально), контакты. Плюс-фича приложения — «no data leaves the device».
4. Data safety форма: честно — контакты/журнал звонков обрабатываются локально, не собираются.
5. Content rating (IARC) — без насилия/покупок, Adults 18+ audience.
6. Видео-демо 30-60с для пермишен-ревью: home → dialer → settings → alarm.

### Фаза 3 — аккаунт и тест (2+ недели календарно, начать РАНЬШЕ)
1. Регистрация Play Console ($25) + верификация личности/организации (документы, ~2-7 дней)
2. Internal track → сразу Closed test: найти 12 тестеров (семья, друзья, сообщества — r/androidapps, форумы карегиверов, BaldPhone community). Правило: тестер должен оставаться opted-in 14 дней подряд — иначе часы обнуляются → набрать 15-18 с запасом.
3. Пока тест идёт — доделывать листинг и фиксить баги от тестеров.
4. Заявка на production access (вопросы о тестировании).

### Фаза 4 — листинг и релиз
1. AAB (`assembleGPlayRelease` / `bundleGPlayRelease`) + Play App Signing + keystore (**keystore backup — потеря = потеря приложения навсегда**)
2. Ассеты: иконка 512, feature graphic 1024×500, 2+ phone-скриншота, (опц.) tablet
3. Описание: EN+RU; core-фичи в описании обязательны (пермишен-ревью сверяет)
4. Цена: разовая покупка $2.99–4.99 ИЛИ free на старте (платный listing можно включить позже — проще сразу paid? Решение: сначала free+оценки, потом... — paid→free нельзя обратно! free→paid тоже нельзя. **Решить до первого релиза: раз и навсегда.** Рекомендация: free с «Pro unlock» позже через IAP, либо сразу paid если уверен)
5. Production release → ревью 3-7 дней (пермишен-декларации + видео ускоряют)

### Фаза 5 — F-Droid (фоновый, после стабильного gPlay)
1. Чистый чекаут → сборка без машинных путей (после фикса B6)
2. `fastlane/metadata/android/en-US/` (short/full description, icon, screenshots) + `ru-RU`
3. Git tag на релизный коммит
4. MR в fdroiddata → ревью недели. Риск: AGP 4.2/Gradle 6.7 может не собраться их тулчейном → запасной план: reproducible build или собственный F-Droid репозиторий

## Риски

- **12/14 правило** — единственный пункт, который нельзя ускорить деньгами/усилиями. Каждый тестер, отписавшийся раньше срока, обнуляет свои 14 дней.
- **Perm declarations** — могут запросить доп. видео или отказать в Notification access (тогда вырезать фичу из gPlay).
- **Edge-to-edge на реальных OEM** (HiOS/Tecno) — свои закругления; тестировать на api35-эмуляторе + физическом Tecno.
- **target 36 через ~9 мес** — edge-to-edge opt-out умрёт, нужна будет вторая миграция (инсеты + predictive back `enableOnBackInvokedCallback` станет обязательным — наш PredictiveBackHelper к этому готов).
- **Paid vs free навсегда** — решить до первой публикации.

## Не делал / открытые вопросы
- Не проверял собираемость чистым чекаутом (фаза 5 закрывает)
- Вопрос к тебе: Play Console аккаунт — личный или организация? (от этого зависит правило 12/14 — организации старше нов.2023 освобождены)
- Paid сразу или free→IAP потом — решить до листинга
