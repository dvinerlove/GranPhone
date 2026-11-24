/*
 * Copyright 2019 Uriah Shaul Mandel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bald.uriah.baldphone.activities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.app.ActivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.bald.uriah.baldphone.BuildConfig;
import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.alarms.AlarmScreenActivity;
import com.bald.uriah.baldphone.activities.pills.PillTimeSetterActivity;
import com.bald.uriah.baldphone.utils.DialogBuilder;
import com.bald.uriah.baldphone.utils.DialogFlags;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldPrefsUtils;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.CallManager;
import com.bald.uriah.baldphone.utils.KioskManager;
import com.bald.uriah.baldphone.utils.Const;
import com.bald.uriah.baldphone.utils.AppUtils;
import com.bald.uriah.baldphone.utils.PageDragHelper;
import com.bald.uriah.baldphone.utils.UpdatingUtil;
import com.bald.uriah.baldphone.views.BaldSwitch;
import com.bald.uriah.baldphone.views.BaldTitleBar;
import com.bald.uriah.baldphone.views.ModularRecyclerView;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.ACTION_VIEW;

/**
 * Settings Activity,
 * {{@link SettingsItem}} are generated programmatically because of the huge differences that lies between different settings.
 */
public class SettingsActivity extends BaldActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();
    public static final float[] FONT_SIZES = new float[]{0.8f, 0.9f, 1.0f, 1.1f, 1.3f, 1.5f, 1.7f};
    // parallel to FONT_SIZES — ends repeat: the 5 names predate the extended 7-stop scale
    private static final int[] FONT_SIZE_NAMES = new int[]{
            R.string.tiny, R.string.tiny, R.string.small, R.string.medium, R.string.large, R.string.huge, R.string.huge};
    private static final int VOLUME_MAX = 5;
    /** static so it survives leaving the activity; dies with the process — fine for a 5-min grace. */
    private static final long HELPER_PIN_GRACE = 5 * 60 * Const.SECOND;
    private static long helperUnlockedAt;

    private final Category
            mainCategory = new Category(R.string.settings, R.drawable.settings_on_button, -1),
            helperCategory = new Category(R.string.helper_settings, R.drawable.settings_on_button, 0) {
                @Override
                public void onClick(View v) {
                    if (System.currentTimeMillis() - helperUnlockedAt < HELPER_PIN_GRACE) {
                        helperUnlockedAt = System.currentTimeMillis();
                        newCategory(helperCategory);
                    } else {
                        showHelperPinDialog();
                    }
                }
            },
            screenCategory = new Category(R.string.screen_and_appearance, R.drawable.screen_on_button, 1),
            convenienceCategory = new Category(R.string.convenience_accessibility, R.drawable.accessibility_on_button, 2),
            connectionCategory = new Category(R.string.connection_settings, R.drawable.wifi_on_button, 3),
            systemCategory = new Category(R.string.system_settings, R.drawable.settings_on_button, 4);
    private Category currentCategory = mainCategory;

    private Ringtone ringtone;
    private Vibrator vibrator;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private RecyclerView recyclerView;
    private BaldPrefsUtils baldPrefsUtils;
    private BaldTitleBar baldTitleBar;
    private PageDragHelper pageDrag;
    private boolean layoutAnimationPlayed;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkPermissions(this, requiredPermissions()))
            return;
        setContentView(R.layout.activity_settings);
        sharedPreferences = getSharedPreferences(Const.BALD_PREFS, MODE_PRIVATE);
        baldPrefsUtils = BaldPrefsUtils.newInstance(this);
        editor = sharedPreferences.edit();

        vibrator = (sharedPreferences.getBoolean(BPrefs.VIBRATION_FEEDBACK_KEY, BPrefs.VIBRATION_FEEDBACK_DEFAULT_VALUE)) ? (Vibrator) getSystemService(VIBRATOR_SERVICE) : null;
        baldTitleBar = findViewById(R.id.bald_title_bar);
        baldTitleBar.setBackListener((v) -> onBackPressed());

        recyclerView = findViewById(R.id.child);

        populateSettingsList();
        recyclerView.setAdapter(new SettingsRecyclerViewAdapter());

        // root page drag toward the start edge = turn back to the home page
        final boolean rtl = getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        pageDrag = new PageDragHelper(this, findViewById(R.id.root), rtl ? -1 : 1,
                () -> {
                    finish();
                    overridePendingTransition(0, rtl ? R.anim.slide_out_left : R.anim.slide_out_right);
                }).carousel();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (pageDrag != null && pageDrag.dispatch(ev,
                currentCategory == mainCategory, !AppUtils.animationsDisabled(this)))
            return true;
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (baldPrefsUtils.hasChanged(this))
            recreate();
        else if (recyclerView != null) // refresh live subtitles (system brightness, font scale…)
            recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ringtone != null)
            ringtone.stop();
    }

    /** The kiosk PIN when one exists, else the helper PIN the helper set on first entry. */
    @Nullable
    private String helperPin() {
        final String kioskPin = KioskManager.savedPin(this);
        return kioskPin != null ? kioskPin : sharedPreferences.getString(BPrefs.HELPER_PIN_KEY, null);
    }

    private void showHelperPinDialog() {
        final int pinInputType = android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        final String saved = helperPin();
        // keeps DialogBuilder: DialogScreen has no EditText payload
        DialogBuilder.from(this)
                .setTitle(R.string.helper_settings)
                .setSubText(saved == null ? R.string.helper_pin_create_subtext : R.string.helper_pin_enter_subtext)
                .setInputType(pinInputType)
                .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL)
                .setPositiveButtonListener(params -> {
                    final String pin = String.valueOf(params[0]).trim();
                    if (saved == null) {
                        if (pin.length() < KioskManager.MIN_PIN_LENGTH || !android.text.TextUtils.isDigitsOnly(pin)) {
                            BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.kiosk_pin_too_short).show();
                            return false;
                        }
                        showHelperPinConfirmDialog(pin);
                        return true;
                    } else if (!pin.equals(saved)) {
                        BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.kiosk_wrong_pin).show();
                        return false;
                    }
                    helperUnlockedAt = System.currentTimeMillis();
                    newCategory(helperCategory);
                    return true;
                })
                .show();
    }

    /** Second step of kiosk PIN creation — a mistyped PIN could lock the device itself. */
    private void showKioskPinConfirmDialog(final String pin) {
        final int pinInputType = android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        DialogBuilder.from(this)
                .setTitle(R.string.kiosk_mode)
                .setSubText(R.string.helper_pin_repeat_subtext)
                .setInputType(pinInputType)
                .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL)
                .setPositiveButtonListener(params -> {
                    if (!pin.equals(String.valueOf(params[0]).trim())) {
                        BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.helper_pin_mismatch).show();
                        return false;
                    }
                    if (KioskManager.enable(this, pin))
                        BaldToast.from(this).setText(R.string.kiosk_enabled).show();
                    this.recreate();
                    return true;
                })
                .show();
    }

    /** Second step of PIN creation — a mistyped PIN would lock the helper out of their own settings. */
    private void showHelperPinConfirmDialog(final String pin) {
        final int pinInputType = android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        DialogBuilder.from(this)
                .setTitle(R.string.helper_settings)
                .setSubText(R.string.helper_pin_repeat_subtext)
                .setInputType(pinInputType)
                .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL)
                .setPositiveButtonListener(params -> {
                    if (!pin.equals(String.valueOf(params[0]).trim())) {
                        BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.helper_pin_mismatch).show();
                        return false;
                    }
                    editor.putString(BPrefs.HELPER_PIN_KEY, pin).apply();
                    helperUnlockedAt = System.currentTimeMillis();
                    newCategory(helperCategory);
                    return true;
                })
                .show();
    }

    private void showKioskDialog() {
        final int pinInputType = android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        if (!KioskManager.isDeviceOwner(this)) {
            DialogScreen.info(this, R.string.kiosk_mode, R.string.kiosk_not_device_owner_subtext);
        } else if (!KioskManager.isEnabled(this)) {
            // keeps DialogBuilder: DialogScreen has no EditText payload
            DialogBuilder.from(this)
                    .setTitle(R.string.kiosk_mode)
                    .setSubText(R.string.kiosk_enable_subtext)
                    .setInputType(pinInputType)
                    .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL)
                    .setPositiveButtonListener(params -> {
                        final String pin = String.valueOf(params[0]).trim();
                        if (pin.length() < KioskManager.MIN_PIN_LENGTH || !android.text.TextUtils.isDigitsOnly(pin)) {
                            BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.kiosk_pin_too_short).show();
                            return false;
                        }
                        showKioskPinConfirmDialog(pin);
                        return true;
                    })
                    .show();
        } else {
            // keeps DialogBuilder: DialogScreen has no EditText payload
            DialogBuilder.from(this)
                    .setTitle(R.string.kiosk_mode)
                    .setSubText(R.string.kiosk_disable_subtext)
                    .setInputType(pinInputType)
                    .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL)
                    .setPositiveButtonListener(params -> {
                        if (!KioskManager.checkPin(this, String.valueOf(params[0]).trim())) {
                            BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.kiosk_wrong_pin).show();
                            return false;
                        }
                        KioskManager.disable(this, this);
                        BaldToast.from(this).setText(R.string.kiosk_disabled).show();
                        this.recreate();
                        return true;
                    })
                    .show();
        }
    }

    private static final String[] LANGUAGE_CODES = {
            "", "ar", "bg", "ca", "cs", "da", "de", "el", "en", "es", "et", "eu", "fi", "fr",
            "ha", "hi", "hr", "hu", "id", "it", "iw", "ka", "ko", "nl", "pl", "pt", "pt-rBR",
            "ro", "ru", "sl", "sr", "sv", "tr", "zh"
    };

    private void showLanguageDialog() {
        final String current = sharedPreferences.getString(BPrefs.LANGUAGE_KEY, BPrefs.LANGUAGE_DEFAULT_VALUE);
        final CharSequence[] options = new CharSequence[LANGUAGE_CODES.length];
        int selected = 0;
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (i == 0) {
                options[i] = getText(R.string.system_default);
            } else {
                final java.util.Locale locale = java.util.Locale.forLanguageTag(LANGUAGE_CODES[i].replace("-r", "-"));
                options[i] = locale.getDisplayName(locale);
            }
            if (LANGUAGE_CODES[i].equals(current))
                selected = i;
        }
        DialogScreen.select(this, R.string.language_settings, selected,
                index -> {
                    editor.putString(BPrefs.LANGUAGE_KEY, LANGUAGE_CODES[index]).apply();
                    this.recreate();
                }, options);
    }

    private void populateSettingsList() {
        // User layer: the safe rows an elderly user can change directly.
        mainCategory.add(new Header(R.string.settings_for_user));
        setupAlarmVolume();
        mainCategory.add(new RunnableSettingsItem(R.string.brightness,
                v -> startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)), R.drawable.brightness_on_button) {
            @Override
            public CharSequence getSubtitle() {
                return brightnessSubtitle(SettingsActivity.this);
            }
        });
        mainCategory.add(new RunnableSettingsItem(R.string.font_size, v -> startActivity(new Intent(this, FontChangerActivity.class)), R.drawable.font_on_button) {
            @Override
            public CharSequence getSubtitle() {
                return fontSizeName(SettingsActivity.this);
            }
        });
        mainCategory.add(new SwitchSettingsItem(R.string.vibrate, R.drawable.vibration_on_background,
                BPrefs.VIBRATION_FEEDBACK_KEY, BPrefs.VIBRATION_FEEDBACK_DEFAULT_VALUE) {
            @Override
            protected void onPrefChanged(boolean checked) {
                vibrator = checked ? (Vibrator) getSystemService(VIBRATOR_SERVICE) : null;
            }
        });
        mainCategory.add(new RunnableSettingsItem(R.string.theme_settings,
                v -> DialogScreen.select(this, R.string.theme_settings,
                        Math.max(0, Math.min(sharedPreferences.getInt(BPrefs.THEME_KEY, BPrefs.THEME_DEFAULT_VALUE), BPrefs.Themes.THEMES.length - 1)) == BPrefs.Themes.DARK ? 1 : 0,
                        index -> {
                            editor.putInt(BPrefs.THEME_KEY, index == 1 ? BPrefs.Themes.DARK : BPrefs.Themes.LIGHT).apply();
                            this.recreate();
                        },
                        R.string.light, R.string.dark),
                R.drawable.brush_on_button
        ) {
            @Override
            public CharSequence getSubtitle() {
                return getText(sharedPreferences.getInt(BPrefs.THEME_KEY, BPrefs.THEME_DEFAULT_VALUE) == BPrefs.Themes.DARK
                        ? R.string.dark : R.string.light);
            }
        });
        mainCategory.add(new Header(R.string.settings_for_helper));
        mainCategory.add(helperCategory);

        // Helper layer: everything operational or dangerous stays behind the PIN gate,
        // grouped into four drill-in sub-screens (modern settings idiom).
        helperCategory.add(screenCategory);
        helperCategory.add(convenienceCategory);
        helperCategory.add(connectionCategory);
        helperCategory.add(systemCategory);

        screenCategory.add(new RunnableSettingsItem(R.string.edit_home_screen, v -> startActivity(new Intent(this, Page1EditorActivity.class)), R.drawable.edit_on_button));
        screenCategory.add(new SwitchSettingsItem(R.string.hide_bottom_row, R.drawable.keyboard_hide_on_button,
                BPrefs.HIDE_BOTTOM_ROW_KEY, BPrefs.HIDE_BOTTOM_ROW_DEFAULT_VALUE));
        screenCategory.add(
                new RunnableSettingsItem(R.string.apps_sort_method,
                        v -> DialogScreen.select(this, R.string.apps_sort_method,
                                sharedPreferences.getBoolean(BPrefs.APPS_ONE_GRID_KEY, BPrefs.APPS_ONE_GRID_DEFAULT_VALUE) ? 0 : 1,
                                index -> {
                                    editor.putBoolean(BPrefs.APPS_ONE_GRID_KEY, index == 0).apply();
                                    this.recreate();
                                },
                                R.string.one_grid, R.string.groups),
                        R.drawable.apps_on_button));
        screenCategory.add(new RunnableSettingsItem(R.string.status_bar_settings,
                v -> DialogScreen.select(this, R.string.status_bar_settings,
                        sharedPreferences.getInt(BPrefs.STATUS_BAR_KEY, BPrefs.STATUS_BAR_DEFAULT_VALUE),
                        index -> {
                            editor.putInt(BPrefs.STATUS_BAR_KEY, index).apply();
                            this.recreate();
                        },
                        R.string.nowhere, R.string.only_home_screen, R.string.everywhere),
                R.drawable.status_bar_on_button
        ));
        screenCategory.add(new SwitchSettingsItem(R.string.colorful, R.drawable.colors_on_button,
                BPrefs.COLORFUL_KEY, BPrefs.COLORFUL_DEFAULT_VALUE));
        screenCategory.add(new RunnableSettingsItem(R.string.language_settings, v -> showLanguageDialog(), R.drawable.translate_on_button) {
            @Override
            public CharSequence getSubtitle() {
                final String code = sharedPreferences.getString(BPrefs.LANGUAGE_KEY, BPrefs.LANGUAGE_DEFAULT_VALUE);
                if (code.isEmpty())
                    return getText(R.string.system_default);
                final java.util.Locale locale = java.util.Locale.forLanguageTag(code.replace("-r", "-"));
                return locale.getDisplayName(locale);
            }
        });

        convenienceCategory.add(new RunnableSettingsItem(R.string.accessibility_level, v -> startActivity(new Intent(this, AccessibilityLevelChangerActivity.class)), R.drawable.accessibility_on_button) {
            @Override
            public CharSequence getSubtitle() {
                // same flag math as TutorialFragment2: 2=high, 1=medium, 0=regular
                final int flag = sharedPreferences.getBoolean(BPrefs.LONG_PRESSES_KEY, BPrefs.LONG_PRESSES_DEFAULT_VALUE)
                        ? (sharedPreferences.getBoolean(BPrefs.LONG_PRESSES_SHORTER_KEY, BPrefs.LONG_PRESSES_SHORTER_DEFAULT_VALUE) ? 1 : 2) : 0;
                return getText(flag == 2 ? R.string.high_level : flag == 1 ? R.string.medium_level : R.string.regular_level);
            }
        });
        convenienceCategory.add(new SwitchSettingsItem(R.string.accidental_touches, R.drawable.blocked_on_button,
                BPrefs.USE_ACCIDENTAL_GUARD_KEY, BPrefs.USE_ACCIDENTAL_GUARD_DEFAULT_VALUE));
        convenienceCategory.add(
                new RunnableSettingsItem(R.string.strong_hand,
                        v -> DialogScreen.select(this, R.string.strong_hand,
                                sharedPreferences.getBoolean(BPrefs.RIGHT_HANDED_KEY, BPrefs.RIGHT_HANDED_DEFAULT_VALUE) ? 1 : 0,
                                index -> {
                                    editor.putBoolean(BPrefs.RIGHT_HANDED_KEY, index == 1).apply();
                                    this.recreate();
                                },
                                R.string.left_handed, R.string.right_handed),
                        R.drawable.hand_on_button
                )
        );
        convenienceCategory.add(new RunnableSettingsItem(R.string.set_keyboard, v -> startActivity(new Intent(this, KeyboardChangerActivity.class)), R.drawable.keyboard_on_button));
        convenienceCategory.add(new SwitchSettingsItem(R.string.dialer_sounds, R.drawable.phone_on_button,
                BPrefs.DIALER_SOUNDS_KEY, BPrefs.DIALER_SOUNDS_DEFAULT_VALUE));
        convenienceCategory.add(new SwitchSettingsItem(R.string.low_battery_alert, R.drawable.low_battery_alert_on_button,
                BPrefs.LOW_BATTERY_ALERT_KEY, BPrefs.LOW_BATTERY_ALERT_DEFAULT_VALUE));
        convenienceCategory.add(new RunnableSettingsItem(R.string.time_changer, v -> startActivity(new Intent(this, PillTimeSetterActivity.class)), R.drawable.pill));

        connectionCategory.add(new RunnableSettingsItem(R.string.wifi, v -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)), R.drawable.wifi_on_button));
        connectionCategory.add(new RunnableSettingsItem(R.string.bluetooth, v -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)), R.drawable.bluetooth_on_button));
        connectionCategory.add(new RunnableSettingsItem(R.string.airplane_mode, v -> startActivity(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)), R.drawable.airplane_mode_on_button));
        connectionCategory.add(new RunnableSettingsItem(R.string.location, v -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)), R.drawable.location_on_button));

        final NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        final NfcAdapter adapter = manager == null ? null : manager.getDefaultAdapter();
        if (adapter != null)
            connectionCategory.add(new RunnableSettingsItem(R.string.nfc, v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                    BaldToast.from(this).setText(R.string.setting_does_not_exist).setType(BaldToast.TYPE_ERROR).show();

                }
            }, R.drawable.nfc_on_button));
        if (hasMultipleSims())
            connectionCategory.add(new RunnableSettingsItem(R.string.dual_sim,
                    v -> DialogScreen.select(this, R.string.dual_sim,
                            sharedPreferences.getBoolean(BPrefs.DUAL_SIM_KEY, BPrefs.DUAL_SIM_DEFAULT_VALUE) ? 0 : 1,
                            index -> {
                                editor.putBoolean(BPrefs.DUAL_SIM_KEY, index == 0).apply();
                                this.recreate();
                            },
                            R.string.show_options, R.string.use_default),
                    R.drawable.sim_on_button
            ));

        if (!checkPermissions(this, -1))
            systemCategory.add(
                    new RunnableSettingsItem(R.string.grant_all_permissions,
                            v -> startActivity(new Intent(this, PermissionActivity.class)),
                            R.drawable.grant_all_permissions_on_button)
            );
        systemCategory.add(
                new RunnableSettingsItem(R.string.set_home_screen,
                        v -> FakeLauncherActivity.resetPreferredLauncherAndOpenChooser(this)
                        , R.drawable.home_on_button)
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            systemCategory.add(
                    new RunnableSettingsItem(R.string.use_granphone_for_calls,
                            v -> CallManager.requestDefaultDialer(this)
                            , R.drawable.phone_on_button)
            );
        systemCategory.add(new RunnableSettingsItem(R.string.kiosk_mode, v -> showKioskDialog(), R.drawable.blocked_on_button));
        if (KioskManager.isDeviceOwner(this) && !KioskManager.isEnabled(this))
            systemCategory.add(new RunnableSettingsItem(R.string.remove_device_owner, v -> DialogScreen.confirm(this,
                    R.string.remove_device_owner,
                    R.string.remove_device_owner_subtext,
                    R.string.delete,
                    () -> {
                        KioskManager.removeDeviceOwner(this);
                        this.recreate();
                    }), R.drawable.blocked_on_button));
        systemCategory.add(new RunnableSettingsItem(R.string.advanced_options, v -> startActivity(new Intent(Settings.ACTION_SETTINGS)), R.drawable.settings_on_button));
        systemCategory.add(
                new BDBSettingsItem(R.string.device_memory,
                        // keeps DialogBuilder: DialogScreen can't carry the storage/RAM extraView
                        DialogBuilder.from(this)
                                .addFlag(DialogFlags.FLAG_OK)
                                .setTitle(R.string.device_memory)
                                .setSubText(R.string.device_memory_subtext)
                                .setPositiveButtonListener(params -> true)
                                .setExtraView(buildMemoryStatusView())
                        , R.drawable.storage_on_button)
        );
        systemCategory.add(
                new RunnableSettingsItem(R.string.technical_information,
                        v -> startActivity(new Intent(this, TechnicalInfoActivity.class)),
                        R.drawable.tech_info_on_button)
        );
        if (BuildConfig.FLAVOR.equals("baldUpdates"))
            systemCategory.add(
                    new RunnableSettingsItem(R.string.check_for_updates,
                            v -> UpdatingUtil.checkForUpdates(this, true),
                            R.drawable.updates_on_button)
            );
    }

    /** pref 0..VOLUME_MAX -> real % of alarm stream (AlarmScreenActivity uses (v+6)/10, Android clamps at max). */
    private static String volumePercent(int v) {
        return Math.min((v + 6) * 10, 100) + "%";
    }

    /** true when the device provably has 2+ active SIMs; no permission or API<22 = treated as single-SIM. */
    private boolean hasMultipleSims() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
            return false;
        if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED)
            return false;
        try {
            final android.telephony.SubscriptionManager sm = (android.telephony.SubscriptionManager)
                    getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            final List<android.telephony.SubscriptionInfo> list = sm == null ? null : sm.getActiveSubscriptionInfoList();
            return list != null && list.size() > 1;
        } catch (SecurityException e) {
            return false;
        }
    }

    private boolean goBack() {
        if (currentCategory == mainCategory)
            return false;
        if (vibrator != null)
            vibrator.vibrate(Const.vibetime);
        final Category parent = currentCategory.parent;
        newCategory(parent == null ? mainCategory : parent, false);
        return true;
    }

    private void newCategory(Category category) {
        newCategory(category, true);
    }

    private void newCategory(Category category, boolean forward) {
        this.currentCategory = category;
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scrollToPosition(0);
        // the fall-down stagger is a first-entry flourish — not per drill-in/out
        if (!layoutAnimationPlayed && !AppUtils.animationsDisabled(this)) {
            layoutAnimationPlayed = true;
            recyclerView.scheduleLayoutAnimation();
        }
        baldTitleBar.setTitle(category.textResId);
        recyclerView.animate().cancel();
        if (AppUtils.animationsDisabled(this)) {
            recyclerView.setTranslationX(0);
            return;
        }
        final int dir = getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? -1 : 1;
        recyclerView.setTranslationX(forward ? recyclerView.getWidth() * dir : -recyclerView.getWidth() * 0.3f * dir);
        recyclerView.animate().translationX(0).setDuration(250).start();
    }

    private View buildMemoryStatusView() {
        final View view = LayoutInflater.from(this).inflate(R.layout.storage_status, null, false);

        final StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        final long storageTotal = statFs.getTotalBytes();
        final long storageFree = statFs.getAvailableBytes();
        ((ProgressBar) view.findViewById(R.id.pb_storage))
                .setProgress((int) ((storageTotal - storageFree) * 100 / storageTotal));
        ((TextView) view.findViewById(R.id.tv_storage))
                .setText(getString(R.string.free_of_total,
                        Formatter.formatFileSize(this, storageFree),
                        Formatter.formatFileSize(this, storageTotal)));

        final ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(memoryInfo);
        ((ProgressBar) view.findViewById(R.id.pb_ram))
                .setProgress((int) ((memoryInfo.totalMem - memoryInfo.availMem) * 100 / memoryInfo.totalMem));
        ((TextView) view.findViewById(R.id.tv_ram))
                .setText(getString(R.string.free_of_total,
                        Formatter.formatFileSize(this, memoryInfo.availMem),
                        Formatter.formatFileSize(this, memoryInfo.totalMem)));
        return view;
    }

    private void setupAlarmVolume() {
        final SharedPreferences prefs = BPrefs.get(this);
        final View control = LayoutInflater.from(this).inflate(R.layout.volume_buttons, null, false);
        final TextView tvVolume = control.findViewById(R.id.tv_volume);
        final Handler handler = new Handler();
        final Runnable closeRingtone = () -> {
            final Ringtone r = ringtone;
            if (r == null)
                return;
            r.stop();
            ringtone = null;
        };
        final Runnable refresh = () -> tvVolume.setText(
                volumePercent(prefs.getInt(BPrefs.ALARM_VOLUME_KEY, BPrefs.ALARM_VOLUME_DEFAULT_VALUE)));
        refresh.run();
        final View.OnClickListener step = v -> {
            final int volume = prefs.getInt(BPrefs.ALARM_VOLUME_KEY, BPrefs.ALARM_VOLUME_DEFAULT_VALUE)
                    + (v.getId() == R.id.bt_volume_up ? 1 : -1);
            if (volume < 0 || volume > VOLUME_MAX)
                return;
            prefs.edit().putInt(BPrefs.ALARM_VOLUME_KEY, volume).apply();
            handler.removeCallbacks(closeRingtone);
            closeRingtone.run();
            ringtone = AlarmScreenActivity.getRingtone(this);
            if (ringtone != null)
                ringtone.play();
            handler.postDelayed(closeRingtone, 4 * Const.SECOND);
            refresh.run();
        };
        control.findViewById(R.id.bt_volume_down).setOnClickListener(step);
        control.findViewById(R.id.bt_volume_up).setOnClickListener(step);

        // keeps DialogBuilder: DialogScreen can't carry the volume controls extraView
        final SettingsItem alarmVolumeSettingsItem = new BDBSettingsItem(R.string.alarm_volume,
                DialogBuilder.from(this)
                        .addFlag(DialogFlags.FLAG_OK)
                        .setTitle(R.string.alarm_volume)
                        .setSubText(R.string.alarm_volume_subtext)
                        .setPositiveButtonListener(params -> true)
                        .setExtraView(control), R.drawable.clock_on_background

        ) {
            @Override
            public CharSequence getSubtitle() {
                return volumePercent(prefs.getInt(BPrefs.ALARM_VOLUME_KEY, BPrefs.ALARM_VOLUME_DEFAULT_VALUE));
            }
        };
        mainCategory.add(alarmVolumeSettingsItem);

    }

    public static CharSequence fontSizeName(Context context) {
        float scale;
        try {
            scale = Settings.System.getFloat(context.getContentResolver(), Settings.System.FONT_SCALE);
        } catch (Settings.SettingNotFoundException e) {
            scale = 1.0f;
        }
        int nearest = 0;
        for (int i = 1; i < FONT_SIZES.length; i++)
            if (Math.abs(FONT_SIZES[i] - scale) < Math.abs(FONT_SIZES[nearest] - scale))
                nearest = i;
        return context.getText(FONT_SIZE_NAMES[nearest]);
    }

    public static CharSequence brightnessSubtitle(Context context) {
        // int 0-255 on most devices, float 0-1 where SCREEN_BRIGHTNESS_FLOAT wrote it
        final float b = Settings.System.getFloat(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1f);
        if (b < 0)
            return null;
        final float brightness = b <= 1f ? b * 255f : b;
        return Math.round(linearToSliderPercent(context, brightness)) + "%";
    }

    // SettingsLib BrightnessUtils.convertLinearToGamma — the hybrid-log-gamma curve the
    // system slider/notification shade uses, so our % reads 1:1 with them.
    private static float linearToSliderPercent(Context context, float brightness) {
        final float min = screenBrightnessBound(context, "getMinimumScreenBrightnessSetting", 10f);
        final float max = screenBrightnessBound(context, "getMaximumScreenBrightnessSetting", 255f);
        final float n = Math.max(0f, (brightness - min) / (max - min)) * 12f;
        final float ret = n <= 1f
                ? (float) Math.sqrt(n) * 0.5f
                : 0.17883277f * (float) Math.log(n - 0.28466892f) + 0.55991073f;
        return Math.max(0f, Math.min(1f, ret)) * 100f;
    }

    private static float screenBrightnessBound(Context context, String methodName, float fallback) {
        // getMinimum/MaximumScreenBrightnessSetting are @SystemApi below API 33.
        try {
            final android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(POWER_SERVICE);
            return (Integer) pm.getClass().getMethod(methodName).invoke(pm);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void onBackPressed() {
        if (!goBack())
            super.onBackPressed();
    }

    public class SettingsRecyclerViewAdapter extends ModularRecyclerView.ModularAdapter<SettingsRecyclerViewAdapter.ViewHolder> {
        final LayoutInflater layoutInflater;

        SettingsRecyclerViewAdapter() {
            layoutInflater = (LayoutInflater) SettingsActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getItemViewType(int position) {
            return currentCategory.get(position) instanceof Header ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(layoutInflater.inflate(
                    viewType == VIEW_TYPE_HEADER ? R.layout.settings_header : R.layout.settings_item, parent, false));
        }

        @Override
        public int getItemCount() {
            return currentCategory.size();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            holder.update(currentCategory.get(position));
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            TextView subtitleView;
            ImageView setting_icon;
            ViewGroup trailing;

            public ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.tv_setting_name);
                subtitleView = itemView.findViewById(R.id.tv_setting_subtitle);
                setting_icon = itemView.findViewById(R.id.setting_icon);
                trailing = itemView.findViewById(R.id.setting_trailing);
            }

            public void update(SettingsItem settingsItem) {
                if (settingsItem instanceof Header) {
                    ((TextView) this.itemView).setText(settingsItem.textResId);
                    return;
                }
                this.textView.setText(settingsItem.textResId);
                this.setting_icon.setImageResource(settingsItem.drawableResId);
                final CharSequence subtitle = settingsItem.getSubtitle();
                this.subtitleView.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
                if (subtitle != null)
                    this.subtitleView.setText(subtitle);
                this.trailing.removeAllViews();
                settingsItem.bindTrailing(this.trailing);
                this.itemView.setOnClickListener(settingsItem);
            }
        }
    }

    //OOP
    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_HEADER = 1;

    public abstract class SettingsItem implements View.OnClickListener {
        public final @StringRes
        int textResId;
        public final @DrawableRes
        int drawableResId;

        protected SettingsItem(@StringRes int textResId, @DrawableRes int drawableResId) {
            this.textResId = textResId;
            this.drawableResId = drawableResId;
        }

        /** Current value shown under the title (e.g. "Large"); null = no subtitle. */
        @Nullable
        public CharSequence getSubtitle() {
            return null;
        }

        /** Add a trailing widget (e.g. BaldSwitch) into the row's end slot. Default: none. */
        public void bindTrailing(@NonNull ViewGroup container) {
        }

    }

    public class BDBSettingsItem extends SettingsItem {
        private final DialogBuilder bdb;

        BDBSettingsItem(@StringRes int textResId, DialogBuilder bdb, @DrawableRes int drawableResId) {
            super(textResId, drawableResId);
            this.bdb = bdb;
        }

        @Override
        public void onClick(View v) {
            bdb.show();
        }
    }

    /** Row with an inline {@link BaldSwitch} in the trailing slot; row tap toggles it. */
    public class SwitchSettingsItem extends SettingsItem {
        private final String key;
        private final boolean defaultValue;
        private BaldSwitch baldSwitch;

        SwitchSettingsItem(@StringRes int textResId, @DrawableRes int drawableResId, String key, boolean defaultValue) {
            super(textResId, drawableResId);
            this.key = key;
            this.defaultValue = defaultValue;
        }

        /** Hook for side effects beyond the pref write (e.g. updating the vibrator field). */
        protected void onPrefChanged(boolean checked) {
        }

        private void apply(boolean checked) {
            editor.putBoolean(key, checked).apply();
            onPrefChanged(checked);
        }

        @Override
        public void onClick(View v) {
            if (baldSwitch == null)
                return;
            // BaldSwitch.setChecked doesn't fire the change listener — apply manually.
            baldSwitch.setChecked(!baldSwitch.isChecked());
            apply(baldSwitch.isChecked());
        }

        @Override
        public void bindTrailing(@NonNull ViewGroup container) {
            final Context ctx = container.getContext();
            baldSwitch = new BaldSwitch(ctx);
            baldSwitch.setLayoutParams(new ViewGroup.LayoutParams(
                    ctx.getResources().getDimensionPixelSize(R.dimen.switch_width),
                    ctx.getResources().getDimensionPixelSize(R.dimen.switch_height)));
            baldSwitch.setChecked(sharedPreferences.getBoolean(key, defaultValue));
            baldSwitch.setOnChangeListener(this::apply);
            container.addView(baldSwitch);
        }
    }

    public class RunnableSettingsItem extends SettingsItem {
        private final View.OnClickListener onClickListener;

        RunnableSettingsItem(@StringRes int textResId, View.OnClickListener onClickListener, @DrawableRes int drawableResId) {
            super(textResId, drawableResId);
            this.onClickListener = onClickListener;
        }

        @Override
        public void onClick(View v) {
            try {
                onClickListener.onClick(v);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
                BaldToast.from(v.getContext()).setText(R.string.setting_does_not_exist).setType(BaldToast.TYPE_ERROR).show();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
                BaldToast.from(v.getContext()).setText(R.string.an_error_has_occurred).setType(BaldToast.TYPE_ERROR).show();
                BaldToast.from(v.getContext()).setText(e.getMessage()).setType(BaldToast.TYPE_ERROR).show();
            }
        }
    }

    /** Non-clickable section label («Для пользователя» / «Для помощника»). */
    public class Header extends SettingsItem {
        Header(@StringRes int textResId) {
            super(textResId, 0);
        }

        @Override
        public void onClick(View v) {
            // decorative
        }
    }

    public class Category extends SettingsItem {
        public final List<SettingsItem> settingsItemList;
        public final int id;
        private Category parent;

        public Category(int stringRes, int drawableRes, int id) {
            super(stringRes, drawableRes);
            this.settingsItemList = new ArrayList<>();
            this.id = id;
        }

        public void add(SettingsItem settingsItem) {
            if (settingsItem instanceof Category)
                ((Category) settingsItem).parent = this;
            settingsItemList.add(settingsItem);
        }

        public SettingsItem get(int index) {
            return settingsItemList.get(index);
        }

        public int size() {
            return settingsItemList.size();
        }

        @Override
        public void onClick(View v) {
            newCategory(this);
        }

        @Override
        public void bindTrailing(@NonNull ViewGroup container) {
            // chevron marks drill-in rows (same idiom as system Settings)
            final Context ctx = container.getContext();
            final ImageView chevron = new ImageView(ctx);
            final int size = ctx.getResources().getDimensionPixelSize(R.dimen.icon_sm);
            chevron.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            chevron.setImageResource(R.drawable.chevron_end_on_button);
            chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            container.addView(chevron);
        }
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_WRITE_SETTINGS;
    }
}