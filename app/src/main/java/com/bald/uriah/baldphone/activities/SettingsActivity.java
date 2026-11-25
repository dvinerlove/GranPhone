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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.alarms.AlarmScreenActivity;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.Const;
import com.bald.uriah.baldphone.utils.DialogScreen;

/**
 * The root settings screen — the safe rows an elderly user can change
 * directly. Drill-in screens are real activities ({@link SettingsPageActivity}
 * subclasses); the task stack IS the back stack.
 */
public class SettingsActivity extends SettingsPageActivity {
    public static final float[] FONT_SIZES = new float[]{0.8f, 0.9f, 1.0f, 1.1f, 1.3f, 1.5f, 1.7f};
    // parallel to FONT_SIZES — ends repeat: the 5 names predate the extended 7-stop scale
    private static final int[] FONT_SIZE_NAMES = new int[]{
            R.string.tiny, R.string.tiny, R.string.small, R.string.medium, R.string.large, R.string.huge, R.string.huge};
    private static final int VOLUME_MAX = 5;

    private AlarmVolumeControl alarmVolume;

    @Override
    protected int pageTitleRes() {
        return R.string.settings;
    }

    @Override
    protected void populateItems() {
        // User layer: the safe rows an elderly user can change directly.
        items.add(new Header(R.string.settings_for_user));
        alarmVolume = new AlarmVolumeControl(this);
        items.add(new RunnableSettingsItem(R.string.alarm_volume,
                v -> DialogScreen.info(this, R.string.alarm_volume, R.string.alarm_volume_subtext,
                        alarmVolume.view, R.string.back)
                , R.drawable.clock_on_background) {
            @Override
            public CharSequence getSubtitle() {
                return AlarmVolumeControl.subtitle(SettingsActivity.this);
            }
        });
        items.add(new RunnableSettingsItem(R.string.brightness,
                v -> startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)), R.drawable.brightness_on_button) {
            @Override
            public CharSequence getSubtitle() {
                return brightnessSubtitle(SettingsActivity.this);
            }
        });
        items.add(new RunnableSettingsItem(R.string.font_size, v -> startActivity(new Intent(this, FontChangerActivity.class)), R.drawable.font_on_button) {
            @Override
            public CharSequence getSubtitle() {
                return fontSizeName(SettingsActivity.this);
            }
        });
        items.add(new SwitchSettingsItem(R.string.vibrate, R.drawable.vibration_on_background,
                BPrefs.VIBRATION_FEEDBACK_KEY, BPrefs.VIBRATION_FEEDBACK_DEFAULT_VALUE) {
            @Override
            protected void onPrefChanged(boolean checked) {
                vibrator = checked ? (Vibrator) getSystemService(VIBRATOR_SERVICE) : null;
            }
        });
        items.add(new RunnableSettingsItem(R.string.theme_settings,
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
        items.add(new Header(R.string.settings_for_helper));
        items.add(new Category(R.string.helper_settings, R.drawable.settings_on_button, HelperSettingsActivity.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (alarmVolume != null)
            alarmVolume.stop();
    }

    /** pref 0..VOLUME_MAX -> real % of alarm stream (AlarmScreenActivity uses (v+6)/10, Android clamps at max). */
    private static String volumePercent(int v) {
        return Math.min((v + 6) * 10, 100) + "%";
    }

    /**
     * The ± stepper + live ringtone preview behind the Alarm Volume row's dialog.
     * One instance per host activity (settings page and home quick-settings share it);
     * call {@link #stop()} from onStop().
     */
    public static class AlarmVolumeControl {
        public final View view;
        private final SharedPreferences prefs;
        private final TextView tvVolume;
        private final Handler handler = new Handler();
        private Ringtone ringtone;
        private final Runnable closeRingtone = () -> {
            final Ringtone r = ringtone;
            if (r == null)
                return;
            r.stop();
            ringtone = null;
        };

        public AlarmVolumeControl(Context context) {
            prefs = BPrefs.get(context);
            view = LayoutInflater.from(context).inflate(R.layout.volume_buttons, null, false);
            tvVolume = view.findViewById(R.id.tv_volume);
            refresh();
            final View.OnClickListener step = v -> {
                final int volume = prefs.getInt(BPrefs.ALARM_VOLUME_KEY, BPrefs.ALARM_VOLUME_DEFAULT_VALUE)
                        + (v.getId() == R.id.bt_volume_up ? 1 : -1);
                if (volume < 0 || volume > VOLUME_MAX)
                    return;
                prefs.edit().putInt(BPrefs.ALARM_VOLUME_KEY, volume).apply();
                handler.removeCallbacks(closeRingtone);
                closeRingtone.run();
                ringtone = AlarmScreenActivity.getRingtone(view.getContext());
                if (ringtone != null)
                    ringtone.play();
                handler.postDelayed(closeRingtone, 4 * Const.SECOND);
                refresh();
            };
            view.findViewById(R.id.bt_volume_down).setOnClickListener(step);
            view.findViewById(R.id.bt_volume_up).setOnClickListener(step);
        }

        public void stop() {
            closeRingtone.run();
        }

        private void refresh() {
            tvVolume.setText(subtitle(view.getContext()));
        }

        public static CharSequence subtitle(Context context) {
            return volumePercent(BPrefs.get(context)
                    .getInt(BPrefs.ALARM_VOLUME_KEY, BPrefs.ALARM_VOLUME_DEFAULT_VALUE));
        }
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
}
