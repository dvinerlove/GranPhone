/*
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

package com.bald.uriah.baldphone.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * Elderly-friendly phone settings, applied once (the family can change them afterwards):
 * loud ringer and call volume, bigger letters, screen stays on longer, no auto-rotate.
 * Works both when GranPhone is a normal app (after "change system settings" is allowed)
 * and when it is built into the ROM.
 */
public final class FirstRunDefaults {
    private static final String TAG = FirstRunDefaults.class.getSimpleName();
    private static final String VOLUMES_DONE_KEY = "GRANPHONE_DEFAULT_VOLUMES_DONE";
    private static final String SYSTEM_SETTINGS_DONE_KEY = "GRANPHONE_DEFAULT_SYSTEM_SETTINGS_DONE";
    /** Same as the 5th step of the font size changer (see SettingsActivity.FONT_SIZES) */
    private static final float FONT_SCALE = 1.3f;
    private static final int SCREEN_OFF_TIMEOUT_MS = 2 * 60 * 1000;

    private FirstRunDefaults() {
    }

    public static void apply(Context context) {
        final SharedPreferences prefs = BPrefs.get(context);
        if (!prefs.getBoolean(VOLUMES_DONE_KEY, false)) {
            setVolumesToMax(context);
            prefs.edit().putBoolean(VOLUMES_DONE_KEY, true).apply();
        }
        if (!prefs.getBoolean(SYSTEM_SETTINGS_DONE_KEY, false) && canWriteSettings(context)) {
            if (applySystemSettings(context))
                prefs.edit().putBoolean(SYSTEM_SETTINGS_DONE_KEY, true).apply();
        }
    }

    /**
     * Turns the in-call (earpiece) volume all the way up.
     */
    public static void setCallVolumeToMax(Context context) {
        setStreamToMax((AudioManager) context.getSystemService(Context.AUDIO_SERVICE), AudioManager.STREAM_VOICE_CALL);
    }

    private static void setVolumesToMax(Context context) {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        setStreamToMax(audioManager, AudioManager.STREAM_RING);
        setStreamToMax(audioManager, AudioManager.STREAM_NOTIFICATION);
        setStreamToMax(audioManager, AudioManager.STREAM_VOICE_CALL);
    }

    private static void setStreamToMax(AudioManager audioManager, int stream) {
        if (audioManager == null)
            return;
        try {
            audioManager.setStreamVolume(stream, audioManager.getStreamMaxVolume(stream), 0);
        } catch (Exception e) { // e.g. "Do not disturb" is on
            Log.e(TAG, "could not set volume of stream " + stream, e);
        }
    }

    private static boolean canWriteSettings(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(context);
    }

    private static boolean applySystemSettings(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        try {
            if (Settings.System.getFloat(resolver, Settings.System.FONT_SCALE, 1f) < FONT_SCALE)
                Settings.System.putFloat(resolver, Settings.System.FONT_SCALE, FONT_SCALE);
            if (Settings.System.getInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT, 0) < SCREEN_OFF_TIMEOUT_MS)
                Settings.System.putInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT, SCREEN_OFF_TIMEOUT_MS);
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0);
            Settings.System.putInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "could not change system settings", e);
            return false;
        }
    }
}
