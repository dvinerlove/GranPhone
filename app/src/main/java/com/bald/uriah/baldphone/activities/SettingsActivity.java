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
import android.content.res.Resources;
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
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
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
import com.bald.uriah.baldphone.utils.UpdatingUtil;
import com.bald.uriah.baldphone.views.BaldTitleBar;
import com.bald.uriah.baldphone.views.ModularRecyclerView;
import androidx.recyclerview.widget.DividerItemDecoration;

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
    public static final String SAVEABLE_HISTORY_KEY = "SAVEABLE_HISTORY_KEY";

    private final Category
            mainCategory = new Category(R.string.settings, R.drawable.settings_on_button, -1),
            connectionCategory = new Category(R.string.connection_settings, R.drawable.wifi_on_button, 0),
            accessibilityCategory = new Category(R.string.accessibility_settings, R.drawable.accessibility_on_button, 1),
            displayCategory = new Category(R.string.display_settings, R.drawable.screen_on_button, 2),
            personalizationCategory = new Category(R.string.personalization_settings, R.drawable.brush_on_button, 3);
    private final Category[] categoriesArray = new Category[]{mainCategory, connectionCategory, accessibilityCategory, displayCategory, displayCategory, personalizationCategory};
    private final SparseArray<Category> categorySparseArray;
    private Category currentCategory = mainCategory;

    private Ringtone ringtone;
    private Vibrator vibrator;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private RecyclerView recyclerView;
    private BaldPrefsUtils baldPrefsUtils;
    private BaldTitleBar baldTitleBar;

    {
        categorySparseArray = new SparseArray<>();
        for (final Category category : categoriesArray) {
            categorySparseArray.append(category.id, category);
        }
    }

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
        baldTitleBar.getBt_back().setOnClickListener((v) -> onBackPressed());

        recyclerView = findViewById(R.id.child);

        final TypedValue typedValue = new TypedValue();
        final Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.bald_stroke_color, typedValue, true);
        final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getDrawable(R.drawable.settings_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        populateSettingsList();
        recyclerView.setAdapter(new SettingsRecyclerViewAdapter());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (baldPrefsUtils.hasChanged(this))
            recreate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ringtone != null)
            ringtone.stop();
    }

    private void showKioskDialog() {
        final int pinInputType = android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        if (!KioskManager.isDeviceOwner(this)) {
            DialogScreen.info(this, R.string.kiosk_mode, R.string.kiosk_not_device_owner_subtext);
        } else if (!KioskManager.isEnabled(this)) {
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
                        if (KioskManager.enable(this, pin))
                            BaldToast.from(this).setText(R.string.kiosk_enabled).show();
                        this.recreate();
                        return true;
                    })
                    .show();
        } else {
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVEABLE_HISTORY_KEY, currentCategory.id);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int id = savedInstanceState.getInt(SAVEABLE_HISTORY_KEY);
        if (id != -1)
            newCategory(categorySparseArray.get(id));
    }

    private void populateSettingsList() {
        mainCategory.add(connectionCategory);
        mainCategory.add(accessibilityCategory);
        mainCategory.add(displayCategory);
        mainCategory.add(personalizationCategory);

        mainCategory.add(
                new RunnableSettingsItem(R.string.set_home_screen,
                        v -> FakeLauncherActivity.resetPreferredLauncherAndOpenChooser(this)
                        , R.drawable.home_on_button)
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mainCategory.add(
                    new RunnableSettingsItem(R.string.use_granphone_for_calls,
                            v -> CallManager.requestDefaultDialer(this)
                            , R.drawable.phone_on_button)
            );

        mainCategory.add(new RunnableSettingsItem(R.string.kiosk_mode, v -> showKioskDialog(), R.drawable.blocked_on_button));
        if (KioskManager.isDeviceOwner(this) && !KioskManager.isEnabled(this))
            mainCategory.add(new RunnableSettingsItem(R.string.remove_device_owner, v -> DialogBuilder.from(this)
                    .setTitle(R.string.remove_device_owner)
                    .setSubText(R.string.remove_device_owner_subtext)
                    .addFlag(DialogFlags.FLAG_YES | DialogFlags.FLAG_NO)
                    .setPositiveButtonListener(params -> {
                        KioskManager.removeDeviceOwner(this);
                        this.recreate();
                        return true;
                    })
                    .show(), R.drawable.blocked_on_button));

        mainCategory.add(new RunnableSettingsItem(R.string.advanced_options, v -> startActivity(new Intent(Settings.ACTION_SETTINGS)), R.drawable.settings_on_button));

        final SettingsItem keyboard = new RunnableSettingsItem(R.string.set_keyboard, v -> startActivity(new Intent(this, KeyboardChangerActivity.class)), R.drawable.keyboard_on_button);
        accessibilityCategory.add(keyboard);

        personalizationCategory.add(new RunnableSettingsItem(R.string.language_settings, v -> startActivity(new Intent(Settings.ACTION_LOCALE_SETTINGS)), R.drawable.translate_on_button));

        personalizationCategory.add(new RunnableSettingsItem(R.string.time_changer, v -> startActivity(new Intent(this, PillTimeSetterActivity.class)), R.drawable.pill));
        personalizationCategory.add(new RunnableSettingsItem(R.string.edit_home_screen, v -> startActivity(new Intent(this, Page1EditorActivity.class)), R.drawable.edit_on_button));

        accessibilityCategory.add(new RunnableSettingsItem(R.string.accessibility_level, v -> startActivity(new Intent(this, AccessibilityLevelChangerActivity.class)), R.drawable.accessibility_on_button));
        accessibilityCategory.add(
                new BDBSettingsItem(R.string.accidental_touches, DialogBuilder.from(this)
                        .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL).setTitle(R.string.accidental_touches)
                        .setSubText(R.string.accidental_touches_settings_subtext)
                        .setOptions(R.string.on, R.string.off)
                        .setPositiveButtonListener(params -> {
                            editor.putBoolean(BPrefs.USE_ACCIDENTAL_GUARD_KEY, params[0].equals(0)).apply();
                            this.recreate();
                            return true;
                        })
                        .setOptionsStartingIndex(() -> sharedPreferences.getBoolean(BPrefs.USE_ACCIDENTAL_GUARD_KEY, BPrefs.USE_ACCIDENTAL_GUARD_DEFAULT_VALUE) ? 0 : 1), R.drawable.blocked_on_button));
        accessibilityCategory.add(
                new BDBSettingsItem(R.string.strong_hand,
                        DialogBuilder.from(this)
                                .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL)
                                .setTitle(R.string.strong_hand)
                                .setSubText(R.string.strong_hand_subtext)
                                .setOptions(R.string.left_handed, R.string.right_handed)
                                .setPositiveButtonListener(params -> {
                                    editor.putBoolean(BPrefs.RIGHT_HANDED_KEY, params[0].equals(1)).apply();
                                    this.recreate();
                                    return true;
                                })
                                .setOptionsStartingIndex(() -> sharedPreferences.getBoolean(BPrefs.RIGHT_HANDED_KEY, BPrefs.RIGHT_HANDED_DEFAULT_VALUE) ? 1 : 0), R.drawable.hand_on_button
                )
        );
        final SettingsItem themeSettingsItem = new BDBSettingsItem(R.string.theme_settings,
                DialogBuilder.from(this)
                        .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL)
                        .setTitle(R.string.theme_settings)
                        .setSubText(R.string.theme_settings_subtext)
                        .setOptions(R.string.light, R.string.adaptive, R.string.dark)
                        .setPositiveButtonListener(params -> {
                            editor.putInt(BPrefs.THEME_KEY, (Integer) params[0]).apply();
                            this.recreate();
                            return true;
                        })
                        .setOptionsStartingIndex(() -> sharedPreferences.getInt(BPrefs.THEME_KEY, BPrefs.THEME_DEFAULT_VALUE)), R.drawable.brush_on_button

        );
        displayCategory.add(themeSettingsItem);

        personalizationCategory.add(new BDBSettingsItem(R.string.status_bar_settings,
                DialogBuilder.from(this)
                        .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL)
                        .setTitle(R.string.status_bar_settings)
                        .setSubText(R.string.status_bar_settings_subtext)
                        .setOptions(R.string.nowhere, R.string.only_home_screen, R.string.everywhere)
                        .setPositiveButtonListener(params -> {
                            editor.putInt(BPrefs.STATUS_BAR_KEY, (Integer) params[0]).apply();
                            this.recreate();
                            return true;
                        })
                        .setOptionsStartingIndex(() -> sharedPreferences.getInt(BPrefs.STATUS_BAR_KEY, BPrefs.STATUS_BAR_DEFAULT_VALUE)),
                R.drawable.status_bar_on_button
        ));

        personalizationCategory.add(new BDBSettingsItem(R.string.dialer_sounds,
                DialogBuilder.from(this)
                        .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL)
                        .setTitle(R.string.dialer_sounds)
                        .setSubText(R.string.dialer_sounds_subtext)
                        .setOptions(R.string.yes, R.string.no)
                        .setPositiveButtonListener(params -> {
                            editor.putBoolean(BPrefs.DIALER_SOUNDS_KEY, (Integer) params[0] == 0).apply();
                            this.recreate();
                            return true;
                        })
                        .setOptionsStartingIndex(() -> sharedPreferences.getBoolean(BPrefs.DIALER_SOUNDS_KEY, BPrefs.DIALER_SOUNDS_DEFAULT_VALUE) ? 0 : 1),
                R.drawable.phone_on_button
        ));
        personalizationCategory.add(new BDBSettingsItem(R.string.dual_sim,
                DialogBuilder.from(this)
                        .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL)
                        .setTitle(R.string.dual_sim)
                        .setSubText(R.string.dual_sim_subtext)
                        .setOptions(R.string.show_options, R.string.use_default)
                        .setPositiveButtonListener(params -> {
                            editor.putBoolean(BPrefs.DUAL_SIM_KEY, (Integer) params[0] == 0).apply();
                            this.recreate();
                            return true;
                        })
                        .setOptionsStartingIndex(() -> sharedPreferences.getBoolean(BPrefs.DUAL_SIM_KEY, BPrefs.DUAL_SIM_DEFAULT_VALUE) ? 0 : 1),
                R.drawable.sim_on_button
        ));
        personalizationCategory.add(
                new BDBSettingsItem(R.string.apps_sort_method,
                        DialogBuilder.from(this)
                                .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL).setTitle(R.string.apps_sort_method)
                                .setSubText(R.string.apps_sort_method_subtext)
                                .setOptions(R.string.one_grid, R.string.groups)
                                .setPositiveButtonListener(params -> {
                                    editor.putBoolean(BPrefs.APPS_ONE_GRID_KEY, params[0].equals(0)).apply();
                                    this.recreate();
                                    return true;
                                })
                                .setOptionsStartingIndex(() -> sharedPreferences.getBoolean(BPrefs.APPS_ONE_GRID_KEY, BPrefs.APPS_ONE_GRID_DEFAULT_VALUE) ? 0 : 1),
                        R.drawable.apps_on_button));

        personalizationCategory.add(
                new BDBSettingsItem(R.string.colorful,
                        DialogBuilder.from(this)
                                .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL).setTitle(R.string.apps_sort_method)
                                .setSubText(R.string.colorful_subtext)
                                .setOptions(R.string.yes, R.string.no)
                                .setPositiveButtonListener(params -> {
                                    editor.putBoolean(BPrefs.COLORFUL_KEY, params[0].equals(0)).apply();
                                    this.recreate();
                                    return true;
                                })
                                .setOptionsStartingIndex(() -> sharedPreferences.getBoolean(BPrefs.COLORFUL_KEY, BPrefs.COLORFUL_DEFAULT_VALUE) ? 0 : 1),
                        R.drawable.colors_on_button));

        personalizationCategory.add(new BDBSettingsItem(R.string.low_battery_alert,
                DialogBuilder.from(this)
                        .addFlag(DialogFlags.FLAG_OK | DialogFlags.FLAG_CANCEL)
                        .setTitle(R.string.low_battery_alert)
                        .setSubText(R.string.low_battery_alert_subtext)
                        .setOptions(R.string.yes, R.string.no)
                        .setPositiveButtonListener(params -> {
                            editor.putBoolean(BPrefs.LOW_BATTERY_ALERT_KEY, (Integer) params[0] == 0).apply();
                            this.recreate();
                            return true;
                        })
                        .setOptionsStartingIndex(() -> sharedPreferences.getBoolean(BPrefs.LOW_BATTERY_ALERT_KEY, BPrefs.LOW_BATTERY_ALERT_DEFAULT_VALUE) ? 0 : 1),
                R.drawable.low_battery_alert_on_button
        ));
        accessibilityCategory.add(new RunnableSettingsItem(R.string.font_size, v -> startActivity(new Intent(this, FontChangerActivity.class)), R.drawable.font_on_button));

        displayCategory.add(new RunnableSettingsItem(R.string.brightness,
                v -> startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)), R.drawable.brightness_on_button));
        setupAlarmVolume();

        //connections
        connectionCategory.add(new RunnableSettingsItem(R.string.airplane_mode, v -> startActivity(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)), R.drawable.airplane_mode_on_button));
        connectionCategory.add(new RunnableSettingsItem(R.string.wifi, v -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)), R.drawable.wifi_on_button));
        connectionCategory.add(new RunnableSettingsItem(R.string.bluetooth, v -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)), R.drawable.bluetooth_on_button));

        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager == null ? null : manager.getDefaultAdapter();
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
        connectionCategory.add(new RunnableSettingsItem(R.string.location, v -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)), R.drawable.location_on_button));

        mainCategory.add(
                new BDBSettingsItem(R.string.device_memory,
                        DialogBuilder.from(this)
                                .addFlag(DialogFlags.FLAG_OK)
                                .setTitle(R.string.device_memory)
                                .setSubText(R.string.device_memory_subtext)
                                .setPositiveButtonListener(params -> true)
                                .setExtraView(buildMemoryStatusView())
                        , R.drawable.tech_info_on_button)
        );
        mainCategory.add(
                new RunnableSettingsItem(R.string.technical_information,
                        v -> startActivity(new Intent(this, TechnicalInfoActivity.class)),
                        R.drawable.tech_info_on_button)
        );
        if (!checkPermissions(this, -1))
            mainCategory.add(
                    new RunnableSettingsItem(R.string.grant_all_permissions,
                            v -> startActivity(new Intent(this, PermissionActivity.class)),
                            R.drawable.grant_all_permissions_on_button)
            );
        if (BuildConfig.FLAVOR.equals("baldUpdates"))
            mainCategory.add(
                    new RunnableSettingsItem(R.string.check_for_updates,
                            v -> UpdatingUtil.checkForUpdates(this, true),
                            R.drawable.updates_on_button)
            );
    }

    /**
     * @return true if succeeded
     */
    private boolean goBack() {
        if (currentCategory == mainCategory)
            return false;
        if (vibrator != null)
            vibrator.vibrate(Const.vibetime);

        newCategory(mainCategory);
        return true;
    }

    private void newCategory(Category category) {
        this.currentCategory = category;
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
        baldTitleBar.setTitle(category.textResId);

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
        final SeekBar volumeSeekBar = (SeekBar) LayoutInflater.from(this).inflate(R.layout.volume_seek_bar, null, false);
        final SharedPreferences sharedPreferences = BPrefs.get(this);
        int volume = sharedPreferences.getInt(BPrefs.ALARM_VOLUME_KEY, BPrefs.ALARM_VOLUME_DEFAULT_VALUE);
        volumeSeekBar.setProgress(volume);
        final Handler handler = new Handler();
        final Runnable closeRingtone = () -> {
            final Ringtone r = ringtone;
            if (r == null)
                return;
            r.stop();
            ringtone = null;
        };

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                handler.removeCallbacks(closeRingtone);
                closeRingtone.run();

                sharedPreferences.edit().putInt(BPrefs.ALARM_VOLUME_KEY, progress).apply();
                ringtone = AlarmScreenActivity.getRingtone(SettingsActivity.this);
                if (ringtone != null)
                    ringtone.play();
                handler.postDelayed(closeRingtone, 4 * Const.SECOND);

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final SettingsItem alarmVolumeSettingsItem = new BDBSettingsItem(R.string.alarm_volume,
                DialogBuilder.from(this)
                        .addFlag(DialogFlags.FLAG_OK)
                        .setTitle(R.string.alarm_volume)
                        .setSubText(R.string.alarm_volume_subtext)
                        .setPositiveButtonListener(params -> true)
                        .setExtraView(volumeSeekBar), R.drawable.clock_on_background

        );
        personalizationCategory.add(alarmVolumeSettingsItem);

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

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(layoutInflater.inflate(R.layout.settings_item, parent, false));
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
            ImageView setting_icon;

            public ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.tv_setting_name);
                setting_icon = itemView.findViewById(R.id.setting_icon);
            }

            public void update(SettingsItem settingsItem) {
                this.textView.setText(settingsItem.textResId);
                this.setting_icon.setImageResource(settingsItem.drawableResId);
                this.itemView.setOnClickListener(settingsItem);
            }
        }
    }

    //OOP
    public abstract class SettingsItem implements View.OnClickListener {
        public final @StringRes
        int textResId;
        public final @DrawableRes
        int drawableResId;

        protected SettingsItem(@StringRes int textResId, @DrawableRes int drawableResId) {
            this.textResId = textResId;
            this.drawableResId = drawableResId;
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

    public class Category extends SettingsItem {
        public final List<SettingsItem> settingsItemList;
        public final int id;

        public Category(int stringRes, int drawableRes, int id) {
            super(stringRes, drawableRes);
            this.settingsItemList = new ArrayList<>();
            this.id = id;
        }

        public void add(SettingsItem settingsItem) {
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
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_WRITE_SETTINGS;
    }
}