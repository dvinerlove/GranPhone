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

package com.bald.uriah.baldphone.adapters;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.BaldActivity;
import com.bald.uriah.baldphone.activities.SOSActivity;
import com.bald.uriah.baldphone.activities.contacts.ShareActivity;
import com.bald.uriah.baldphone.activities.contacts.SingleContactActivity;
import com.bald.uriah.baldphone.fragments_and_dialogs.LetterChooserDialog;
import com.bald.uriah.baldphone.utils.AppUtils;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.RandomColorMaker;
import com.bald.uriah.baldphone.views.ModularRecyclerView;
import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Map;

public class ContactRecyclerViewAdapter extends ModularRecyclerView.ModularAdapter<ContactRecyclerViewAdapter.ViewHolder> {
    public final static String[] PROJECTION = {
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data._ID,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Data.STARRED};
    /**
     * For queries against {@code Data}/{@code Phone} URIs only — {@code contact_id} is
     * NOT a valid projection on {@code Contacts.CONTENT_URI}.
     */
    public final static String[] PROJECTION_WITH_CONTACT_ID = {
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data._ID,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Data.STARRED,
            ContactsContract.Data.CONTACT_ID};
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_SOS = 1;
    public static final int MODE_SHARE = 2;
    private final BaldActivity activity;
    private final LayoutInflater layoutInflater;
    private final Drawable letterContactBackground;
    private final SparseIntArray letterToPosition;
    private final RecyclerView recyclerView;
    private final int mode;
    private RandomColorMaker randomColorMaker;
    private Cursor cursor;
    private final Map<String, String> numbersCache = new HashMap<>();

    public ContactRecyclerViewAdapter(BaldActivity activity, Cursor cursor, RecyclerView recyclerView, @IntRange(from = MODE_DEFAULT, to = MODE_SHARE) int mode) {
        this.mode = mode;
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
        this.cursor = cursor;
        this.recyclerView = recyclerView;
        final TypedValue typedValue = new TypedValue();
        final Resources.Theme theme = activity.getTheme();
        theme.resolveAttribute(R.attr.bald_background, typedValue, true);
        this.letterContactBackground = new ColorDrawable(typedValue.data);
        this.randomColorMaker = new RandomColorMaker(typedValue.data);
        letterToPosition = new SparseIntArray();
        applyToCursor();
    }

