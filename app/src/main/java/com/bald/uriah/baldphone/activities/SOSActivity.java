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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.contacts.ContactsActivity;
import com.bald.uriah.baldphone.adapters.ContactRecyclerViewAdapter;
import com.bald.uriah.baldphone.databases.contacts.MiniContact;
import com.bald.uriah.baldphone.databases.home_screen_pins.HomeScreenPinHelper;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.RandomColorMaker;
import com.bald.uriah.baldphone.views.BaldPictureTextButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.bald.uriah.baldphone.databases.home_screen_pins.HomeScreenPinHelper.SHARED_PREFS_KEY;

public class SOSActivity extends BaldActivity {
    private static final int MAX_PINNED_CONTACTS = 2;
    private static final long HOLD_TO_CALL_MS = 5 * 1000;
    private static final long HOLD_TICK_MS = 50;

    private View btSos;
    private TextView sosSubtitle;
    private ProgressBar sosProgress;
    private View[] cards;
    private ImageView[] avatars;
    private TextView[] letters, names;
    private BaldPictureTextButton btAddContact;

    private int backgroundColor;
    private RandomColorMaker randomColorMaker;
    private long holdStartedAt = -1;

    private final Runnable holdTicker = new Runnable() {
        @Override
        public void run() {
            if (holdStartedAt < 0)
                return;
            final long elapsed = SystemClock.uptimeMillis() - holdStartedAt;
            if (elapsed >= HOLD_TO_CALL_MS) {
                resetHoldUi();
                callEmergencyNumber();
            } else {
                sosProgress.setProgress((int) elapsed);
                sosSubtitle.setText(getString(R.string.sos_calling_in,
                        (int) (HOLD_TO_CALL_MS - elapsed + 999) / 1000));
                btSos.postDelayed(this, HOLD_TICK_MS);
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkPermissions(this, requiredPermissions()))
            return;
        setContentView(R.layout.activity_sos);

        final TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.bald_background, typedValue, true);
        backgroundColor = typedValue.data;
        randomColorMaker = new RandomColorMaker(backgroundColor);

        btSos = findViewById(R.id.bt_sos);
        sosSubtitle = findViewById(R.id.sos_subtitle);
        sosProgress = findViewById(R.id.sos_progress);
        tintSosButton();

        cards = new View[]{findViewById(R.id.ec1), findViewById(R.id.ec2)};
        avatars = new ImageView[]{findViewById(R.id.ec1_avatar), findViewById(R.id.ec2_avatar)};
        letters = new TextView[]{findViewById(R.id.ec1_letter), findViewById(R.id.ec2_letter)};
        names = new TextView[]{findViewById(R.id.ec1_name), findViewById(R.id.ec2_name)};
        btAddContact = findViewById(R.id.bt_add_contact);
        btAddContact.getTextView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        btSos.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startHold();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    cancelHold(event.getActionMasked() == MotionEvent.ACTION_UP);
                    return true;
                default:
                    return true;
            }
        });
        setupYoutube(4);
    }

    /**
     * same trick as {@link com.bald.uriah.baldphone.views.BaldPictureTextButton}'s variant tint:
     * pressed state shows a 25% darker bald_danger
     */
    private void tintSosButton() {
        final TypedArray themeColors = obtainStyledAttributes(new int[]{R.attr.bald_danger});
        final int danger = themeColors.getColor(0, Color.RED);
        themeColors.recycle();
        final float[] hsv = new float[3];
        Color.colorToHSV(danger, hsv);
        hsv[2] *= 0.75f;
        btSos.setBackgroundTintList(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_pressed},
                        new int[]{}},
                new int[]{Color.HSVToColor(hsv), danger}));
    }

    private void startHold() {
        holdStartedAt = SystemClock.uptimeMillis();
        btSos.setPressed(true);
        sosProgress.setProgress(0);
        sosProgress.setVisibility(View.VISIBLE);
        btSos.post(holdTicker);
    }

    private void cancelHold(boolean released) {
        if (holdStartedAt < 0)
            return;
        final boolean earlyRelease = released &&
                SystemClock.uptimeMillis() - holdStartedAt < HOLD_TO_CALL_MS;
        holdStartedAt = -1;
        btSos.removeCallbacks(holdTicker);
        resetHoldUi();
        if (earlyRelease)
            BaldToast.longer(this);
    }

    private void resetHoldUi() {
        holdStartedAt = -1;
        btSos.setPressed(false);
        sosProgress.setVisibility(View.GONE);
        sosSubtitle.setText(R.string.sos_call_help);
    }

    @Override
    protected void onStart() {
        super.onStart();
        resetHoldUi();
        final List<MiniContact> miniContacts = PinHelper.getAllPinnedContacts(this);
        final int size = miniContacts == null ? 0 : miniContacts.size();
        for (int i = 0; i < MAX_PINNED_CONTACTS; i++)
            bindCard(i, size > i ? miniContacts.get(i) : null);

        btAddContact.setVisibility(size < MAX_PINNED_CONTACTS ? View.VISIBLE : View.GONE);
        btAddContact.setOnClickListener(v -> startActivity(new Intent(v.getContext().getApplicationContext(), ContactsActivity.class)
                .putExtra(ContactsActivity.INTENT_EXTRA_CONTACT_ADAPTER_MODE, ContactRecyclerViewAdapter.MODE_SOS)));
    }

    /**
     * photo card for one pinned SOS contact; initials on a hash-colored circle
     * when the contact has no photo, same trick as {@link ContactRecyclerViewAdapter}
     */
    private void bindCard(int i, @Nullable MiniContact miniContact) {
        if (miniContact == null) {
            cards[i].setVisibility(View.GONE);
            return;
        }
        cards[i].setVisibility(View.VISIBLE);
        names[i].setText(miniContact.name);
        if (miniContact.photo != null) {
            avatars[i].setImageURI(Uri.parse(miniContact.photo));
            letters[i].setVisibility(View.GONE);
        } else {
            letters[i].setText(ContactRecyclerViewAdapter.initials(miniContact.name));
            letters[i].setVisibility(View.VISIBLE);
            avatars[i].setImageDrawable(new ColorDrawable(colorful ?
                    randomColorMaker.generateColor(miniContact.lookupKey == null ? 0 : miniContact.lookupKey.hashCode()) :
                    backgroundColor));
        }
        cards[i].setOnClickListener(v -> DialerActivity.call(miniContact, v.getContext()));
    }

    private void callEmergencyNumber() {
        DialerActivity.call("112", this, true);//should work 99.99% of the times
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nothing, R.anim.slide_out_up);
    }

    public static class PinHelper {
        /**
         * @return true if succeeded
         */
        public static boolean pinContact(Context context, String lookupKey) {
            final SharedPreferences sharedPreferences = context.getSharedPreferences(HomeScreenPinHelper.PinnedContactPreferences.KEY, Context.MODE_PRIVATE);
            final Set<String> befSet = sharedPreferences.getStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, null);
            final Set<String> newSet;
            if (befSet == null)
                newSet = new HashSet<>();
            else {
                if (befSet.size() >= SOSActivity.MAX_PINNED_CONTACTS)
                    return false;
                else
                    newSet = new HashSet<>(befSet);
            }
            newSet.add(lookupKey);
            sharedPreferences.edit().putStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, newSet).apply();
            return true;
        }

        public static boolean isPinned(Context context, String lookupKey) {
            Set<String> set = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE).getStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, null);
            return set != null && set.contains(lookupKey);
        }

        public static void removeContact(Context context, String lookupKey) {
            final SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
            final Set<String> befSet = sharedPreferences.getStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, null);
            final Set<String> newSet;
            if (befSet == null)
                newSet = new HashSet<>();
            else
                newSet = new HashSet<>(befSet);
            newSet.remove(lookupKey);
            sharedPreferences.edit().putStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, newSet).apply();
        }

        /**
         * @param context
         * @return sorted list of lookup keys
         */
        private static List<MiniContact> getAllPinnedContacts(Context context) {
            final Set<String> lookupKeys = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE).getStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, null);
            if (lookupKeys == null)
                return null;
            final List<MiniContact> ret = new ArrayList<>(lookupKeys.size());
            final ContentResolver contentResolver = context.getContentResolver();
            for (String lookupKey :
                    lookupKeys) {
                try (Cursor cursor = contentResolver.query(
                        ContactsContract.Contacts.CONTENT_URI,
                        MiniContact.PROJECTION,
                        ContactsContract.Data.LOOKUP_KEY + " = ?",
                        new String[]{
                                lookupKey
                        }, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        ret.add(new MiniContact(
                                lookupKey,
                                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)),
                                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)),
                                cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID)),
                                cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.STARRED)) == 1
                        ));
                    } else {
                        removeContact(context, lookupKey);
                    }

                } catch (Exception e) {
                    android.util.Log.e("SOSActivity", "pinned contact lookup failed", e);
                }
            }
            Collections.sort(ret, (o1, o2) -> o1.name.compareTo(o2.name));
            return ret;
        }
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_CALL_PHONE | PERMISSION_READ_CONTACTS;
    }
}
