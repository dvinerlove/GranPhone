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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;

import com.duolingo.open.rtlviewpager.RtlViewPager;

import com.bald.uriah.baldphone.BuildConfig;
import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.databases.apps.AppsDatabaseHelper;
import com.bald.uriah.baldphone.services.NotificationListenerService;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldHomeWatcher;
import com.bald.uriah.baldphone.utils.BaldPrefsUtils;
import com.bald.uriah.baldphone.utils.CallManager;
import com.bald.uriah.baldphone.utils.FirstRunDefaults;
import com.bald.uriah.baldphone.utils.KioskManager;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.Const;
import com.bald.uriah.baldphone.utils.DropDownRecyclerViewAdapter;
import com.bald.uriah.baldphone.views.BaldPictureTextButton;
import com.bald.uriah.baldphone.utils.AppUtils;
import com.bald.uriah.baldphone.utils.UpdatingUtil;
import com.bald.uriah.baldphone.views.BaldImageButton;
import com.bald.uriah.baldphone.views.BaldSwitch;
import com.bald.uriah.baldphone.views.BatteryView;
import com.bald.uriah.baldphone.views.home.HomePage1;
import com.bald.uriah.baldphone.views.home.NotesView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.bald.uriah.baldphone.services.NotificationListenerService.ACTION_REGISTER_ACTIVITY;
import static com.bald.uriah.baldphone.services.NotificationListenerService.ACTIVITY_NONE;
import static com.bald.uriah.baldphone.services.NotificationListenerService.KEY_EXTRA_ACTIVITY;
import static com.bald.uriah.baldphone.services.NotificationListenerService.NOTIFICATIONS_ALOT;
import static com.bald.uriah.baldphone.services.NotificationListenerService.NOTIFICATIONS_HOME_SCREEN;
import static com.bald.uriah.baldphone.services.NotificationListenerService.NOTIFICATIONS_NONE;
import static com.bald.uriah.baldphone.services.NotificationListenerService.NOTIFICATIONS_SOME;

public class HomeScreenActivity extends BaldActivity {
    private static final String TAG = HomeScreenActivity.class.getSimpleName();

    private static final int[]
            SOUND_DRAWABLES = {R.drawable.mute_on_background, R.drawable.vibration_on_background, R.drawable.sound_on_background},
            SOUND_TEXTS = {R.string.mute, R.string.vibrate, R.string.sound};

    private static final IntentFilter BATTERY_FILTER = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private static final String KEY_HOME_PAGE = "home_page";
    private static final int SPEECH_REQUEST_CODE = 7;

    private static int onStartCounter = 0;
    private static boolean flashState;

    @NonNull
    public final NotesView.RecognizerManager recognizerManager = new NotesView.RecognizerManager();

    public boolean finishedUpdatingApps, launchAppsActivity;