    private void applyToCursor() {
        letterToPosition.clear();
        if (cursor == null || !cursor.moveToFirst())
            return;
        String previousFirstLetter = null;
        String tmpThis;

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            final String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            tmpThis = groupLetter(displayName);
            if (!tmpThis.equals(previousFirstLetter)) {
                letterToPosition.append(tmpThis.codePointAt(0), i);
            }
            previousFirstLetter = tmpThis;
        }
    }

    public void changeCursor(@NonNull Cursor cursor) {
        this.cursor = cursor;
        numbersCache.clear();
        applyToCursor();
        notifyDataSetChanged();

        if (getItemCount() > 0) {
            RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(activity) {
                @Override
                protected int getVerticalSnapPreference() {
                    return LinearSmoothScroller.SNAP_TO_START;
                }
            };
            smoothScroller.setTargetPosition(0);
            recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = layoutInflater.inflate(R.layout.contact_row_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final @NonNull ViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);
        cursor.moveToPosition(position);

        holder.lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY));
        final String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
        final String letter = groupLetter(name);
        holder.setFavorite(cursor.getInt(cursor.getColumnIndex(ContactsContract.Data.STARRED)) == 1);
        holder.tv_contact_name.setText(name);

        // Contacts cursors expose the contact id as _id; Data/Phone cursors as contact_id
        final int contactIdIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
        final String number = numberFor(contactIdIndex >= 0 ? cursor.getString(contactIdIndex) :
                cursor.getString(cursor.getColumnIndex(ContactsContract.Data._ID)));
        if (number == null) {
            holder.tv_contact_number.setVisibility(View.GONE);
        } else {
            holder.tv_contact_number.setText(number);
            holder.tv_contact_number.setVisibility(View.VISIBLE);
        }

        if (position == 0) {
            holder.setLetter(letter);
        } else {
            final boolean moved = cursor.moveToPosition(position - 1);
            if (moved) {
                final String prevName = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                final String prevNameFirstLetter = groupLetter(prevName);
                if (!prevNameFirstLetter.equals(letter)) {
                    holder.setLetter(letter);
                } else
                    holder.setLetter(null);
            } else {
                holder.setLetter(null);
            }
            cursor.moveToPosition(position);
        }
        if (cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)) != null) {
            holder.iv_contact_pic.setImageURI(Uri.parse(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI))));
            holder.tv_image_letter.setVisibility(View.GONE);
        } else {
            drawText(holder, initials(name), holder.lookupKey == null ? 0 : holder.lookupKey.hashCode());
        }

    }

    /**
     * Gutter letter for {@code name}: digit-leading names share one "#" group
     * instead of a separate header per first digit.
     */
    private static String groupLetter(@Nullable String name) {
        final String glyph = AppUtils.firstGlyph(name);
        return Character.isDigit(glyph.codePointAt(0)) ? "#" : glyph;
    }

    /**
     * first letters of the first two words, e.g. "John Smith" -> "JS"
     */
    public static String initials(@Nullable String name) {
        if (name == null)
            return "?";
        final String[] words = name.trim().split("\\s+");
        final StringBuilder ret = new StringBuilder(2);
        for (final String word : words) {
            if (word.isEmpty() || ret.length() >= 2)
                break;
            ret.append(AppUtils.firstGlyph(word));
        }
        return ret.length() == 0 ? "?" : ret.toString();
    }

    @Nullable
    private String numberFor(@Nullable String contactId) {
        if (contactId == null)
            return null;
        if (numbersCache.containsKey(contactId))
            return numbersCache.get(contactId);
        String number = null;
        try (final Cursor phones = activity.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactId},
                null)) {
            if (phones != null && phones.moveToFirst())
                number = phones.getString(0);
        } catch (Exception e) {
            // shortcut: a failed lookup just leaves the second line empty; a proper fix needs an async loader
        }
        numbersCache.put(contactId, number);
        return number;
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    private void drawText(ViewHolder viewHolder, String chr, int hash) {
        viewHolder.tv_image_letter.setVisibility(View.VISIBLE);
        viewHolder.tv_image_letter.setText(chr);
        Glide.with(viewHolder.iv_contact_pic).load(
                activity.colorful ?
                        new ColorDrawable(randomColorMaker.generateColor(hash)) :
                        letterContactBackground
        ).into(viewHolder.iv_contact_pic);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        static final int expandedSize = 133;
        static final int notExpandedSize = 80;

        final View line;
        final LinearLayout container, ll_contact_only;
        final TextView tv_contact_name, tv_contact_number, tv_letter, tv_image_letter;
        final ImageView iv_contact_pic;

        String lookupKey;
        private boolean expanded;
        private boolean favorite;

        public ViewHolder(final View itemView) {
            super(itemView);
            this.container = (LinearLayout) itemView;
            this.ll_contact_only = container.findViewById(R.id.ll_contact_only);
            this.tv_contact_name = container.findViewById(R.id.contact_name);
            this.tv_contact_number = container.findViewById(R.id.contact_number);
            this.iv_contact_pic = container.findViewById(R.id.profile_pic);
            this.tv_letter = container.findViewById(R.id.letter);
            this.ll_contact_only.setOnClickListener(this);
            this.line = container.findViewById(R.id.line);
            this.tv_image_letter = container.findViewById(R.id.image_letter);

            // phone numbers read digit-by-digit LTR even in RTL locales
            this.tv_contact_name.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_LTR);
            this.tv_contact_number.setTextDirection(View.TEXT_DIRECTION_LTR);

            this.tv_letter.setOnClickListener((v) -> {
                final LetterChooserDialog letterChooserDialog =
                        new LetterChooserDialog(activity, letterToPosition,
                                (position -> {
                                    ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(position, 0);
                                }
                                )
                        );
                letterChooserDialog.show();
                activity.autoDismiss(letterChooserDialog);

            });

        }

        public void setLetter(final @Nullable String character) {
            if (character == null && expanded) {
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) this.container.getLayoutParams();
                layoutParams.height =
                        (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                notExpandedSize,
                                activity.getResources().getDisplayMetrics());
                this.container.setLayoutParams(layoutParams);
                this.line.setVisibility(View.GONE);
                this.tv_letter.setVisibility(View.GONE);
                expanded = false;
            } else if (character != null && expanded) {
                this.tv_letter.setText(character);
            } else if (character != null) {
                final RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) this.container.getLayoutParams();
                layoutParams.height =
                        (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                expandedSize,
                                activity.getResources().getDisplayMetrics());
                this.container.setLayoutParams(layoutParams);
                this.line.setVisibility(View.VISIBLE);
                this.tv_letter.setVisibility(View.VISIBLE);
                this.tv_letter.setText(character);
                expanded = true;
            }
            // the letter alone reads as a stray character — tell TalkBack it opens the letter picker
            this.tv_letter.setContentDescription(character == null ? null :
                    activity.getString(R.string.cd_letters_with_char, character));
        }

        public void setFavorite(final boolean f) {
            if (f == favorite)
                return;
            favorite = f;
            ll_contact_only.setSelected(favorite);
        }

        @Override
        public void onClick(View v) {
            if (lookupKey == null)
                throw new IllegalStateException("lookupKey cannot be null!");
            switch (mode) {
                case MODE_DEFAULT:
                    final Intent intent = new Intent(activity, SingleContactActivity.class)
                            .putExtra(SingleContactActivity.CONTACT_LOOKUP_KEY, lookupKey);
                    activity.startActivityForResult(intent, SingleContactActivity.REQUEST_CHECK_CHANGE);
                    break;
                case MODE_SOS:
                    if (SOSActivity.PinHelper.pinContact(v.getContext(), lookupKey))
                        activity.finish();
                    else
                        DialogScreen.info(activity, R.string.sos_is_full, R.string.sos_is_full_subtext);
                    break;
                case MODE_SHARE:
                    ((ShareActivity) activity).whatsappShare(lookupKey);
                    break;
            }
        }
    }
}