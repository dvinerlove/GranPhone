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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.adapters.ContactRecyclerViewAdapter;
import com.bald.uriah.baldphone.databases.contacts.Contact;
import com.bald.uriah.baldphone.databases.contacts.MiniContact;
import com.bald.uriah.baldphone.utils.CallManager;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.Const;
import com.bald.uriah.baldphone.views.ScrollingHelper;

import java.util.List;

import static android.media.AudioManager.STREAM_SYSTEM;

public class DialerActivity extends BaldActivity {
    private static final String TAG = DialerActivity.class.getSimpleName();
    public static final String SORT_ORDER =
            "upper(" + ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + ") ASC";
    private final static String[] PROJECTION =
            {ContactsContract.Data.DISPLAY_NAME, ContactsContract.Data._ID, ContactsContract.Contacts.PHOTO_URI, ContactsContract.Data.LOOKUP_KEY, ContactsContract.Data.STARRED, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.Data.CONTACT_ID};
    private static final String NUMBER_STATE = "NUMBER_STATE";
    private static final int TONE_DURATION = 300 * Const.MILLISECOND;
    private static final int TONE_VOLUME = 75; // 0-100

    private ToneGenerator dtmfGenerator;
    private ContentResolver contentResolver;
    private ContactRecyclerViewAdapter contactRecyclerViewAdapter;
    private RecyclerView recyclerView;
    private TextView tv_number, t9_chip;
    private View b_call, b_clear, b_hash, b_sulamit, b_backspace;
    private View[] numpad;
    private StringBuilder number = new StringBuilder();
    private boolean playDialSounds;

