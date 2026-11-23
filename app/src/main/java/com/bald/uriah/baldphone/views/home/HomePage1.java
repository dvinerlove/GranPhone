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

package com.bald.uriah.baldphone.views.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.provider.Telephony;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bald.uriah.baldphone.BuildConfig;
import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.AppsActivity;
import com.bald.uriah.baldphone.activities.DialerActivity;
import com.bald.uriah.baldphone.activities.HomeScreenActivity;
import com.bald.uriah.baldphone.activities.Page1EditorActivity;
import com.bald.uriah.baldphone.activities.RecentActivity;
import com.bald.uriah.baldphone.activities.SOSActivity;
import com.bald.uriah.baldphone.activities.alarms.AlarmsActivity;
import com.bald.uriah.baldphone.activities.contacts.ContactsActivity;
import com.bald.uriah.baldphone.activities.media.PhotosActivity;
import com.bald.uriah.baldphone.activities.media.VideosActivity;
import com.bald.uriah.baldphone.activities.pills.PillsActivity;
import com.bald.uriah.baldphone.databases.apps.App;
import com.bald.uriah.baldphone.databases.apps.AppsDatabase;
import com.bald.uriah.baldphone.databases.apps.AppsDatabaseHelper;
import com.bald.uriah.baldphone.databases.calls.CallLogsHelper;
import com.bald.uriah.baldphone.databases.home_screen_pins.HomeScreenPinHelper;
import com.bald.uriah.baldphone.services.NotificationListenerService;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.Const;
import com.bald.uriah.baldphone.utils.AppUtils;
import com.bald.uriah.baldphone.views.BaldLinearLayoutButton;
import com.bald.uriah.baldphone.views.FirstPageAppIcon;
import com.bald.uriah.baldphone.views.HomeScreenAppView;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.bald.uriah.baldphone.databases.apps.AppsDatabaseHelper.baldComponentNameBeginning;
import static com.bald.uriah.baldphone.services.NotificationListenerService.ACTION_REGISTER_ACTIVITY;
import static com.bald.uriah.baldphone.services.NotificationListenerService.KEY_EXTRA_ACTIVITY;
import static com.bald.uriah.baldphone.services.NotificationListenerService.NOTIFICATIONS_HOME_SCREEN;

