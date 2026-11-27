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
import android.widget.LinearLayout;
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
import com.bald.uriah.baldphone.utils.BaldExecutors;
import com.bald.uriah.baldphone.utils.GenerationGuard;
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
    private final static int[] TILE_ROW_IDS = {R.id.phone_container, R.id.messages_container, R.id.media_container, R.id.time_container};
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
    // lazy: HomeView ctor calls onCreateView before subclass field initializers run
    private GenerationGuard tilesGuard;

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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        resizeTileRows(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * The clock keeps its measured content height; visible peer rows share whatever
     * viewport remains, but never go below the accessibility floor. If the floor
     * does not fit, the enclosing ModularScrollView takes over instead of squeezing.
     */
    private void resizeTileRows(final int width, final int viewportHeight) {
        if (view == null || viewportHeight <= 0)
            return;
        final View content = view.findViewById(R.id.page1_content);
        final View clockCard = view.findViewById(R.id.clock_card);
        if (content == null || clockCard == null)
            return;

        int visibleRows = 0, rowMargins = 0;
        for (final int id : TILE_ROW_IDS) {
            final View row = view.findViewById(id);
            if (row.getVisibility() == View.GONE)
                continue;
            final LinearLayout.LayoutParams rowParams = (LinearLayout.LayoutParams) row.getLayoutParams();
            visibleRows++;
            rowMargins += rowParams.topMargin + rowParams.bottomMargin;
        }
        if (visibleRows == 0)
            return;

        final int clockHeight = measureClockCard(clockCard, content, width);
        final int available = viewportHeight
                - content.getPaddingTop() - content.getPaddingBottom()
                - clockHeight - rowMargins;
        final int rowHeight = Math.max(Math.max(0, available / visibleRows), measureTileRowFloor());
        for (final int id : TILE_ROW_IDS) {
            final View row = view.findViewById(id);
            final LinearLayout.LayoutParams rowParams = (LinearLayout.LayoutParams) row.getLayoutParams();
            rowParams.height = rowHeight;
        }
    }

    /** Measures the clock as wrap-content at the grid width, including its own margins. */
    private int measureClockCard(final View clockCard, final View content, final int gridWidth) {
        final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) clockCard.getLayoutParams();
        final int horizontalMargins = params.leftMargin + params.rightMargin;
        final int cardWidth = gridWidth - content.getPaddingLeft() - content.getPaddingRight() - horizontalMargins;
        if (cardWidth > 0)
            clockCard.measure(
                    MeasureSpec.makeMeasureSpec(cardWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        return clockCard.getMeasuredHeight() + params.topMargin + params.bottomMargin;
    }

    /** Floor is the greater of 88dp and the measured icon + max-two-line label at the active fontScale. */
    private int measureTileRowFloor() {
        int floor = getResources().getDimensionPixelSize(R.dimen.home_tile_row_min_height);
        for (final int rowId : TILE_ROW_IDS) {
            final ViewGroup row = view.findViewById(rowId);
            if (row.getVisibility() == View.GONE)
                continue;
            for (int i = 0; i < row.getChildCount(); i++) {
                final View child = row.getChildAt(i);
                if (!(child instanceof FirstPageAppIcon) || child.getVisibility() != View.VISIBLE)
                    continue;
                final FirstPageAppIcon tile = (FirstPageAppIcon) child;
                final ViewGroup.LayoutParams labelParams = tile.textView.getLayoutParams();
                int labelHeight = tile.textView.getLineHeight() * Math.max(1, tile.textView.getMaxLines());
                if (labelParams instanceof ViewGroup.MarginLayoutParams) {
                    final ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) labelParams;
                    labelHeight += margins.topMargin + margins.bottomMargin;
                }
                final int iconHeight = tile.imageView.getLayoutParams().height > 0
                        ? tile.imageView.getLayoutParams().height
                        : getResources().getDimensionPixelSize(R.dimen.icon_md);
                floor = Math.max(floor, iconHeight + labelHeight);
            }
        }
        return floor;
    }

    /**
     * TextClock has no autosize on minSdk 21 — shrink it manually once the parent's
     * real width is known. Height is deliberately excluded: the card is wrap-content
     * and vertical overflow belongs to the scroll fallback, not to smaller clock text.
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
            if (availW <= 0)
                return;
            fitTextWidth(date, dateBase, availW, dateFloor);
            fitTextWidth(clock, clockBase, availW, clockFloor);
        });
    }

    /** Sizes {@code tv} to the largest size ≤ {@code base} that fits width, never below {@code floor}. */
    private static void fitTextWidth(final TextView tv, final float base, final float availW, final float floor) {
        final CharSequence text = tv.getText();
        if (text == null || text.length() == 0)
            return;
        final android.text.TextPaint paint = tv.getPaint();
        final float prevSize = paint.getTextSize();
        paint.setTextSize(base);
        float scale = 1f;
        final float wantedW = paint.measureText(text.toString());
        if (wantedW > availW)
            scale = availW / wantedW;
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
        bt_assistant.post(this::updateFlashIcon);
        if (tilesGuard == null)
            tilesGuard = new GenerationGuard();
        final int generation = tilesGuard.next();
        final Context appContext = activity.getApplicationContext();
        BaldExecutors.db().execute(() -> {
            // migration and resolution share the single db thread, so pins migrated
            // here are always visible to the slot resolution below - no race.
            HomeScreenPinHelper.migrateLegacyPins(appContext);
            final ArrayMap<String, SlotData> resolved = new ArrayMap<>();
            for (final String key : HomeScreenPinHelper.TILE_PREF_KEYS)
                resolved.put(key, resolveSlot(appContext, key));
            BaldExecutors.postToMain(() -> {
                if (!tilesGuard.isCurrent(generation) || view == null || activity.isFinishing() || activity.isDestroyed())
                    return;
                viewsToApps.clear();
                bindTiles(resolved);
            });
        });
    }

    /** Resolves one tile slot's backing data off the main thread; performs the dead-pin prefs cleanup. */
    private SlotData resolveSlot(Context context, String key) {
        final SlotData data = new SlotData();
        data.prefValue = sharedPreferences.getString(key, null);
        data.hidden = HIDDEN_TILE.equals(data.prefValue);
        if (data.prefValue != null && data.prefValue.startsWith(HomeScreenPinHelper.CONTACT_TILE_PREFIX)) {
            data.contactTile = true;
            data.contact = HomeScreenPinHelper.resolveContact(context,
                    data.prefValue.substring(HomeScreenPinHelper.CONTACT_TILE_PREFIX.length()));
            if (data.contact == null)
                sharedPreferences.edit().putString(key, HIDDEN_TILE).apply();
            return data;
        }
        if (BPrefs.CUSTOM_APP_KEY.equals(key) && BuildConfig.FLAVOR.equals("gPlay") && !sharedPreferences.contains(key)) {
            data.app = AppsDatabase.getInstance(context).appsDatabaseDao()
                    .findByFlattenComponentName(baldComponentNameBeginning + Page1EditorActivity.class.getName());
        } else if (BPrefs.CUSTOM_RECENTS_KEY.equals(key) && BuildConfig.FLAVOR.equals("gPlay") && !sharedPreferences.contains(key)) {
            data.phone = true;
        } else if (data.prefValue != null && !data.hidden) {
            data.app = AppsDatabase.getInstance(context).appsDatabaseDao().findByFlattenComponentName(data.prefValue);
            if (data.app == null)
                sharedPreferences.edit().remove(key).apply();
        }
        return data;
    }

    /** Per-bind snapshot of a tile slot, filled on the db thread, read on main. */
    private static final class SlotData {
        String prefValue;
        boolean hidden;
        boolean contactTile;
        MiniContact contact;
        App app;
        boolean phone;
    }

    private void bindTiles(ArrayMap<String, SlotData> resolved) {
        setupButton(BPrefs.CUSTOM_RECENTS_KEY, bt_recent, resolved.get(BPrefs.CUSTOM_RECENTS_KEY), v -> homeScreen.startActivity(new Intent(homeScreen, RecentActivity.class)));
        setupButton(BPrefs.CUSTOM_DIALER_KEY, bt_dialer, resolved.get(BPrefs.CUSTOM_DIALER_KEY), v -> homeScreen.startActivity(new Intent(homeScreen, DialerActivity.class)));
        setupButton(BPrefs.CUSTOM_CONTACTS_KEY, bt_contacts, resolved.get(BPrefs.CUSTOM_CONTACTS_KEY), v -> homeScreen.startActivity(new Intent(homeScreen, ContactsActivity.class)));
        setupButton(BPrefs.CUSTOM_APP_KEY, bt_whatsapp, resolved.get(BPrefs.CUSTOM_APP_KEY), v -> homeScreen.startActivity(new Intent(homeScreen, SOSActivity.class)));
        setupButton(BPrefs.CUSTOM_ASSISTANT_KEY, bt_assistant, resolved.get(BPrefs.CUSTOM_ASSISTANT_KEY), v -> {
            homeScreen.toggleTorch();
            updateFlashIcon();
        });
        setupButton(BPrefs.CUSTOM_MESSAGES_KEY, bt_messages, resolved.get(BPrefs.CUSTOM_MESSAGES_KEY), v -> {
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
                BaldToast.error(homeScreen, R.string.messages_could_not_open);
            }
        });
        setupButton(BPrefs.CUSTOM_PHOTOS_KEY, bt_photos, resolved.get(BPrefs.CUSTOM_PHOTOS_KEY), v -> homeScreen.startActivity(new Intent(homeScreen, PhotosActivity.class)));
        setupButton(BPrefs.CUSTOM_CAMERA_KEY, bt_camera, resolved.get(BPrefs.CUSTOM_CAMERA_KEY), v -> {
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
                    BaldToast.error(homeScreen, R.string.camera_could_not_open);
            } catch (ActivityNotFoundException e) {
                BaldToast.from(homeScreen).setType(BaldToast.TYPE_ERROR).setText(R.string.no_camera_app).show();
            }
        });
        setupButton(BPrefs.CUSTOM_VIDEOS_KEY, bt_videos, resolved.get(BPrefs.CUSTOM_VIDEOS_KEY), v -> homeScreen.startActivity(new Intent(homeScreen, VideosActivity.class)));
        setupButton(BPrefs.CUSTOM_PILLS_KEY, bt_reminders, resolved.get(BPrefs.CUSTOM_PILLS_KEY), v -> homeScreen.startActivity(new Intent(homeScreen, PillsActivity.class)));
        setupButton(BPrefs.CUSTOM_APPS_KEY, bt_apps, resolved.get(BPrefs.CUSTOM_APPS_KEY), v -> {
            if (!homeScreen.finishedUpdatingApps)
                homeScreen.launchAppsActivity = true;
            else
                homeScreen.startActivity(new Intent(HomePage1.this.homeScreen, AppsActivity.class));
        });
        setupButton(BPrefs.CUSTOM_ALARMS_KEY, bt_clock, resolved.get(BPrefs.CUSTOM_ALARMS_KEY), v -> homeScreen.startActivity(new Intent(homeScreen, AlarmsActivity.class)));
        if (homeScreen != null)
            for (int id : TILE_ROW_IDS)
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
        row.setVisibility(View.VISIBLE);
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

    private void setupButton(String bPrefsKey, FirstPageAppIcon bt, SlotData data, View.OnClickListener onClickListener) {
        final boolean hidden = data.hidden;
        if (hidden) {
            if (homeScreen != null) {
                bt.setVisibility(View.GONE);
                return;
            }
            bt.setAlpha(0.3f);
        }
        final MiniContact pinContact = data.contact;
        if (data.contactTile && pinContact == null) {
            // contact was deleted - resolve already freed the slot; mirror the dead-tile early-out
            if (homeScreen != null) {
                bt.setVisibility(View.GONE);
                return;
            }
        }

        final App app;
        boolean phone = false;
        if (pinContact != null) {
            app = null;
        } else if (data.phone) {
            app = null;
            phone = true;
        } else app = data.app;
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
