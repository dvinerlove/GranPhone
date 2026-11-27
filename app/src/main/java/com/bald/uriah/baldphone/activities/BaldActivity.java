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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bald.uriah.baldphone.BuildConfig;
import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.Const;
import com.bald.uriah.baldphone.utils.PredictiveBackHelper;
import com.bald.uriah.baldphone.utils.AppUtils;
import com.bald.uriah.baldphone.utils.CallManager;
import com.bald.uriah.baldphone.utils.PermissionContract;
import com.bald.uriah.baldphone.views.BaldTitleBar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_CALL_LOG;
import static android.Manifest.permission.WRITE_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.bald.uriah.baldphone.activities.PermissionActivity.EXTRA_INTENT;

/**
 * the parent of all of the activitys in this app.
 */
public abstract class BaldActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = BaldActivity.class.getSimpleName();
    // Keep the caller-facing names here; their bit contract lives in PermissionContract.
    protected static final int
            PERMISSION_NONE = PermissionContract.PERMISSION_NONE,
            PERMISSION_READ_PHONE_STATE = PermissionContract.PERMISSION_READ_PHONE_STATE,
            PERMISSION_WRITE_SETTINGS = PermissionContract.PERMISSION_WRITE_SETTINGS,
            PERMISSION_DEFAULT_PHONE_HANDLER = PermissionContract.PERMISSION_DEFAULT_PHONE_HANDLER,
            PERMISSION_READ_CONTACTS = PermissionContract.PERMISSION_READ_CONTACTS,
            PERMISSION_WRITE_CONTACTS = PermissionContract.PERMISSION_WRITE_CONTACTS,
            PERMISSION_POST_NOTIFICATIONS = PermissionContract.PERMISSION_POST_NOTIFICATIONS,
            PERMISSION_CALL_PHONE = PermissionContract.PERMISSION_CALL_PHONE,
            PERMISSION_READ_CALL_LOG = PermissionContract.PERMISSION_READ_CALL_LOG,
            PERMISSION_WRITE_CALL_LOG = PermissionContract.PERMISSION_WRITE_CALL_LOG,
            PERMISSION_CAMERA = PermissionContract.PERMISSION_CAMERA,
            PERMISSION_WRITE_EXTERNAL_STORAGE = PermissionContract.PERMISSION_WRITE_EXTERNAL_STORAGE,
            PERMISSION_NOTIFICATION_LISTENER = PermissionContract.PERMISSION_NOTIFICATION_LISTENER,
            PERMISSION_REQUEST_INSTALL_PACKAGES = PermissionContract.PERMISSION_REQUEST_INSTALL_PACKAGES,
            PERMISSION_SYSTEM_ALERT_WINDOW = PermissionContract.PERMISSION_SYSTEM_ALERT_WINDOW,
            PERMISSION_EXACT_ALARM = PermissionContract.PERMISSION_EXACT_ALARM,
            PERMISSION_MEDIA_IMAGES = PermissionContract.PERMISSION_MEDIA_IMAGES,
            PERMISSION_MEDIA_VIDEO = PermissionContract.PERMISSION_MEDIA_VIDEO,
            PERMISSION_ALL = PermissionContract.PERMISSION_ALL;

    public boolean testing = false;
    public boolean colorful;
    protected Vibrator vibrator;
    @StyleRes
    private int themeIndex;
    private String appliedLanguage;
    private List<WeakReference<Dialog>> dialogsToClose = new ArrayList<>(1);
    private List<WeakReference<PopupWindow>> popupWindowsToClose = new ArrayList<>(1);
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private boolean near;
    private int touches;
    private int accidentalMinTouches = 3, accidentalTime = 3 * Const.SECOND;
    private boolean useAccidentalGuard = true;
    private Handler handler;
    private Runnable touchesDecreaser = () -> touches = (touches -= 1) < 0 ? 0 : touches;

    @Override
    protected void attachBaseContext(Context newBase) {
        appliedLanguage = BPrefs.get(newBase).getString(BPrefs.LANGUAGE_KEY, BPrefs.LANGUAGE_DEFAULT_VALUE);
        if (appliedLanguage.isEmpty()) {
            super.attachBaseContext(newBase);
            return;
        }
        final java.util.Locale locale = java.util.Locale.forLanguageTag(appliedLanguage);
        java.util.Locale.setDefault(locale);
        final Configuration config = new Configuration();
        config.setLocale(locale);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    public void
    applyOverrideConfiguration(Configuration cfgOverride) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // add this to fix androidx.appcompat:appcompat 1.1.0 bug
            // which happens on Android 6.x ~ 7.x
            getResources();
        }

        super.applyOverrideConfiguration(cfgOverride);
    }

    /**
     * @return true if all permissions are granted.
     */
    public static boolean checkPermissions(BaldActivity activity, final int requiredPermissions) {
        final int permissions = PermissionContract.normalizeRequiredPermissions(requiredPermissions);
        if (permissions == PERMISSION_NONE)
            return true;
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_DEFAULT_DIALER)
                && !defaultDialerGranted(activity))
            return false;
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_WRITE_SETTINGS)
                && !writeSettingsGranted(activity))
            return false;
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_NOTIFICATION_LISTENER)
                && !notificationListenerGranted(activity))
            return false;
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_READ_CONTACTS)
                && ActivityCompat.checkSelfPermission(activity, READ_CONTACTS) != PERMISSION_GRANTED)
            return false;
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_WRITE_CONTACTS)
                && ActivityCompat.checkSelfPermission(activity, WRITE_CONTACTS) != PERMISSION_GRANTED)
            return false;
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_CALL_PHONE)
                && ActivityCompat.checkSelfPermission(activity, CALL_PHONE) != PERMISSION_GRANTED)
            return false;
        if (!BuildConfig.FLAVOR.equals("gPlay")) {
            if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_WRITE_CALL_LOG)
                    && ActivityCompat.checkSelfPermission(activity, WRITE_CALL_LOG) != PERMISSION_GRANTED)
                return false;
            if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_READ_CALL_LOG)
                    && ActivityCompat.checkSelfPermission(activity, READ_CALL_LOG) != PERMISSION_GRANTED)
                return false;
        }
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_READ_PHONE_STATE)
                && BPrefs.get(activity).getBoolean(BPrefs.DUAL_SIM_KEY, BPrefs.DUAL_SIM_DEFAULT_VALUE)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
                && ActivityCompat.checkSelfPermission(activity, READ_PHONE_STATE) != PERMISSION_GRANTED)
            return false;
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_CAMERA)
                && ActivityCompat.checkSelfPermission(activity, CAMERA) != PERMISSION_GRANTED)
            return false;
        // WRITE_EXTERNAL_STORAGE is a no-op since API 33; media-reading callers use the granular bits.
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_WRITE_EXTERNAL_STORAGE)
                && Build.VERSION.SDK_INT < 33
                && ActivityCompat.checkSelfPermission(activity, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED)
            return false;
        if (Build.VERSION.SDK_INT >= 33) {
            if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_MEDIA_IMAGES)
                    && !mediaPermissionGranted(activity, READ_MEDIA_IMAGES))
                return false;
            if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_MEDIA_VIDEO)
                    && !mediaPermissionGranted(activity, READ_MEDIA_VIDEO))
                return false;
        } else if ((PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_MEDIA_IMAGES)
                || PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_MEDIA_VIDEO))
                && ActivityCompat.checkSelfPermission(activity, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED)
            return false;
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_POST_NOTIFICATIONS)
                && Build.VERSION.SDK_INT >= 33
                && ActivityCompat.checkSelfPermission(activity, POST_NOTIFICATIONS) != PERMISSION_GRANTED)
            return false;
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_SYSTEM_ALERT_WINDOW)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && !Settings.canDrawOverlays(activity))
            return false;
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_EXACT_ALARM)
                && !AppUtils.canScheduleExactAlarms((android.app.AlarmManager) activity.getSystemService(Context.ALARM_SERVICE)))
            return false;
        if (PermissionContract.hasAtom(permissions, PermissionContract.PERMISSION_ATOM_REQUEST_INSTALL_PACKAGES)) {
            return
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                            (!BuildConfig.FLAVOR.equals("baldUpdates")) ||
                            activity.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    /**
     * Honest role state for the atomic default-dialer bit. Before API 23 there is no
     * dialer role to grant, so the requirement is vacuously satisfied. Legacy callers
     * do not carry this atom yet; Wave 3b can opt a flow in explicitly.
     */
    static boolean defaultDialerGranted(BaldActivity activity) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || CallManager.isDefaultDialer(activity);
    }

    static boolean writeSettingsGranted(BaldActivity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(activity);
        }
        return true;
    }

    static boolean notificationListenerGranted(BaldActivity activity) {
        final String listeners = Settings.Secure.getString(activity.getContentResolver(), "enabled_notification_listeners");
        return listeners != null && listeners.contains(activity.getApplicationContext().getPackageName());
    }

    static boolean mediaPermissionGranted(BaldActivity activity, String mediaPermission) {
        if (ActivityCompat.checkSelfPermission(activity, mediaPermission) == PERMISSION_GRANTED)
            return true;
        if (Build.VERSION.SDK_INT < 34
                || ActivityCompat.checkSelfPermission(activity, READ_MEDIA_VISUAL_USER_SELECTED) != PERMISSION_GRANTED)
            return false;
        // USER_SELECTED is shared by photos and videos; a visible row is the only type-specific evidence MediaStore exposes.
        final Uri collection = READ_MEDIA_IMAGES.equals(mediaPermission)
                ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        try (Cursor cursor = activity.getContentResolver().query(collection,
                new String[]{MediaStore.MediaColumns._ID}, null, null, null)) {
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // predictive back: intercepted screens get the events; un-intercepted
        // ones get the system's own predictive animation on Android 15
        if (backDispatchIntercepted())
            PredictiveBackHelper.register(this, this::onBackPressed);
        final SharedPreferences sharedPreferences = BPrefs.get(this);
        testing = sharedPreferences.getBoolean(BPrefs.TEST_KEY, BPrefs.TEST_DEFAULT_VALUE);

        if (!checkPermissions(this, requiredPermissions())) {
            startActivity(new Intent(this, PermissionActivity.class)
                    .putExtra(PermissionActivity.EXTRA_REQUIRED_PERMISSIONS, requiredPermissions())
                    .putExtra(EXTRA_INTENT, getIntent())
            );
            finish();
            return;
        }

        handler = new Handler();
        vibrator = sharedPreferences
                .getBoolean(BPrefs.VIBRATION_FEEDBACK_KEY, BPrefs.VIBRATION_FEEDBACK_DEFAULT_VALUE)
                ? (Vibrator) getSystemService(VIBRATOR_SERVICE) : null;
        colorful = sharedPreferences.getBoolean(BPrefs.COLORFUL_KEY, BPrefs.COLORFUL_DEFAULT_VALUE);
        themeIndex = AppUtils.getTheme(this);
        setTheme(themeIndex);

        final int statusBar = sharedPreferences.getInt(BPrefs.STATUS_BAR_KEY, BPrefs.STATUS_BAR_DEFAULT_VALUE);
        if (statusBar != 2) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (statusBar != 0) {
            final Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Const.DEFAULT_STATUS_BAR_COLOR);
        }

        if (useAccidentalGuard = sharedPreferences.getBoolean(BPrefs.USE_ACCIDENTAL_GUARD_KEY, BPrefs.USE_ACCIDENTAL_GUARD_DEFAULT_VALUE)) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager != null) {
                proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            } else {
                useAccidentalGuard = false;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (themeIndex != AppUtils.getTheme(this) ||
                !BPrefs.get(this).getString(BPrefs.LANGUAGE_KEY, BPrefs.LANGUAGE_DEFAULT_VALUE).equals(appliedLanguage))
            recreate();
        if (useAccidentalGuard)
            sensorManager.registerListener(this, proximitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        for (WeakReference<Dialog> dialogWeakReference : dialogsToClose) {
            final Dialog dialog = dialogWeakReference.get();
            if (dialog != null)
                dialog.dismiss();
        }

        for (WeakReference<PopupWindow> windowWeakReference : popupWindowsToClose) {
            final PopupWindow window = windowWeakReference.get();
            if (window != null)
                window.dismiss();
        }

        if (useAccidentalGuard)
            sensorManager.unregisterListener(this);
        super.onPause();
    }

    /**
     * Whether back is intercepted on API 33+ (predictive back): the callback
     * then receives every back event — default back still goes through
     * {@link #onBackPressed} which must never reach the dispatcher.
     */
    protected boolean backDispatchIntercepted() {
        return false;
    }

    @Override
    public void onBackPressed() {
        if (vibrator != null)
            vibrator.vibrate(Const.vibetime);
        // on API 33+ back goes through OnBackInvokedDispatcher — calling super
        // would re-enter it (registered callbacks invoke this method), so the
        // chain bottoms out at finish() instead
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            finish();
        else
            super.onBackPressed();
    }

    public void autoDismiss(Dialog dialog) {
        if (dialogsToClose.size() > 10)
            dialogsToClose = AppUtils.cleanWeakList(dialogsToClose);
        dialogsToClose.add(new WeakReference<>(dialog));
    }

    public void autoDismiss(PopupWindow popupWindow) {
        if (popupWindowsToClose.size() > 10)
            popupWindowsToClose = AppUtils.cleanWeakList(popupWindowsToClose);
        popupWindowsToClose.add(new WeakReference<>(popupWindow));
    }

    @Override
    public void onUserInteraction() {
        if (useAccidentalGuard) {
            if (near) {
                Log.e(TAG, "onUserInteraction: ");
                if (touches >= 0) {
                    touches += 1;
                    accidentalTouchChecker();
                    handler.postDelayed(touchesDecreaser, accidentalTime);
                }
            } else touches = 0;
        }
    }

    private void accidentalTouchChecker() {
        if (touches > accidentalMinTouches) {
            touches = -1;
            handler.removeCallbacks(touchesDecreaser);
            // non-cancelable: only «Понятно» dismisses → counters reset on dismissal
            final DialogScreen dialog = DialogScreen.info(this,
                    R.string.accidental_touches,
                    R.string.accidental_touches_subtext);
            dialog.setCancelable(false);
            dialog.setOnDismissListener(d -> {
                touches = 0;
                accidentalMinTouches += 2;
                accidentalTime /= 2;
            });

        }
    }

    public void onSensorChanged(SensorEvent event) {
        near = event.values[0] == 0;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * be sure {@link #setContentView(int)} was called, and the rootview contains a {@link BaldTitleBar} whose id is R.id.bald_title_bar.
     *
     * @param index the youtube video index. see
     */
    protected void setupYoutube(int index) {
        final View baldTitleBar = findViewById(R.id.bald_title_bar);
        if (baldTitleBar instanceof BaldTitleBar) {
            ((BaldTitleBar) baldTitleBar).getBt_help()
                    .setOnClickListener(
                            (v) -> startActivity(
                                    new Intent(this, YoutubeActivity.class)
                                            .putExtra(YoutubeActivity.EXTRA_ID, index)
                            )
                    );
        }

    }

    protected abstract int requiredPermissions();
}