@SuppressLint("ViewConstructor")
public class HomePage1 extends HomeView {
    public static final String TAG = HomePage1.class.getSimpleName();
    private final static String WHATSAPP_PACKAGE_NAME = "com.whatsapp";
    private final static String HIDDEN_TILE = "__HIDDEN__";
    private final static String TILES_SEEDED_KEY = "HOME_TILES_SEEDED";
    private final static ComponentName WHATSAPP_COMPONENT_NAME =
            new ComponentName(WHATSAPP_PACKAGE_NAME, WHATSAPP_PACKAGE_NAME + ".Main");
    public Map<App, FirstPageAppIcon> viewsToApps;
    /**
     * Listens to broadcasts from {@link NotificationListenerService}
     * This listener only checks if there are new messages\whatsapps,
     * and updates {@link HomePage1#bt_messages} and {@link HomePage1#bt_whatsapp} according to it
     * The notification icon is being updated via {@link HomeScreenActivity#notificationReceiver}
     */
    public final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Set<String> packagesSet = new HashSet<>(intent.getStringArrayListExtra("packages"));
            if (!viewsToApps.containsValue(bt_whatsapp))
                bt_whatsapp.setBadgeVisibility(packagesSet.contains(Const.WHATSAPP_PACKAGE_NAME));
            if (!viewsToApps.containsValue(bt_recent))
                bt_recent.setBadgeVisibility(!CallLogsHelper.isAllReadSafe(getContext().getContentResolver()));
            if (!viewsToApps.containsValue(bt_messages))
                bt_messages.setBadgeVisibility(packagesSet.contains(Telephony.Sms.getDefaultSmsPackage(context)));
            for (final App app : viewsToApps.keySet()) {
                viewsToApps.get(app).setBadgeVisibility(packagesSet.contains(ComponentName.unflattenFromString(app.getFlattenComponentName()).getPackageName()));
            }

        }
    };
    private View view;
    private FirstPageAppIcon bt_clock, bt_camera, bt_videos, bt_assistant, bt_messages, bt_photos, bt_contacts, bt_dialer, bt_whatsapp, bt_apps, bt_reminders, bt_recent;
    private boolean registered = false;
    private SharedPreferences sharedPreferences;

    public HomePage1(@NonNull Context context) {
        super((context instanceof HomeScreenActivity) ? (HomeScreenActivity) context : null, (Activity) context);
        sharedPreferences = BPrefs.get(activity);
    }

    public HomePage1(@NonNull Context context, AttributeSet attributeSet) {
        this(context);
        // attrs never reach View (ctor chain drops them) so android:id is not applied — set it.
        setId(R.id.home_page_1);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container) {
        viewsToApps = new ArrayMap<>();
        view = inflater.inflate(R.layout.fragment_home_page1, container, false);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            view.findViewById(R.id.clock).setVisibility(View.GONE);

        attachXml();
        genOnClickListeners();
        populatePins();
        return view;
    }

    /**
     * Re-reads {@link HomeScreenPinHelper#getAll} and renders pinned contacts/apps as
     * 2-column tile rows below the clock. The section stays hidden when there are no pins.
     */
    public void populatePins() {
        final LinearLayout pinsContainer = view.findViewById(R.id.pins_container);
        pinsContainer.removeAllViews();
        final List<HomeScreenPinHelper.HomeScreenPinnable> pins = homeScreen == null ?
                Collections.emptyList() : HomeScreenPinHelper.getAll(activity);
        if (pins.isEmpty()) {
            pinsContainer.setVisibility(GONE);
            return;
        }
        pinsContainer.setVisibility(VISIBLE);
        final int tileHeight = getResources().getDimensionPixelSize(R.dimen.home_tile_row_height);
        final int tileMargin = getResources().getDimensionPixelSize(R.dimen.home_pin_tile_margin);
        final LayoutInflater inflater = LayoutInflater.from(activity);
        for (int i = 0; i < pins.size(); i += 2) {
            final LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = i; j < i + 2; j++) {
                final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, tileHeight, 1);
                lp.setMargins(tileMargin, 0, tileMargin, 0);
                if (j < pins.size()) {
                    final BaldLinearLayoutButton tile = (BaldLinearLayoutButton)
                            inflater.inflate(R.layout.home_screen_app_view, row, false);
                    row.addView(tile, lp);
                    pins.get(j).applyToHomeScreenAppView(new HomeScreenAppView(tile));
                    // Names must stay readable — allow two lines with ellipsis instead of clipping.
                    final TextView name = tile.findViewById(R.id.et_name);
                    name.setMaxLines(2);
                    name.setEllipsize(TextUtils.TruncateAt.END);
                } else {
                    row.addView(new Space(activity), lp);
                }
            }
            final LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i + 2 < pins.size())
                rowLp.bottomMargin = tileMargin * 2;
            pinsContainer.addView(row, rowLp);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!registered) {
            LocalBroadcastManager.getInstance(activity)
                    .registerReceiver(notificationReceiver,
                            new IntentFilter(NotificationListenerService.HOME_SCREEN_ACTIVITY_BROADCAST));
            registered = true;
            LocalBroadcastManager.getInstance(activity).sendBroadcast(
                    new Intent(ACTION_REGISTER_ACTIVITY)
                            .putExtra(KEY_EXTRA_ACTIVITY, NOTIFICATIONS_HOME_SCREEN));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (registered) {
            LocalBroadcastManager.getInstance(activity)
                    .unregisterReceiver(notificationReceiver);
            registered = false;
        }
    }

    private void attachXml() {
        bt_apps = view.findViewById(R.id.bt_apps);
        bt_contacts = view.findViewById(R.id.bt_contacts);
        bt_dialer = view.findViewById(R.id.bt_dialer);
        bt_whatsapp = view.findViewById(R.id.bt_whatsapp);
        bt_clock = view.findViewById(R.id.bt_clock);
        bt_reminders = view.findViewById(R.id.bt_reminders);
        bt_recent = view.findViewById(R.id.bt_recent);
        bt_camera = view.findViewById(R.id.bt_camera);
        bt_videos = view.findViewById(R.id.bt_videos);
        bt_photos = view.findViewById(R.id.bt_photos);
        bt_messages = view.findViewById(R.id.bt_messages);
        bt_assistant = view.findViewById(R.id.bt_assistant);
    }

    private Intent getCameraIntent() {
        final Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        final ResolveInfo resolveInfo = activity.getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo == null)
            return null;
        final ActivityInfo activityInfo = resolveInfo.activityInfo;
        final ComponentName name = new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);
        return new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                .setComponent(name);

    }

    private void seedHiddenTiles() {
        if (sharedPreferences.getBoolean(TILES_SEEDED_KEY, false))
            return;
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String key : new String[]{BPrefs.CUSTOM_APP_KEY, BPrefs.CUSTOM_ASSISTANT_KEY, BPrefs.CUSTOM_MESSAGES_KEY,
                BPrefs.CUSTOM_PHOTOS_KEY, BPrefs.CUSTOM_CAMERA_KEY, BPrefs.CUSTOM_VIDEOS_KEY,
                BPrefs.CUSTOM_PILLS_KEY, BPrefs.CUSTOM_APPS_KEY, BPrefs.CUSTOM_ALARMS_KEY})
            if (!sharedPreferences.contains(key))
                editor.putString(key, HIDDEN_TILE);
        editor.putBoolean(TILES_SEEDED_KEY, true).apply();
    }

    private void genOnClickListeners() {
        sharedPreferences = BPrefs.get(activity);
        seedHiddenTiles();
        setupButton(BPrefs.CUSTOM_RECENTS_KEY, bt_recent, v -> homeScreen.startActivity(new Intent(homeScreen, RecentActivity.class)));
        setupButton(BPrefs.CUSTOM_DIALER_KEY, bt_dialer, v -> homeScreen.startActivity(new Intent(homeScreen, DialerActivity.class)));
        setupButton(BPrefs.CUSTOM_CONTACTS_KEY, bt_contacts, v -> homeScreen.startActivity(new Intent(homeScreen, ContactsActivity.class)));
        setupButton(BPrefs.CUSTOM_APP_KEY, bt_whatsapp, v -> homeScreen.startActivity(new Intent(homeScreen, SOSActivity.class)));
        setupButton(BPrefs.CUSTOM_ASSISTANT_KEY, bt_assistant, v -> {
            homeScreen.toggleTorch();
            updateFlashIcon();
        });
        bt_assistant.post(this::updateFlashIcon);
        setupButton(BPrefs.CUSTOM_MESSAGES_KEY, bt_messages, v -> {
            try {
                final String smsPackage = Telephony.Sms.getDefaultSmsPackage(homeScreen);
                if (smsPackage == null) {
                    BaldToast.from(homeScreen).setType(BaldToast.TYPE_ERROR).setText(R.string.an_error_has_occurred).show();
                    return;
                }
                final ResolveInfo resolveInfo =
                        homeScreen.getPackageManager()
                                .queryIntentActivities(
                                        new Intent("android.intent.action.MAIN", null)
                                                .setPackage(smsPackage)
                                        , 0)
                                .iterator()
                                .next();
                AppUtils.startComponentName(homeScreen, new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));

            } catch (Exception e) {
                BaldToast.from(homeScreen).setType(BaldToast.TYPE_ERROR).setText(R.string.an_error_has_occurred).show();
            }
        });
        setupButton(BPrefs.CUSTOM_PHOTOS_KEY, bt_photos, v -> homeScreen.startActivity(new Intent(homeScreen, PhotosActivity.class)));
        setupButton(BPrefs.CUSTOM_CAMERA_KEY, bt_camera, v -> {
            final Intent intent = getCameraIntent();
            if (intent != null)
                homeScreen.startActivity(intent);
            else
                BaldToast.from(homeScreen).setType(BaldToast.TYPE_ERROR).setText(R.string.an_error_has_occurred).show();
        });
        setupButton(BPrefs.CUSTOM_VIDEOS_KEY, bt_videos, v -> homeScreen.startActivity(new Intent(homeScreen, VideosActivity.class)));
        setupButton(BPrefs.CUSTOM_PILLS_KEY, bt_reminders, v -> homeScreen.startActivity(new Intent(homeScreen, PillsActivity.class)));
        setupButton(BPrefs.CUSTOM_APPS_KEY, bt_apps, v -> {
            if (!homeScreen.finishedUpdatingApps)
                homeScreen.launchAppsActivity = true;
            else
                homeScreen.startActivity(new Intent(HomePage1.this.homeScreen, AppsActivity.class));
        });
        setupButton(BPrefs.CUSTOM_ALARMS_KEY, bt_clock, v -> homeScreen.startActivity(new Intent(homeScreen, AlarmsActivity.class)));
        if (homeScreen != null)
            for (int id : new int[]{R.id.phone_container, R.id.messages_container, R.id.media_container, R.id.time_container})
                hideRowIfEmpty(id);
    }

    private void hideRowIfEmpty(int id) {
        final ViewGroup row = view.findViewById(id);
        for (int i = 0; i < row.getChildCount(); i++) {
            final View child = row.getChildAt(i);
            if (child instanceof FirstPageAppIcon && child.getVisibility() == View.VISIBLE)
                return;
        }
        row.setVisibility(View.GONE);
    }

    private void updateFlashIcon() {
        if (homeScreen == null)
            return;
        bt_assistant.setImageResource(homeScreen.isFlashOn()
                ? R.drawable.flashlight_on_background
                : R.drawable.flashlight_off_on_background);
    }

    private void setupButton(String bPrefsKey, FirstPageAppIcon bt, View.OnClickListener onClickListener) {
        final String prefValue = sharedPreferences.getString(bPrefsKey, null);
        final boolean hidden = HIDDEN_TILE.equals(prefValue);
        if (hidden) {
            if (homeScreen != null) {
                bt.setVisibility(View.GONE);
                return;
            }
            bt.setAlpha(0.3f);
        }
        final App app;
        boolean phone = false;
        if (bt == bt_whatsapp && BuildConfig.FLAVOR.equals("gPlay") && !sharedPreferences.contains(bPrefsKey)) {
            app = AppsDatabase.getInstance(homeScreen).appsDatabaseDao().findByFlattenComponentName(baldComponentNameBeginning + Page1EditorActivity.class.getName());
        } else if (bt == bt_recent && BuildConfig.FLAVOR.equals("gPlay") && !sharedPreferences.contains(bPrefsKey)) {
            app = null;
            phone = true;
        } else if (prefValue != null && !hidden) {
            app = AppsDatabase.getInstance(homeScreen).appsDatabaseDao().findByFlattenComponentName(prefValue);
            if (app == null)
                sharedPreferences.edit().remove(bPrefsKey).apply();
        } else app = null;
        if (homeScreen != null) {
            if (app == null) {
                if (phone) {
                    bt.setOnClickListener(v -> activity.startActivity(AppUtils.getPhoneIntent(activity)));
                    bt.setText(R.string.phone);
                    bt.setImageResource(R.drawable.phone_on_button);
                } else {
                    bt.setOnClickListener(onClickListener);
                }
            } else {
                bt.setText(app.getLabel());
                AppsDatabaseHelper.loadPic(app, bt.imageView);
                bt.setOnClickListener(v -> AppUtils.startComponentName(homeScreen, ComponentName.unflattenFromString(app.getFlattenComponentName())));
                viewsToApps.put(app, bt);
            }
        } else {
            final Page1EditorActivity page1EditorActivity = (Page1EditorActivity) activity;
            final CharSequence initialAppName;
            if (BuildConfig.FLAVOR.equals("gPlay")) {
                if (bt == bt_whatsapp) {
                    initialAppName = activity.getString(R.string.edit_home_screen);
                } else if (bt == bt_recent)
                    initialAppName = activity.getString(R.string.phone);
                else initialAppName = bt.getText();
            } else {
                initialAppName = bt.getText();
            }
            bt.setOnClickListener(v -> DialogScreen.select(activity, R.string.custom_app,
                            hidden ? 2 : (sharedPreferences.contains(bPrefsKey) ? 1 : 0),
                            index -> {
                                if (index == 0)
                                    sharedPreferences.edit().remove(bPrefsKey).apply();
                                else if (index == 1)
                                    activity.startActivityForResult(new Intent(activity, AppsActivity.class).putExtra(AppsActivity.CHOOSE_MODE, bPrefsKey), AppsActivity.REQUEST_SELECT_CUSTOM_APP);
                                else
                                    sharedPreferences.edit().putString(bPrefsKey, HIDDEN_TILE).apply();
                            },
                            initialAppName, activity.getText(R.string.custom), activity.getText(R.string.hide))
                    .setOnDismissListener(dialog -> {
                        if (page1EditorActivity.baldPrefsUtils.hasChanged(page1EditorActivity)) {
                            page1EditorActivity.recreate();
                        }
                    }));

            if (app != null) {
                bt.setText(app.getLabel());
                AppsDatabaseHelper.loadPic(app, bt.imageView);
                viewsToApps.put(app, bt);
            }
            if (phone) {
                bt.setText(R.string.phone);
                bt.setImageResource(R.drawable.phone_on_button);
            }
        }
    }
}