    private Point screenSize;
    private android.hardware.camera2.CameraManager cameraManager;
    private String torchCameraId;
    private SharedPreferences sharedPreferences;
    private BaldPrefsUtils baldPrefsUtils;
    private HomePage1 homePage1;
    private RtlViewPager homePager;
    private int restoredHomePage;
    private BatteryView batteryView;
    private boolean lowBatteryAlert;
    /**
     * Listens to changes in battery {@value Intent#ACTION_BATTERY_CHANGED}
     */
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            applyBatteryIntent(intent);
        }
    };

    private void applyBatteryIntent(@Nullable Intent intent) {
        if (intent == null || batteryView == null)
            return;
        final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0)
            return;
        final int batteryPct = Math.round(level / (float) scale * 100);
        final int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        final boolean charged = chargePlug == BatteryManager.BATTERY_PLUGGED_AC || chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS || chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        batteryView.setLevel(batteryPct, charged);
        if (lowBatteryAlert)
            getWindow().setStatusBarColor((batteryPct < Const.LOW_BATTERY_LEVEL && !charged) ? ContextCompat.getColor(this, R.color.battery_low) : Const.DEFAULT_STATUS_BAR_COLOR);
    }
    private int notificationCount = 0;
    @ColorInt
    private int decorationColorOnBackground;
    private BaldImageButton notificationsButton, soundButton;
    private android.widget.ImageView signalView;
    private TextView alarmSubtitleView;
    private TextView brightnessSubtitleView;
    private TextView fontSubtitleView;
    private SettingsActivity.AlarmVolumeControl alarmVolume;
    private android.telephony.TelephonyManager telephonyManager;
    private final android.telephony.PhoneStateListener signalListener = new android.telephony.PhoneStateListener() {
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
    };
    private AudioManager audioManager;
    private BaldHomeWatcher baldHomeWatcher;
    private boolean flashInited;
    private Handler handler = new Handler();
    /**
     * "Shakes" the notifications icon when it has more than {@value NotificationListenerService#NOTIFICATIONS_ALOT}
     */
    private final Runnable shakeIt = new Runnable() {
        @Override
        public void run() {
            final Drawable d = notificationsButton.getDrawable();
            if (d instanceof AnimatedVectorDrawable) {
                final AnimatedVectorDrawable animatedVectorDrawable = (AnimatedVectorDrawable) d;
                animatedVectorDrawable.start();
                final int minusSeconds = Math.min((int) (Math.max((notificationCount - NOTIFICATIONS_ALOT) * 0.5f, 0)), 7);
                handler.postDelayed(this, (10 - minusSeconds) * Const.SECOND);
            }
        }
    };
    /**
     * Listens to broadcasts from {@link NotificationListenerService}
     * This listener only gets the number of notifications and updates {@link HomeScreenActivity#notificationsButton}
     * The red dot is being updated via {@link HomePage1#notificationReceiver}
     */
    public final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            notificationCount = intent.getIntExtra("amount", -1);
            if (notificationCount >= NOTIFICATIONS_ALOT) {
                final Drawable drawable = getDrawable(R.drawable.notification_alot_on_background);
                final float opacity = Math.min(((notificationCount - NOTIFICATIONS_ALOT) / 10.0f), 1.0f);
                drawable.setTint(AppUtils.blendColors(decorationColorOnBackground, getResources().getColor(R.color.battery_low), 1 - opacity));
                notificationsButton.setImageDrawable(drawable);
            } else if (notificationCount >= NOTIFICATIONS_SOME) {
                notificationsButton.setImageResource(R.drawable.notification_some_on_background);
            } else if (notificationCount >= NOTIFICATIONS_NONE) {
                notificationsButton.setImageResource(R.drawable.notification_none_on_background);
            } else {
                notificationsButton.setImageResource(R.drawable.error_on_background);
            }

            handler.removeCallbacks(shakeIt);
            handler.postDelayed(shakeIt, 5 * Const.SECOND);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.logImportant("HomeScreenActivity was started!");
        sharedPreferences = BPrefs.get(this);
        if (savedInstanceState != null)
            restoredHomePage = savedInstanceState.getInt(KEY_HOME_PAGE, 0);

        if (!sharedPreferences.getBoolean(BPrefs.AFTER_TUTORIAL_KEY, false) && !testing) {
            startActivity(new Intent(this, TutorialActivity.class));
            finish();
            return;
        }
        try {
            startService(new Intent(this, NotificationListenerService.class));
        } catch (Exception e) {
            Log.e(TAG, AppUtils.str(e.getMessage()));
            e.printStackTrace();
            BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(getString(R.string.notification_service_fail)).show();
        }
        new UpdateApps(this).execute(this.getApplicationContext());
        lowBatteryAlert = sharedPreferences.getBoolean(BPrefs.LOW_BATTERY_ALERT_KEY, BPrefs.LOW_BATTERY_ALERT_DEFAULT_VALUE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        final Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        screenSize = new Point();
        display.getSize(screenSize);

        final TypedValue typedValue = new TypedValue();
        final Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.bald_decoration_on_background, typedValue, true);
        decorationColorOnBackground = typedValue.data;

        if ((sharedPreferences.getInt(BPrefs.STATUS_BAR_KEY, BPrefs.STATUS_BAR_DEFAULT_VALUE) == 1)) {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        attachToXml();
        cameraManager = (android.hardware.camera2.CameraManager) getSystemService(Context.CAMERA_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                torchCameraId = cameraManager.getCameraIdList()[0];
                flashInited = true;
            } catch (Exception e) {
                flashInited = false;
            }
        }

        notificationsButton.setOnClickListener((v) -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });
        soundButton.setOnClickListener(v -> AppUtils.showDropDownPopup(this, getWindow().getDecorView().getWidth(), new DropDownRecyclerViewAdapter.DropDownListener() {
            @SuppressLint("InlinedApi")
            @Override
            public void onUpdate(DropDownRecyclerViewAdapter.ViewHolder viewHolder, final int position, PopupWindow popupWindow) {
                viewHolder.pic.setImageResource(SOUND_DRAWABLES[position]);
                viewHolder.text.setText(SOUND_TEXTS[position]);
                viewHolder.itemView.setOnClickListener(v1 -> {
                    try {
                        audioManager.setRingerMode(position);
                        soundButton.setImageResource(SOUND_DRAWABLES[position]);
                    } catch (SecurityException e) {
                        startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                    }
                    popupWindow.dismiss();
                });
            }

            @Override
            public int size() {
                return 3;
            }
        }, soundButton));
        batteryView.setOnClickListener((v) -> BaldToast.from(this)
                .setText(batteryView.percentage + "%")
                .setBig(true)
                .setType(BaldToast.TYPE_INFORMATIVE)
                .show());
        baldPrefsUtils = BaldPrefsUtils.newInstance(this);
        baldHomeWatcher = new BaldHomeWatcher(this, this::refreshHome);
        recognizerManager.setHomeScreen(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        onStartCounter++;
        if (finishedUpdatingApps)
            refreshHome();
        baldHomeWatcher.startWatch();
        if (!testing) { // TODO replace with system env
            final int percent = (int) (Math.random() * 100) + 1;//1 - 100
            if (percent < (100 + (onStartCounter - 20) * 5))
                if (percent % 3 == 1 && !isActivityDefault()) { // one in 3 chance
                    onStartCounter = 0;
                    DialogScreen.confirm(this,
                            R.string.set_home_screen,
                            R.string.set_home_screen_subtext,
                            R.string.install,
                            () -> FakeLauncherActivity.resetPreferredLauncherAndOpenChooser(this));
                } else if (percent > 95) {
                    //noinspection ConstantConditions
                    if (BuildConfig.FLAVOR.equals("baldUpdates"))
                        if (sharedPreferences.getLong(BPrefs.LAST_UPDATE_ASKED_VERSION_KEY, 0) + 2 * Const.DAY < System.currentTimeMillis()) {
                            UpdatingUtil.checkForUpdates(this, false);
                        }
                }
        }

    }

    /* the security exception will happen only after api 23 so Lint please shush*/
    @SuppressLint("InlinedApi")
    protected void onResume() { // remember to change in Page1EditorActivity.java too!
        super.onResume();
        hideNavBar();
        // While on a call, the home button brings the user back to the big call screen.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && CallManager.hasOngoingCall())
            startActivity(CallActivity.newIntent(this));
        KioskManager.enterIfNeeded(this);
        FirstRunDefaults.apply(this);
        if (baldPrefsUtils.hasChanged(this)) {
            this.recreate();
        }
        refreshQuickSettingSubtitles();

        soundButton.setImageResource(SOUND_DRAWABLES[audioManager.getRingerMode()]);
        try {
            telephonyManager.listen(signalListener, android.telephony.PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        } catch (Exception ignore) {
        }

        LocalBroadcastManager.getInstance(this).
                registerReceiver(notificationReceiver,
                        new IntentFilter(NotificationListenerService.HOME_SCREEN_ACTIVITY_BROADCAST));
        handler.postDelayed(() -> LocalBroadcastManager.getInstance(this).
                sendBroadcast(
                        new Intent(ACTION_REGISTER_ACTIVITY)
                                .putExtra(KEY_EXTRA_ACTIVITY, NOTIFICATIONS_HOME_SCREEN)), 200 * Const.MILLISECOND);

        registerReceiver(batteryReceiver, BATTERY_FILTER);
        refreshBatteryView();
    }

    // Bound once at page creation; refresh on resume — brightness lives in system settings, outside hasChanged().
    private void refreshQuickSettingSubtitles() {
        if (alarmSubtitleView != null)
            alarmSubtitleView.setText(SettingsActivity.AlarmVolumeControl.subtitle(this));
        if (brightnessSubtitleView != null)
            brightnessSubtitleView.setText(SettingsActivity.brightnessSubtitle(this));
        if (fontSubtitleView != null)
            fontSubtitleView.setText(SettingsActivity.fontSizeName(this));
    }

    // Receiver delivery of the sticky broadcast is unreliable on some ROMs — read it synchronously once per resume.
    private void refreshBatteryView() {
        applyBatteryIntent(registerReceiver(null, BATTERY_FILTER));
    }

    // Sticky immersive: nav bar hidden on the home screen; a bottom edge swipe reveals it briefly.
    private void hideNavBar() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            hideNavBar();
    }

    public boolean isFlashOn() {
        return flashState;
    }

    public void toggleTorch() {
        if (!flashInited)
            return;
        flashState = !flashState;
        try {
            cameraManager.setTorchMode(torchCameraId, flashState);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (homePager != null)
            outState.putInt(KEY_HOME_PAGE, homePager.getCurrentItem());
    }

    @Override
    protected void onPause() {
        //read https://stackoverflow.com/questions/6165070/receiver-not-registered-exception-error
        //android platform may unregister the receiver without asking anyone, and this is the best solution.
        //first occurred in LG k10 api level 23
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
        } catch (IllegalArgumentException ignore) {
        }
        try {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_REGISTER_ACTIVITY).putExtra(KEY_EXTRA_ACTIVITY, ACTIVITY_NONE));
        } catch (IllegalArgumentException ignore) {
        }
        try {
            unregisterReceiver(batteryReceiver);
        } catch (IllegalArgumentException ignore) {
        }
        try {
            telephonyManager.listen(signalListener, android.telephony.PhoneStateListener.LISTEN_NONE);
        } catch (Exception ignore) {
        }
        handler.removeCallbacks(shakeIt);
        super.onPause();
    }

    @Override
    protected void onStop() {
        baldHomeWatcher.stopWatch();
        if (alarmVolume != null)
            alarmVolume.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        recognizerManager.setHomeScreen(null);
        super.onDestroy();
    }

    /** Re-binds tile slots from preferences (pins/customizations made elsewhere). */
    private void refreshHome() {
        // BaldHomeWatcher/onStart can fire before attachToXml() binds homePage1.
        if (homePage1 != null)
            homePage1.refreshTiles();
    }

    private void attachToXml() {
        setContentView(R.layout.home_screen);
        homePager = findViewById(R.id.home_pager);
        final View mainPage = LayoutInflater.from(this).inflate(R.layout.home_page_main, homePager, false);
        homePage1 = (HomePage1) mainPage;

        findViewById(R.id.bt_home_sos).setOnClickListener(v -> startActivity(new Intent(this, SOSActivity.class)));
        findViewById(R.id.bt_home_apps).setOnClickListener(v -> {
            if (!finishedUpdatingApps)
                launchAppsActivity = true;
            else
                startActivity(new Intent(this, AppsActivity.class));
        });
        findViewById(R.id.bt_home_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));

        if (sharedPreferences.getBoolean(BPrefs.HIDE_BOTTOM_ROW_KEY, BPrefs.HIDE_BOTTOM_ROW_DEFAULT_VALUE)) {
            findViewById(R.id.bottom_row).setVisibility(View.GONE);
            final View settingsPage = LayoutInflater.from(this).inflate(R.layout.home_settings_page, homePager, false);
            wireSettingsPage(settingsPage);
            homePager.setAdapter(new HomePagerAdapter(mainPage, settingsPage));
        } else {
            homePager.setAdapter(new HomePagerAdapter(mainPage));
        }
        // Land on the page the user left — recreate/process-kill must not dump them back on page 1.
        if (restoredHomePage > 0)
            homePager.setCurrentItem(restoredHomePage, false);

        // Bottom-row labels: one fixed size for all three — autosize made each label a different size.
        for (final int id : new int[]{R.id.bt_home_sos, R.id.bt_home_apps, R.id.bt_home_settings}) {
            final android.widget.TextView label = ((com.bald.uriah.baldphone.views.BaldPictureTextButton) findViewById(id)).getTextView();
            label.setMaxLines(1);
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.tiny));
        }

        final ViewGroup top_bar = findViewById(R.id.top_bar);
        int tmpPadding = Math.min(screenSize.x, screenSize.y) / 45;
        for (int i = 0; i < top_bar.getChildCount(); i++) {
            top_bar.getChildAt(i).setPadding(tmpPadding, tmpPadding, tmpPadding, tmpPadding);
        }

        signalView = top_bar.findViewById(R.id.signal);
        telephonyManager = (android.telephony.TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        soundButton = top_bar.findViewById(R.id.sound);
        batteryView = top_bar.findViewById(R.id.battery);
        notificationsButton = top_bar.findViewById(R.id.notifications);
    }

    /** Second home page (only when the bottom row is hidden): nav buttons + quick settings. */
    private void wireSettingsPage(final View page) {
        page.findViewById(R.id.bt_page_sos).setOnClickListener(v -> startActivity(new Intent(this, SOSActivity.class)));
        page.findViewById(R.id.bt_page_apps).setOnClickListener(v -> {
            if (!finishedUpdatingApps)
                launchAppsActivity = true;
            else
                startActivity(new Intent(this, AppsActivity.class));
        });
        page.findViewById(R.id.bt_page_settings).setOnClickListener(v ->
                startActivity(new Intent(this, HelperSettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));

        final ViewGroup container = page.findViewById(R.id.quick_settings_container);
        alarmVolume = new SettingsActivity.AlarmVolumeControl(this);
        alarmSubtitleView = (TextView) addQuickSetting(container, R.drawable.clock_on_background, R.string.alarm_volume,
                SettingsActivity.AlarmVolumeControl.subtitle(this),
                () -> DialogScreen.info(this, R.string.alarm_volume, R.string.alarm_volume_subtext,
                        alarmVolume.view, R.string.back))
                .findViewById(R.id.tv_setting_subtitle);
        brightnessSubtitleView = (TextView) addQuickSetting(container, R.drawable.brightness_on_button, R.string.brightness,
                SettingsActivity.brightnessSubtitle(this),
                () -> startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)))
                .findViewById(R.id.tv_setting_subtitle);
        fontSubtitleView = (TextView) addQuickSetting(container, R.drawable.font_on_button, R.string.font_size,
                SettingsActivity.fontSizeName(this),
                () -> startActivity(new Intent(this, FontChangerActivity.class)))
                .findViewById(R.id.tv_setting_subtitle);
        addQuickSetting(container, R.drawable.brush_on_button, R.string.theme_settings,
                getText(sharedPreferences.getInt(BPrefs.THEME_KEY, BPrefs.THEME_DEFAULT_VALUE) == BPrefs.Themes.DARK
                        ? R.string.dark : R.string.light),
                () -> DialogScreen.select(this, R.string.theme_settings,
                        Math.max(0, Math.min(sharedPreferences.getInt(BPrefs.THEME_KEY, BPrefs.THEME_DEFAULT_VALUE),
                                BPrefs.Themes.THEMES.length - 1)) == BPrefs.Themes.DARK ? 1 : 0,
                        index -> {
                            sharedPreferences.edit().putInt(BPrefs.THEME_KEY,
                                    index == 1 ? BPrefs.Themes.DARK : BPrefs.Themes.LIGHT).apply();
                            this.recreate();
                        },
                        R.string.light, R.string.dark));
        final View vibrateRow = addQuickSetting(container, R.drawable.vibration_on_background, R.string.vibrate, null, null);
        final BaldSwitch sw = new BaldSwitch(this);
        sw.setLayoutParams(new ViewGroup.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.switch_width),
                getResources().getDimensionPixelSize(R.dimen.switch_height)));
        sw.setChecked(sharedPreferences.getBoolean(BPrefs.VIBRATION_FEEDBACK_KEY, BPrefs.VIBRATION_FEEDBACK_DEFAULT_VALUE));
        sw.setOnChangeListener(this::applyVibrationPref);
        ((ViewGroup) vibrateRow.findViewById(R.id.setting_trailing)).addView(sw);
        vibrateRow.setOnClickListener(v -> {
            // BaldSwitch.setChecked doesn't fire the change listener — apply manually.
            sw.setChecked(!sw.isChecked());
            applyVibrationPref(sw.isChecked());
        });
    }

    private void applyVibrationPref(boolean checked) {
        sharedPreferences.edit().putBoolean(BPrefs.VIBRATION_FEEDBACK_KEY, checked).apply();
        vibrator = checked ? (Vibrator) getSystemService(VIBRATOR_SERVICE) : null;
    }

    private View addQuickSetting(final ViewGroup container, final int iconRes, final int titleRes,
                                 final CharSequence subtitle, final Runnable onClick) {
        final View row = LayoutInflater.from(this).inflate(R.layout.settings_item, container, false);
        ((ImageView) row.findViewById(R.id.setting_icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.tv_setting_name)).setText(titleRes);
        if (subtitle != null) {
            final TextView sub = row.findViewById(R.id.tv_setting_subtitle);
            sub.setText(subtitle);
            sub.setVisibility(View.VISIBLE);
        }
        if (onClick != null)
            row.setOnClickListener(v -> onClick.run());
        container.addView(row);
        return row;
    }

    private static class HomePagerAdapter extends PagerAdapter {
        private final View[] pages;

        HomePagerAdapter(View... pages) {
            this.pages = pages;
        }

        @Override
        public int getCount() {
            return pages.length;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            container.addView(pages[position]);
            return pages[position];
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        try {
            super.startActivity(intent, options);
        } catch (Exception e) {
            Log.e(TAG, AppUtils.str(e.getMessage()));
            e.printStackTrace();
            BaldToast.error(this);
        }
    }

    private boolean isActivityDefault() {
        final List<IntentFilter> filters = new ArrayList<>();
        final ComponentName myComponentName = getComponentName();
        final String myPackageName = myComponentName.getPackageName();
        final List<ComponentName> activities = new ArrayList<>();
        final PackageManager packageManager = getPackageManager();
        packageManager.getPreferredActivities(filters, activities, myPackageName);
        return (activities.contains(myComponentName));
    }

    public void displaySpeechRecognizer() {
        try {
            startActivityForResult(
                    new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            ),
                    SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, AppUtils.str(e.getMessage()));
            e.printStackTrace();
            BaldToast.error(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            final String spokenText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            recognizerManager.onSpeechRecognizerResult(spokenText);
        }
    }

    @Override
    public void onBackPressed() {
        if (vibrator != null)
            vibrator.vibrate(Const.vibetime);
        // launcher convention: back on a secondary page slides home to page 0
        if (homePager != null && homePager.getCurrentItem() > 0) {
            homePager.setCurrentItem(0);
            return;
        }
        refreshHome();
    }

    // launcher consumes back (refreshHome); a callback keeps it on API 33+
    @Override
    protected boolean backDispatchIntercepted() {
        return true;
    }

    static class UpdateApps extends AsyncTask<Context, Void, Void> {
        final WeakReference<HomeScreenActivity> homeScreenWeakReference;

        UpdateApps(HomeScreenActivity homeScreen) {
            super();
            homeScreenWeakReference = new WeakReference<>(homeScreen);
        }

        @Override
        protected Void doInBackground(Context... contexts) {
            try {
                AppsDatabaseHelper.updateDB(contexts[0]);
            } catch (Exception e) {
                BaldToast.from(contexts[0].getApplicationContext()).setType(BaldToast.TYPE_ERROR).setLength(Toast.LENGTH_LONG).setText(e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            HomeScreenActivity homeScreen = homeScreenWeakReference.get();
            if (homeScreen != null) {
                homeScreen.refreshHome();
                homeScreen.finishedUpdatingApps = true;
                if (homeScreen.launchAppsActivity)
                    homeScreen.startActivity(new Intent(homeScreen, AppsActivity.class));
            }
        }

    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_NONE;
    }
}