    // the actual transport: placeCall when default dialer, ACTION_CALL only for non-emergency
    // numbers otherwise (a non-default dialer can't ACTION_CALL emergency numbers — those fall
    // back to a prefilled system dialer). EXTRA_PHONE_ACCOUNT_HANDLE rides along only when the
    // user explicitly picked a SIM.
    @SuppressLint("NewApi")
    private static void call(final CharSequence number, final Context context, @Nullable SubscriptionInfo subscriptionInfo) {
        final Uri uri = telUri(number);
        try {
            PhoneAccountHandle accountHandle = null;
            if (subscriptionInfo != null) {
                accountHandle = resolveAccountHandle(context, subscriptionInfo);
                // fail closed: the user explicitly picked a SIM — silently calling on the wrong one is worse than not calling
                if (accountHandle == null) {
                    BaldToast.error(context);
                    return;
                }
            }
            if (CallManager.isDefaultDialer(context)) {
                final TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                final Bundle extras = new Bundle();
                if (accountHandle != null)
                    extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);
                telecomManager.placeCall(uri, extras);
            } else if (!isEmergencyNumber(number)) {
                final Intent intent = new Intent(Intent.ACTION_CALL).setData(uri);
                if (accountHandle != null)
                    intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);
                context.startActivity(intent);
            } else {
                context.startActivity(new Intent(Intent.ACTION_DIAL).setData(uri));
            }
        } catch (SecurityException e) {
            // e.g. the default-dialer role was revoked between check and placeCall — crashing the launcher mid-call-attempt helps no one
            Log.e(TAG, e.getMessage());
            BaldToast.error(context);
        }

    }

    public static void call(final CharSequence number, final Context context, final boolean directly) {
        if (!isDialable(number)) {
            DialogScreen.info(context, R.string.phone, R.string.enter_phone_number);
            return;
        }
        // the optional confirmation never gates emergency numbers or directly=true (SOS);
        // a snapshot — the dialer's StringBuilder keeps mutating underneath the dialog
        if (!directly && !isEmergencyNumber(number)
                && BPrefs.get(context).getBoolean(BPrefs.CONFIRM_OUTGOING_CALLS_KEY, BPrefs.CONFIRM_OUTGOING_CALLS_DEFAULT_VALUE)) {
            final String snapshot = number.toString();
            DialogScreen.confirm(context, context.getText(R.string.confirm_call_title), snapshot,
                    R.string.call, () -> startCall(snapshot, context, false));
            return;
        }
        startCall(number, context, directly);
    }

    // confirmation (if any) is already behind us here: dual-SIM picker first, actual call after
    private static void startCall(final CharSequence number, final Context context, final boolean directly) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            // the custom picker needs API 23+ (getCallCapablePhoneAccounts) and READ_PHONE_STATE to
            // enumerate subscriptions/accounts at all — without either, fall back to a plain call
            // with no explicit account rather than crashing or fake-offering a choice
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !directly
                    && BPrefs.get(context).getBoolean(BPrefs.DUAL_SIM_KEY, BPrefs.DUAL_SIM_DEFAULT_VALUE)
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                final SubscriptionManager subscriptionManager = (SubscriptionManager) context
                        .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                final List<SubscriptionInfo> activeSubscriptionInfoList;
                try {
                    activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                } catch (SecurityException e) {
                    // permission revoked between check and use — degrade to a plain call, no picker
                    call(number, context, null);
                    return;
                }
                if (activeSubscriptionInfoList != null && (activeSubscriptionInfoList.size() > 1) && subscriptionsResolvable(activeSubscriptionInfoList)) {
                    final CharSequence[] simNames = new CharSequence[activeSubscriptionInfoList.size()];
                    for (int i = 0; i < activeSubscriptionInfoList.size(); i++) {
                        simNames[i] = activeSubscriptionInfoList.get(i).getDisplayName();
                    }
                    DialogScreen.select(context, R.string.choose_sim,
                            0,
                            index -> call(number, context, activeSubscriptionInfoList.get(index)),
                            simNames);
                } else
                    call(number, context, null);
            } else
                call(number, context, null);
        } else
            // no direct-call permission at all — hand the prefilled number to the system dialer
            context.startActivity(new Intent(Intent.ACTION_DIAL).setData(telUri(number)));

    }

    /**
     * Whether a picked {@link SubscriptionInfo} can be mapped back to a {@link PhoneAccountHandle}
     * on this build. API 30+ has {@code TelephonyManager.getSubscriptionId(handle)}; below that the
     * ICCID match is the only mapping, and since API 29 SPL 2021-04-05 {@code getIccId()} is redacted
     * to "" for unprivileged apps — and {@code contains("")} matches the first account, i.e. silently
     * the wrong SIM. On patched API 29 there is no reliable public mapping at all: no picker.
     */
    private static boolean subscriptionsResolvable(final List<SubscriptionInfo> subscriptionInfoList) {
        if (android.os.Build.VERSION.SDK_INT != android.os.Build.VERSION_CODES.Q)
            return true;
        for (final SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
            final String iccId = subscriptionInfo.getIccId();
            if (iccId == null || iccId.isEmpty())
                return false;
        }
        return true;
    }

    // maps the picked subscription to its phone account; null when unmappable — callers must fail
    // closed, never default to the first account. READ_PHONE_STATE is guaranteed by the picker gate;
    // a race is caught by the caller's SecurityException handler.
    @Nullable
    @SuppressLint("NewApi")
    private static PhoneAccountHandle resolveAccountHandle(final Context context, final SubscriptionInfo subscriptionInfo) {
        final TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        final List<PhoneAccountHandle> list = telecomManager.getCallCapablePhoneAccounts();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                for (final PhoneAccountHandle phoneAccountHandle : list) {
                    if (telephonyManager.getSubscriptionId(phoneAccountHandle) == subscriptionInfo.getSubscriptionId())
                        return phoneAccountHandle;
                }
            }
        }
        // legacy mapping, only meaningful with a visible ICCID — an empty one would contains()-match the first account
        final String iccId = subscriptionInfo.getIccId();
        if (iccId != null && !iccId.isEmpty()) {
            for (final PhoneAccountHandle phoneAccountHandle : list) {
                if (phoneAccountHandle.getId().contains(iccId))
                    return phoneAccountHandle;
            }
        }
        return null;
    }

    private static Uri telUri(final CharSequence number) {
        return Uri.parse(("tel:" + number).replace("#", Uri.encode("#")));
    }

    @SuppressWarnings("deprecation") // TelephonyManager.isEmergencyNumber needs API 29+; this one still delegates to the same radio/network check on every version
    private static boolean isEmergencyNumber(final CharSequence number) {
        return PhoneNumberUtils.isEmergencyNumber(number.toString());
    }

    // blocks empty/too-short input and MMI codes ('#', '*') that would surface a foreign system dialog
    private static boolean isDialable(final CharSequence number) {
        if (number == null)
            return false;
        int digits = 0;
        for (int i = 0; i < number.length(); i++) {
            final char c = number.charAt(i);
            if (c == '#' || c == '*')
                return false;
            if (Character.isDigit(c))
                digits++;
        }
        return digits >= 2;
    }

    public static void call(final MiniContact miniContact, final Context context) {
        try {
            call(Contact.fromLookupKey(miniContact.lookupKey, context.getContentResolver()).getPhoneList().get(0).second, context, false);
        } catch (Exception e) {
            BaldToast.error(context);
        }
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkPermissions(this, requiredPermissions()))
            return;
        setContentView(R.layout.dialer);
        contentResolver = getContentResolver();
        playDialSounds = BPrefs.get(this).getBoolean(BPrefs.DIALER_SOUNDS_KEY, BPrefs.DIALER_SOUNDS_DEFAULT_VALUE) && !testing;
        if (playDialSounds)
            dtmfGenerator = new ToneGenerator(STREAM_SYSTEM, TONE_VOLUME);
        attachXml();
        setOnClickListeners();
        setupYoutube(2);
        // "tel:" links, e.g. emergency numbers redirected here by the system when GranPhone is the default phone app
        final Uri data = getIntent() == null ? null : getIntent().getData();
        if (savedInstanceState == null && data != null && "tel".equals(data.getScheme()) && data.getSchemeSpecificPart() != null)
            setNumber(data.getSchemeSpecificPart());
        else
            searchForContact();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(NUMBER_STATE, number);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final CharSequence charSequence = savedInstanceState.getCharSequence(NUMBER_STATE);
        if (charSequence != null) {
            setNumber(charSequence);
        }
    }

    private void getContactsByNumberFilter() {
        final Uri uri = number.length() == 0 ?
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI :
                Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(number.toString()));
        final Cursor contactsCursor = contentResolver.query(uri, PROJECTION, null, null, SORT_ORDER);
        final View sh = findViewById(R.id.scrolling_helper);
        if (sh instanceof ScrollingHelper) {
            ((ScrollingHelper) sh).setEmptyText(getString(number.length() == 0
                    ? R.string.no_contacts : R.string.no_contacts_found));
        }
        updateT9Chip(contactsCursor);
        if (contactRecyclerViewAdapter != null) {
            contactRecyclerViewAdapter.changeCursor(contactsCursor);
        } else {
            contactRecyclerViewAdapter = new ContactRecyclerViewAdapter(this, contactsCursor, recyclerView, ContactRecyclerViewAdapter.MODE_DEFAULT);
            recyclerView.setAdapter(contactRecyclerViewAdapter);
        }
    }

    // surfaces the first contact match as an «Имя — номер» chip under the number field
    private void updateT9Chip(final Cursor contactsCursor) {
        if (number.length() > 0 && contactsCursor != null && contactsCursor.moveToFirst()) {
            final String name = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            final String contactNumber = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            // name == contactNumber (nameless contacts) adds nothing over tv_number → hide
            final String text = name == null ? contactNumber
                    : contactNumber == null ? name
                    : name.equals(contactNumber) ? null
                    : name + " — " + contactNumber;
            if (text == null) {
                t9_chip.setVisibility(View.GONE);
            } else {
                t9_chip.setText(text);
                t9_chip.setVisibility(View.VISIBLE);
            }
        } else {
            t9_chip.setVisibility(View.GONE);
        }
    }

    private void attachXml() {
        recyclerView = findViewById(R.id.contacts_recycler_view);
        final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getDrawable(R.drawable.ll_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);

        tv_number = findViewById(R.id.tv_number);
        // typed digits always read LTR
        tv_number.setTextDirection(View.TEXT_DIRECTION_LTR);
        t9_chip = findViewById(R.id.t9_chip);
        numpad = new View[]{
                findViewById(R.id.b_0),
                findViewById(R.id.b_1),
                findViewById(R.id.b_2),
                findViewById(R.id.b_3),
                findViewById(R.id.b_4),
                findViewById(R.id.b_5),
                findViewById(R.id.b_6),
                findViewById(R.id.b_7),
                findViewById(R.id.b_8),
                findViewById(R.id.b_9)
        };
        b_call = findViewById(R.id.b_call);
        b_clear = findViewById(R.id.b_clear);
        b_backspace = findViewById(R.id.b_backspace);
        b_sulamit = findViewById(R.id.b_sulamit);
        b_hash = findViewById(R.id.b_hash);
    }

    private void setOnClickListeners() {
        for (char i = '0'; i <= '9'; i++)
            numpad[i - '0'].setOnClickListener(new DialerClickListener(i, i - '0'));
        b_sulamit.setOnClickListener(new DialerClickListener('*', ToneGenerator.TONE_DTMF_S));
        b_hash.setOnClickListener(new DialerClickListener('#', ToneGenerator.TONE_DTMF_P));

        b_call.setOnClickListener(v -> call(number, this, false));
        b_clear.setOnClickListener(v -> {
            number.setLength(0);
            tv_number.setText(number);
            searchForContact();
        });
        b_backspace.setOnClickListener(v -> {
            number.setLength(number.length() > 1 ? number.length() - 1 : 0);
            tv_number.setText(number);
            searchForContact();
        });
    }

    private void searchForContact() {
        getContactsByNumberFilter();
    }

    public void setNumber(@NonNull CharSequence charSequence) {
        number = new StringBuilder(charSequence);
        tv_number.setText(number);
        searchForContact();

    }

    private class DialerClickListener implements View.OnClickListener {
        private final char c;
        private final int tone;

        DialerClickListener(final char c, final int tone) {
            this.c = c;
            this.tone = tone;
        }

        @Override
        public void onClick(final View v) {
            number.append(c);
            tv_number.setText(number);
            searchForContact();
            if (playDialSounds)
                dtmfGenerator.startTone(tone, TONE_DURATION);
        }
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_READ_CONTACTS | PERMISSION_CALL_PHONE;
    }
}