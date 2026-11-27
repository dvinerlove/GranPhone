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

package com.bald.uriah.baldphone.databases.home_screen_pins;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.ContactsContract;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.databases.apps.App;
import com.bald.uriah.baldphone.databases.apps.AppsDatabase;
import com.bald.uriah.baldphone.databases.contacts.MiniContact;
import com.bald.uriah.baldphone.utils.BPrefs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Home-page tile ownership. Every tile slot on page 1 is a {@link BPrefs#CUSTOM_*_KEY}
 * preference whose value is one of: unset (slot's default action), {@link #HIDDEN_TILE}
 * (free slot), an app's flattenComponentName, or {@link #CONTACT_TILE_PREFIX}+lookupKey
 * (a pinned contact). Pinned contacts/apps therefore share the exact same grid as
 * every other tile — no separate container.
 */
public class HomeScreenPinHelper {
    public static final String SHARED_PREFS_KEY = PinnedContactPreferences.KEY;
    public static final String SET_KEY = PinnedContactPreferences.SET_KEY;
    /** Slot-pref value meaning "no tile here" — the slot a pin may claim. */
    public static final String HIDDEN_TILE = "__HIDDEN__";
    /** Slot-pref prefix marking a pinned contact (the rest is its lookup key). */
    public static final String CONTACT_TILE_PREFIX = "contact:";

    /** Tile slots in visual row order — the order free slots are handed out in. */
    public static final String[] TILE_PREF_KEYS = {
            BPrefs.CUSTOM_RECENTS_KEY, BPrefs.CUSTOM_DIALER_KEY, BPrefs.CUSTOM_CONTACTS_KEY,
            BPrefs.CUSTOM_APP_KEY, BPrefs.CUSTOM_ASSISTANT_KEY, BPrefs.CUSTOM_MESSAGES_KEY,
            BPrefs.CUSTOM_PHOTOS_KEY, BPrefs.CUSTOM_CAMERA_KEY, BPrefs.CUSTOM_VIDEOS_KEY,
            BPrefs.CUSTOM_PILLS_KEY, BPrefs.CUSTOM_APPS_KEY, BPrefs.CUSTOM_ALARMS_KEY,
    };

    private static SharedPreferences tilePrefs(Context context) {
        return BPrefs.get(context);
    }

    @Nullable
    private static String findFreeSlot(SharedPreferences prefs) {
        for (String key : TILE_PREF_KEYS)
            if (HIDDEN_TILE.equals(prefs.getString(key, null)))
                return key;
        return null;
    }

    @Nullable
    private static String findSlotWith(SharedPreferences prefs, String value) {
        for (String key : TILE_PREF_KEYS)
            if (value.equals(prefs.getString(key, null)))
                return key;
        return null;
    }

    /**
     * Pins a contact into the first free tile slot.
     *
     * @return false when no slot is free — the caller must revert its switch.
     */
    public static boolean pinContact(Context context, String lookupKey) {
        final SharedPreferences prefs = tilePrefs(context);
        if (findSlotWith(prefs, CONTACT_TILE_PREFIX + lookupKey) != null)
            return true;
        final String free = findFreeSlot(prefs);
        if (free == null)
            return false;
        prefs.edit().putString(free, CONTACT_TILE_PREFIX + lookupKey).apply();
        return true;
    }

    public static void removeContact(Context context, String lookupKey) {
        final SharedPreferences prefs = tilePrefs(context);
        final String slot = findSlotWith(prefs, CONTACT_TILE_PREFIX + lookupKey);
        if (slot != null)
            prefs.edit().putString(slot, HIDDEN_TILE).apply();
        // also drop any un-migrated legacy pin for this contact
        final SharedPreferences legacy = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        final Set<String> befSet = legacy.getStringSet(SET_KEY, null);
        if (befSet != null && befSet.contains(lookupKey)) {
            final Set<String> newSet = new HashSet<>(befSet);
            newSet.remove(lookupKey);
            legacy.edit().putStringSet(SET_KEY, newSet).apply();
        }
    }

    public static boolean isPinned(Context context, String lookupKey) {
        if (findSlotWith(tilePrefs(context), CONTACT_TILE_PREFIX + lookupKey) != null)
            return true;
        // pre-slot-format pins that found no free slot still count as pinned
        final Set<String> set = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE).getStringSet(SET_KEY, null);
        return set != null && set.contains(lookupKey);
    }

    /** @return MiniContact for a lookup key, or null if the contact is gone/unreadable. */
    @Nullable
    public static MiniContact resolveContact(Context context, String lookupKey) {
        try (Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                MiniContact.PROJECTION,
                ContactsContract.Data.LOOKUP_KEY + " = ?",
                new String[]{lookupKey}, null)) {
            if (cursor != null && cursor.moveToFirst())
                return new MiniContact(
                        lookupKey,
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)),
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)),
                        cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID)),
                        cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.STARRED)) == 1);
        } catch (Exception e) {
            android.util.Log.e("HomeScreenPinHelper", "pinned contact lookup failed", e);
        }
        return null;
    }

    /**
     * Moves pre-slot-format pins into free tile slots: legacy SharedPreferences
     * contact set and apps flagged pinned in {@link AppsDatabase}. Idempotent —
     * leftover contacts stay in the legacy set until a slot frees up.
     */
    public static void migrateLegacyPins(Context context) {
        final SharedPreferences prefs = tilePrefs(context);
        final SharedPreferences legacy = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        final Set<String> lookupKeys = legacy.getStringSet(SET_KEY, null);
        if (lookupKeys != null) {
            final Set<String> rest = new HashSet<>();
            for (String lookupKey : lookupKeys)
                if (findSlotWith(prefs, CONTACT_TILE_PREFIX + lookupKey) == null) {
                    final String free = findFreeSlot(prefs);
                    if (free == null) {
                        rest.add(lookupKey);
                        continue;
                    }
                    // commit immediately so the next iteration sees this slot as taken
                    prefs.edit().putString(free, CONTACT_TILE_PREFIX + lookupKey).commit();
                }
            if (rest.size() != lookupKeys.size())
                legacy.edit().putStringSet(SET_KEY, rest).apply();
        }
        for (final App app : AppsDatabase.getInstance(context).appsDatabaseDao().getAllPinned()) {
            if (findSlotWith(prefs, app.getFlattenComponentName()) != null) {
                AppsDatabase.getInstance(context).appsDatabaseDao().update(app.getId(), false);
                continue;
            }
            final String free = findFreeSlot(prefs);
            if (free == null)
                break;
            prefs.edit().putString(free, app.getFlattenComponentName()).commit();
            AppsDatabase.getInstance(context).appsDatabaseDao().update(app.getId(), false);
        }
    }

    public static boolean isAppPinned(Context context, String flattenComponentName) {
        return findSlotWith(tilePrefs(context), flattenComponentName) != null;
    }

    /** @return false when no slot is free. */
    public static boolean pinApp(Context context, String flattenComponentName) {
        final SharedPreferences prefs = tilePrefs(context);
        if (findSlotWith(prefs, flattenComponentName) != null)
            return true;
        final String free = findFreeSlot(prefs);
        if (free == null)
            return false;
        prefs.edit().putString(free, flattenComponentName).apply();
        return true;
    }

    public static void unpinApp(Context context, String flattenComponentName) {
        final SharedPreferences prefs = tilePrefs(context);
        final String slot = findSlotWith(prefs, flattenComponentName);
        if (slot != null)
            prefs.edit().putString(slot, HIDDEN_TILE).apply();
    }

    public static final class PinnedContactPreferences {
        public static final String KEY = "PINNED_CONTACTS_KEY";
        public static final String SET_KEY = "PINNED_CONTACTS_SET_KEY";
        public static final String SOS_KEY = "PINNED_CONTACTS_SET_KEY_SOS";
    }
}
