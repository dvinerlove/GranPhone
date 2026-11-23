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

package com.bald.uriah.baldphone.activities.contacts;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.BaldActivity;
import com.bald.uriah.baldphone.activities.DialerActivity;
import com.bald.uriah.baldphone.activities.SOSActivity;
import com.bald.uriah.baldphone.adapters.CallsRecyclerViewAdapter;
import com.bald.uriah.baldphone.adapters.ContactRecyclerViewAdapter;
import com.bald.uriah.baldphone.databases.calls.Call;
import com.bald.uriah.baldphone.databases.calls.CallLogsHelper;
import com.bald.uriah.baldphone.databases.contacts.Contact;
import com.bald.uriah.baldphone.databases.home_screen_pins.HomeScreenPinHelper;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.AppUtils;
import com.bald.uriah.baldphone.utils.RandomColorMaker;
import com.bald.uriah.baldphone.utils.Toggeler;
import com.bald.uriah.baldphone.views.BaldPictureTextButton;
import com.bald.uriah.baldphone.views.BaldSwitch;
import com.bald.uriah.baldphone.views.BaldTitleBar;
import com.bald.uriah.baldphone.views.ScrollingHelper;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Simple Activity for interacting with a {@link Contact}.
 * {@link #CONTACT_ID} or {@link #CONTACT_LOOKUP_KEY} must be added
 */
public class SingleContactActivity extends BaldActivity {
    private static final String TAG = SingleContactActivity.class.getSimpleName();
    public static final String CONTACT_ID = "CONTACT_ID";
    public static final String CONTACT_LOOKUP_KEY = "CONTACT_KEY";
    public static final String PIC_URI_EXTRA = "PIC_URI_EXTRA";
    /**
     * request check if deleted contact, if returns {@link Activity#RESULT_OK} contact was deleted.
     */
    public static final int REQUEST_CHECK_CHANGE = 97;
    public static boolean newPictureAdded = false;
    private boolean viaId = false;
    private String contactKeyExtra;
    private BaldTitleBar baldTitleBar;
    private ImageView contact_image;
    private TextView tv_contact_name, image_letter;
    private LinearLayout ll_phones, ll_dynamic;
    private BaldSwitch sos_switch, home_pin_switch;
    private View bt_call, bt_sms;
    private MediaPlayer mediaPlayer;
    private ContentResolver contentResolver;
    private LayoutInflater layoutInflater;
    private Contact contact; //final
    private boolean changed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_contact_activity);
        attachXml();
        contentResolver = getContentResolver();
        layoutInflater = getLayoutInflater();
        final Intent callingIntent = getIntent();
        viaId = callingIntent.hasExtra(CONTACT_ID);
        contactKeyExtra =
                viaId ?
                        callingIntent.getStringExtra(CONTACT_ID)
                        :
                        callingIntent.getStringExtra(CONTACT_LOOKUP_KEY)
        ;
        callingIntent.removeExtra(PIC_URI_EXTRA);

    }

    @Override
    protected void onStart() {
        super.onStart();

        ll_phones.removeAllViews();
        ll_dynamic.removeAllViews();

        try {
            contact = viaId ?
                    Contact.fromId(contactKeyExtra, contentResolver) :
                    Contact.fromLookupKey(contactKeyExtra, contentResolver);
        } catch (Contact.ContactNotFoundException e) {
            finish();
            return;
        }

        baldTitleBar.setTitle("");
        tv_contact_name.setText(contact.getName());
        setupBar();

        if (contact.hasPhone())
            inflatePhones();

        if (contact.hasWhatsapp())
            inflateWhatsapp();

        if (contact.hasMail())
            inflateMails();

        if (contact.hasAddress())
            inflateAddresses();

        if (newPictureAdded)
            new Handler().postDelayed(() -> {
                newPictureAdded = false;
                String uri = null;
                try (final Cursor cursor =
                             contentResolver.query(
                                     ContactsContract.Contacts.CONTENT_URI,
                                     new String[]{
                                             ContactsContract.Contacts.PHOTO_URI},
                                     (viaId ?
                                             ContactsContract.Contacts._ID :
                                             ContactsContract.Contacts.LOOKUP_KEY)
                                             + " = ?",
                                     new String[]{contactKeyExtra},
                                     null)) {
                    if (cursor.moveToFirst())
                        uri = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));
                }
                if (uri != null) {
                    loadPhoto(uri);
                } else
                    drawInitials();

            }, 1000);

        else {
            if (contact.hasPhoto())
                loadPhoto(contact.getPhoto());
            else
                drawInitials();
        }

        setupActions();

        final List<Call> callList =
                CallLogsHelper.getForSpecificContact(contentResolver, contact);
        if (!callList.isEmpty())
            inflateHistory(callList);

    }

    @Override
    protected void onStop() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        super.onStop();

    }

    private void attachXml() {
        baldTitleBar = findViewById(R.id.bald_title_bar);
        ll_phones = findViewById(R.id.ll_phones);
        ll_dynamic = findViewById(R.id.ll_dynamic);
        contact_image = findViewById(R.id.contact_image);
        image_letter = findViewById(R.id.image_letter);
        tv_contact_name = findViewById(R.id.tv_contact_name);
        // name can be a raw phone number — keep digits LTR in RTL locales
        tv_contact_name.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_LTR);
        sos_switch = findViewById(R.id.sos_switch);
        home_pin_switch = findViewById(R.id.home_pin_switch);
        bt_call = findViewById(R.id.bt_call);
        bt_sms = findViewById(R.id.bt_sms);

        ((BaldPictureTextButton) bt_call).getTextView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        for (int id : new int[]{R.id.bt_sms, R.id.bt_edit, R.id.bt_share, R.id.bt_delete})
            ((BaldPictureTextButton) findViewById(id)).getTextView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
    }

    private void inflatePhones() {
        final List<Pair<Integer, String>> phoneList = contact.getPhoneList(); //removing dup
        for (int i = 0, phoneListSize = phoneList.size(); i < phoneListSize - 1; i++) {
            final Pair<Integer, String> pair = phoneList.get(i);
            final Pair<Integer, String> next = phoneList.get(i + 1);
            if (pair.second
                    .replaceAll("[^0123456789]", "")
                    .equals(next.second
                            .replaceAll("[^0123456789]", ""))) {
                phoneList.remove(pair);
                phoneListSize--;
                i--;
            }
        }

        for (final Pair<Integer /*Type*/ , String> pair : contact.getPhoneList()) {
            final View layout = layoutInflater.inflate(R.layout.contact_number, ll_phones, false);
            final TextView tv_type = layout.findViewById(R.id.tv_type);
            final TextView tv_value = layout.findViewById(R.id.tv_value);

            tv_type.setText(ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    getResources(),
                    pair.first,
                    "Custom"));
            tv_value.setText(pair.second);
            tv_value.setTextDirection(View.TEXT_DIRECTION_LTR);
            layout.setOnClickListener(v -> DialerActivity.call(pair.second, this, false));

            ll_phones.addView(layout);
        }

    }

    private void inflateWhatsapp() {
        final List<String> whatappNumbers = contact.getWhatsappNumbers();
        for (String whatappNumber : whatappNumbers) {
            final View layout = layoutInflater.inflate(R.layout.contact_whatsapp, ll_dynamic, false);
            layout.setOnClickListener((v) ->
                    startActivity(
                            new Intent("android.intent.activate.MAIN")
                                    .setComponent(new ComponentName("com.whatsapp", "com.whatsapp.Conversation"))
                                    .putExtra("jid",
                                            PhoneNumberUtils.stripSeparators(
                                                    whatappNumber
                                                            .replaceAll(
                                                                    "[^0123456789]",
                                                                    "")
                                            ) + "@s.whatsapp.net")
                    )
            );
            ll_dynamic.addView(layout);
        }
    }

    private void inflateMails() {
        final List<String> mailList = contact.getMailList();
        for (int i = 0, mailListSize = mailList.size(); i < mailListSize - 1; i++) {
            final String phone = mailList.get(i);
            final String next = mailList.get(i + 1);
            if (phone.equals(next)) {
                mailList.remove(i);
                mailListSize--;
                i--;
            }
        }
        for (final String phone : contact.getMailList()) {
            final View layout = layoutInflater.inflate(R.layout.contact_mail, ll_dynamic, false);
            final TextView tv_value = layout.findViewById(R.id.tv_value);
            tv_value.setText(phone);
            layout.setOnClickListener(v -> {
                final Intent intent = new Intent(Intent.ACTION_SEND)
                        .setType("text/html")
                        .putExtra(Intent.EXTRA_EMAIL, phone);
                startActivity(intent);

            });
            ll_dynamic.addView(layout);
        }

    }

    private void setupActions() {
        final ImageView favorite = findViewById(R.id.bt_favorite);

        final ContentValues STAR = new ContentValues();
        STAR.put(ContactsContract.Contacts.STARRED, 1);
        final ContentValues UNSTAR = new ContentValues();
        UNSTAR.put(ContactsContract.Contacts.STARRED, 0);
        baldTitleBar.setGold(contact.isFavorite());
        favorite.setContentDescription(getString(contact.isFavorite() ?
                R.string.remove_from_favorite : R.string.add_to_favorite));
        Toggeler.newAdvancedImageToggeler(
                favorite,
                favorite,
                new int[]{R.drawable.star_on_button, R.drawable.star_remove_on_button},
                index -> {
                    final boolean fav = index == 1;
                    favorite.setContentDescription(getString(fav ?
                            R.string.remove_from_favorite : R.string.add_to_favorite));
                    contentResolver.update(
                            ContactsContract.Contacts.CONTENT_URI,
                            fav ? STAR : UNSTAR,
                            ContactsContract.Contacts.LOOKUP_KEY + "=?",
                            new String[]{contact.getLookupKey()});
                    baldTitleBar.setGold(fav);
                    changed = true;
                },
                contact.isFavorite() ? 1 : 0,
                1);

        sos_switch.setChecked(SOSActivity.PinHelper.isPinned(this, contact.getLookupKey()));
        sos_switch.setOnChangeListener(isChecked -> {
            if (isChecked) {
                if (!SOSActivity.PinHelper.pinContact(this, contact.getLookupKey())) {
                    sos_switch.setChecked(false);
                    DialogScreen.info(this, R.string.sos_is_full, R.string.sos_is_full_subtext);
                }
            } else {
                SOSActivity.PinHelper.removeContact(this, contact.getLookupKey());
            }
        });

        home_pin_switch.setChecked(HomeScreenPinHelper.isPinned(this, contact.getLookupKey()));
        home_pin_switch.setOnChangeListener(isChecked -> {
            if (isChecked)
                HomeScreenPinHelper.pinContact(this, contact.getLookupKey());
            else
                HomeScreenPinHelper.removeContact(this, contact.getLookupKey());
        });
    }

    private void inflateAddresses() {
        for (Pair<Integer, String[]> pair : contact.getAddressList()) {
            final View layout = layoutInflater.inflate(R.layout.contact_address, ll_dynamic, false);
            final TextView tv_addressType = layout.findViewById(R.id.tv_address);
            final TextView tv_value = layout.findViewById(R.id.tv_value);
            tv_addressType.setText(
                    String.format("%s %s",
                            String.valueOf(ContactsContract.CommonDataKinds.SipAddress.getTypeLabel(getResources(),
                                    pair.first,
                                    getText(R.string.custom)))
                            , String.valueOf(getText(R.string.address)
                            )
                    )
            );

            final StringBuilder stringBuilder = new StringBuilder();
            for (final String s : pair.second) {
                if (s == null || s.equals(""))
                    continue;

                stringBuilder.append(s).append(" ");

            }
            tv_value.setText(stringBuilder);
            layout.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + stringBuilder))));

            ll_dynamic.addView(layout);
        }
    }

    private void inflateHistory(List<Call> callList) {
        final View view = layoutInflater.inflate(R.layout.contact_history, ll_dynamic, false);
        final ScrollingHelper scrollingHelper = view.findViewById(R.id.scrolling_helper);
        final RecyclerView recyclerView = scrollingHelper.findViewById(R.id.child);
        final DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getDrawable(R.drawable.ll_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(new CallsRecyclerViewAdapter(callList, this));

        final BaldPictureTextButton show = view.findViewById(R.id.bt_show);
        Toggeler.newSimpleTextImageToggeler(
                show,
                show.getImageView(),
                show.getTextView(),
                R.drawable.drop_down_on_button,
                R.drawable.drop_up_on_button,
                R.string.show,
                R.string.hide,
                v -> scrollingHelper.setVisibility(View.VISIBLE),
                v -> scrollingHelper.setVisibility(View.GONE));
        ll_dynamic.addView(view);
    }

    private void setupBar() {
        final Intent editIntent =
                new Intent(this, AddContactActivity.class)
                        .putExtra(CONTACT_LOOKUP_KEY, contact.getLookupKey());
        final View.OnClickListener deleteListener = (v) ->
                AppUtils.showAreYouSureYouWantToDelete(contact.getName(), this, this::deleteContact);
        findViewById(R.id.bt_delete).setOnClickListener(deleteListener);
        findViewById(R.id.bt_edit).setOnClickListener(v -> v.getContext().startActivity(editIntent));

        final Uri vcardUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, contact.getLookupKey());
        final Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE)
                .putExtra(Intent.EXTRA_STREAM, vcardUri)
                .putExtra(Intent.EXTRA_SUBJECT, contact.getName());

        findViewById(R.id.bt_share).setOnClickListener(v -> {
                    changed = true;
                    AppUtils.share(this, shareIntent);
                }
        );

        if (contact.hasPhone()) {
            final String primary = contact.getMobilePhone() != null ?
                    contact.getMobilePhone() : contact.getPhoneList().get(0).second;
            bt_call.setVisibility(View.VISIBLE);
            bt_call.setOnClickListener(v -> DialerActivity.call(primary, this, false));
            bt_sms.setVisibility(View.VISIBLE);
            bt_sms.setOnClickListener(v -> AppUtils.sendMessage(primary, this));
        } else {
            bt_call.setVisibility(View.GONE);
            bt_sms.setVisibility(View.GONE);
        }
    }

    /**
     * initials on a hash-colored background, same trick as {@link ContactRecyclerViewAdapter}
     */
    private void drawInitials() {
        image_letter.setText(ContactRecyclerViewAdapter.initials(contact.getName()));
        image_letter.setVisibility(View.VISIBLE);
        final TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.bald_background, typedValue, true);
        contact_image.setImageDrawable(colorful ?
                new ColorDrawable(new RandomColorMaker(typedValue.data)
                        .generateColor(contact.getLookupKey() == null ? 0 : contact.getLookupKey().hashCode())) :
                new ColorDrawable(typedValue.data));
    }

    private void deleteContact() {
        contentResolver.delete(
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, contact.getLookupKey()),
                null,
                null);
        changed = true;
        finish();
    }

    @Override
    public void finish() {
        if (changed) setResult(RESULT_OK);
        super.finish();
    }

    private void loadPhoto(String uriToLoad) {
        if (AppUtils.isValidContextForGlide(contact_image.getContext())) {
            Glide.with(contact_image).load(uriToLoad).into(contact_image);
            image_letter.setVisibility(View.GONE);
        }
    }

    @Override
    public void startActivity(Intent intent) {
        try {
            super.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
            BaldToast.error(this);
        }
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_WRITE_CONTACTS | PERMISSION_READ_CONTACTS | PERMISSION_CALL_PHONE | PERMISSION_WRITE_CALL_LOG;
    }
}
