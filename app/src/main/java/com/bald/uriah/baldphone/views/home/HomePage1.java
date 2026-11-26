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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
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
import com.bald.uriah.baldphone.activities.contacts.SingleContactActivity;
import com.bald.uriah.baldphone.activities.media.PhotosActivity;
import com.bald.uriah.baldphone.activities.media.VideosActivity;
import com.bald.uriah.baldphone.activities.pills.PillsActivity;
import com.bald.uriah.baldphone.databases.apps.App;
import com.bald.uriah.baldphone.databases.apps.AppsDatabase;
import com.bald.uriah.baldphone.databases.apps.AppsDatabaseHelper;
import com.bald.uriah.baldphone.databases.calls.CallLogsHelper;
import com.bald.uriah.baldphone.databases.contacts.MiniContact;
import com.bald.uriah.baldphone.databases.home_screen_pins.HomeScreenPinHelper;
import com.bald.uriah.baldphone.services.NotificationListenerService;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.Const;
import com.bald.uriah.baldphone.utils.AppUtils;
import com.bald.uriah.baldphone.views.FirstPageAppIcon;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.HashSet;
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
    private final static String HIDDEN_TILE = HomeScreenPinHelper.HIDDEN_TILE;
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
        installClockCardFit();
        return view;
    }

    /**
     * TextClock has no autosize on minSdk 21 — shrink it manually once the parent's
     * real width is known, same pattern as {@link com.bald.uriah.baldphone.views.BaldMultipleSelection}.
     * Only ever shrinks (never grows): at extreme system fontScale the clock/date
     * stay fully readable instead of ellipsizing into uselessness.
     */
    /**
     * Shrinks clock/date ONLY when the card is squeezed by visible tile rows —
     * with 1–3 rows the base sizes stay untouched, and hiding rows restores
     * them, since every pass recomputes from the base size rather than
     * shrinking further. The clock always yields room for the date, so the
     * date can never get pushed out of the card.
     */
    private void installClockCardFit() {
        final View card = view.findViewById(R.id.clock_card);
        final TextView clock = card.findViewById(R.id.clock);
        final TextView date = card.findViewById(R.id.date);
        final float clockBase = clock.getTextSize();
        final float dateBase = date.getTextSize();
        final float clockFloor = getResources().getDimension(R.dimen.huge);
        final float dateFloor = getResources().getDimension(R.dimen.tiny);
        card.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            final float availW = card.getWidth() - card.getPaddingStart() - card.getPaddingEnd();
            final float availH = card.getHeight() - card.getPaddingTop() - card.getPaddingBottom();
            if (availW <= 0 || availH <= 0)
                return;
            fitText(date, dateBase, availW, availH - clockFloor, dateFloor);
            fitText(clock, clockBase, availW, availH - date.getHeight(), clockFloor);
        });
    }

    /** Sizes {@code tv} to the largest size ≤ {@code base} that fits width and height, never below {@code floor}. */
    private static void fitText(final TextView tv, final float base, final float availW,
                                final float availH, final float floor) {
        final CharSequence text = tv.getText();
        if (availH <= 0 || text == null || text.length() == 0)
            return;
        final android.text.TextPaint paint = tv.getPaint();
        final float prevSize = paint.getTextSize();
        paint.setTextSize(base);
        float scale = 1f;
        final float wantedW = paint.measureText(text.toString());
        if (wantedW > availW)
            scale = availW / wantedW;
        final float lineH = paint.descent() - paint.ascent();
        if (lineH > availH)
            scale = Math.min(scale, availH / lineH);
        paint.setTextSize(prevSize);
        final float target = Math.max(floor, base * scale * 0.98f);
        if (Math.abs(target - tv.getTextSize()) > 1f)
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, target);
    }

    /**
     * Re-binds every tile slot from preferences — called when the home screen
     * resumes so pins made elsewhere (SingleContactActivity, AppsActivity,
     * Page1EditorActivity) show up without recreating the view.
     */
    public void refreshTiles() {
        if (view == null)
            return;
        genOnClickListeners();
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
        for (final String action : new String[]{
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA,
                MediaStore.ACTION_IMAGE_CAPTURE,
                MediaStore.ACTION_VIDEO_CAPTURE}) {
            final Intent intent = new Intent(action);
            if (activity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null)
                return intent;
        }
        return null;
    }

    private boolean cameraPermissionGranted() {
        return ActivityCompat.checkSelfPermission(homeScreen, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean captureIntent(Intent intent) {
        return MediaStore.ACTION_IMAGE_CAPTURE.equals(intent.getAction())
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(intent.getAction());
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
        HomeScreenPinHelper.migrateLegacyPins(activity);
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
                    BaldToast.from(homeScreen).setType(BaldToast.TYPE_ERROR).setText(R.string.no_messages_app).show();
                    return;
                }
                final Intent launchIntent = homeScreen.getPackageManager().getLaunchIntentForPackage(smsPackage);
                if (launchIntent != null) {
                    homeScreen.startActivity(launchIntent);
                    return;
                }
                final Intent sendIntent = new Intent(Intent.ACTION_SENDTO)
                        .setData(Uri.parse("smsto:"))
                        .setPackage(smsPackage);
                if (homeScreen.getPackageManager().resolveActivity(sendIntent, PackageManager.MATCH_DEFAULT_ONLY) != null)
                    homeScreen.startActivity(sendIntent);
                else
                    BaldToast.from(homeScreen).setType(BaldToast.TYPE_ERROR).setText(R.string.no_messages_app).show();

            } catch (Exception e) {
                BaldToast.from(homeScreen).setType(BaldToast.TYPE_ERROR).setText(R.string.an_error_has_occurred).show();
            }
        });
        setupButton(BPrefs.CUSTOM_PHOTOS_KEY, bt_photos, v -> homeScreen.startActivity(new Intent(homeScreen, PhotosActivity.class)));
        setupButton(BPrefs.CUSTOM_CAMERA_KEY, bt_camera, v -> {
            try {
                final Intent intent = getCameraIntent();
                if (intent == null)
                    BaldToast.from(homeScreen).setType(BaldToast.TYPE_ERROR).setText(R.string.no_camera_app).show();
                else if (captureIntent(intent) && !cameraPermissionGranted())
                    homeScreen.requestCameraPermission();
                else
                    homeScreen.startActivity(intent);
            } catch (SecurityException e) {
                if (!cameraPermissionGranted())
                    homeScreen.requestCameraPermission();
                else
                    BaldToast.from(homeScreen).setType(BaldToast.TYPE_ERROR).setText(R.string.an_error_has_occurred).show();
            } catch (ActivityNotFoundException e) {
                BaldToast.from(homeScreen).setType(BaldToast.TYPE_ERROR).setText(R.string.no_camera_app).show();
            }
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
        int firstVisible = -1, lastVisible = -1;
        for (int i = 0; i < row.getChildCount(); i++) {
            final View child = row.getChildAt(i);
            if (child instanceof FirstPageAppIcon && child.getVisibility() == View.VISIBLE) {
                if (firstVisible == -1)
                    firstVisible = i;
                lastVisible = i;
            }
        }
        if (firstVisible == -1) {
            row.setVisibility(View.GONE);
            return;
        }
        // dividers are plain <View> includes — show one only between two visible icons
        for (int i = 0; i < row.getChildCount(); i++) {
            final View child = row.getChildAt(i);
            if (child.getClass() == View.class)
                child.setVisibility(i > firstVisible && i < lastVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void updateFlashIcon() {
        if (homeScreen == null)
            return;
        // only while the slot shows its default flashlight tile — an assigned
        // app/contact owns the icon and the posted call must not clobber it.
        if (sharedPreferences.getString(BPrefs.CUSTOM_ASSISTANT_KEY, null) != null)
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
        final MiniContact pinContact;
        if (prefValue != null && prefValue.startsWith(HomeScreenPinHelper.CONTACT_TILE_PREFIX)) {
            pinContact = HomeScreenPinHelper.resolveContact(activity,
                    prefValue.substring(HomeScreenPinHelper.CONTACT_TILE_PREFIX.length()));
            if (pinContact == null) {
                // contact was deleted — free the slot instead of leaving a dead tile
                sharedPreferences.edit().putString(bPrefsKey, HIDDEN_TILE).apply();
                if (homeScreen != null) {
                    bt.setVisibility(View.GONE);
                    return;
                }
            }
        } else pinContact = null;

        final App app;
        boolean phone = false;
        if (pinContact != null) {
            app = null;
        } else if (bt == bt_whatsapp && BuildConfig.FLAVOR.equals("gPlay") && !sharedPreferences.contains(bPrefsKey)) {
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
            bt.setVisibility(View.VISIBLE); // slot may have been GONE from a previous bind
            if (pinContact != null) {
                bt.resetToDefault();
                bt.setText(pinContact.name);
                if (AppUtils.isValidContextForGlide(bt.imageView.getContext()))
                    Glide.with(bt.imageView)
                            .load(pinContact.photo)
                            .apply(new RequestOptions().error(R.drawable.face_on_button))
                            .into(bt.imageView);
                bt.setOnClickListener(v -> activity.startActivity(
                        new Intent(activity, SingleContactActivity.class)
                                .putExtra(SingleContactActivity.CONTACT_LOOKUP_KEY, pinContact.lookupKey)));
            } else if (app == null) {
                if (phone) {
                    bt.setOnClickListener(v -> activity.startActivity(AppUtils.getPhoneIntent(activity)));
                    bt.setText(R.string.phone);
                    bt.setImageResource(R.drawable.phone_on_button);
                } else {
                    // slot reverted to its default — undo any app/contact leftovers
                    bt.resetToDefault();
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
            if (pinContact != null) {
                bt.setText(pinContact.name);
                if (AppUtils.isValidContextForGlide(bt.imageView.getContext()))
                    Glide.with(bt.imageView)
                            .load(pinContact.photo)
                            .apply(new RequestOptions().error(R.drawable.face_on_button))
                            .into(bt.imageView);
            }
            final CharSequence initialAppName;
            if (pinContact != null) {
                // option 0 restores the slot's default tile — label it accordingly
                initialAppName = bt.getDefaultText();
            } else if (BuildConfig.FLAVOR.equals("gPlay")) {
                if (bt == bt_whatsapp) {
                    initialAppName = activity.getString(R.string.edit_home_screen);
                } else if (bt == bt_recent)
                    initialAppName = activity.getString(R.string.phone);
                else initialAppName = bt.getText();
            } else {
                initialAppName = bt.getText();
            }
            bt.setOnClickListener(v -> DialogScreen.select(activity, R.string.custom_app,
                            hidden ? 2 : (pinContact != null ? 0 : (sharedPreferences.contains(bPrefsKey) ? 1 : 0)),
                            index -> {
                                if (index == 0)
                                    sharedPreferences.edit().remove(bPrefsKey).apply();
                                else if (index == 1)
                                    activity.startActivityForResult(new Intent(activity, AppsActivity.class).putExtra(AppsActivity.CHOOSE_MODE, bPrefsKey), AppsActivity.REQUEST_SELECT_CUSTOM_APP);
                                else
                                    sharedPreferences.edit().putString(bPrefsKey, HIDDEN_TILE).apply();
                            },
                            initialAppName, activity.getText(R.string.choose_another_app), activity.getText(R.string.hide))
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
