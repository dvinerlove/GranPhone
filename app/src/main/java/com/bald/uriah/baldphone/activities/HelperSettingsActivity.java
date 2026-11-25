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

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.Const;
import com.bald.uriah.baldphone.utils.KioskManager;

/**
 * The helper layer: everything operational or dangerous stays behind the PIN
 * gate, grouped into four drill-in sub-screens. Backing out of the gate PIN
 * finishes the activity — landing wherever it was launched from (main list or
 * home), so no "unseen level" is ever revealed.
 */
public class HelperSettingsActivity extends SettingsPageActivity {
    /** static so it survives leaving the activity; dies with the process — fine for a 5-min grace. */
    private static final long HELPER_PIN_GRACE = 5 * 60 * Const.SECOND;
    private static long helperUnlockedAt;

    @Override
    protected int pageTitleRes() {
        return R.string.helper_settings;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (viewsReady && helperPin() != null
                && System.currentTimeMillis() - helperUnlockedAt >= HELPER_PIN_GRACE)
            showHelperPinPage();
    }

    @Override
    protected void populateItems() {
        // The lock's state IS the PIN's presence (null key — no boolean pref): the
        // switch flips optimistically, then resync() re-reads truth if the PIN
        // page is backed out of. On = create+confirm, off = verify then drop HELPER_PIN_KEY.
        items.add(new SwitchSettingsItem(R.string.settings_lock, R.drawable.blocked_on_button, null, false) {
            @Override
            protected boolean readChecked() {
                return helperPin() != null;
            }

            @Override
            protected void apply(boolean checked) {
                if (checked) {
                    showPinPage(R.string.settings_lock, R.string.helper_pin_create_subtext,
                            KioskManager.MIN_PIN_LENGTH,
                            text -> {
                                final String pin = text.trim();
                                if (pin.length() < KioskManager.MIN_PIN_LENGTH || !android.text.TextUtils.isDigitsOnly(pin)) {
                                    BaldToast.from(HelperSettingsActivity.this).setType(BaldToast.TYPE_ERROR).setText(R.string.kiosk_pin_too_short).show();
                                    return false;
                                }
                                showHelperPinConfirmPage(pin, this::resync);
                                return true;
                            }, this::resync);
                } else {
                    final String saved = helperPin();
                    showPinPage(R.string.settings_lock, R.string.helper_pin_enter_subtext,
                            saved.length(),
                            text -> {
                                if (!saved.equals(text.trim())) {
                                    BaldToast.from(HelperSettingsActivity.this).setType(BaldToast.TYPE_ERROR).setText(R.string.kiosk_wrong_pin).show();
                                    return false;
                                }
                                editor.remove(BPrefs.HELPER_PIN_KEY).apply();
                                return true;
                            }, this::resync);
                }
            }
        });
        items.add(new Category(R.string.screen_and_appearance, R.drawable.screen_on_button, ScreenSettingsActivity.class));
        items.add(new Category(R.string.convenience_accessibility, R.drawable.accessibility_on_button, ConvenienceSettingsActivity.class));
        items.add(new Category(R.string.connection_settings, R.drawable.wifi_on_button, ConnectionSettingsActivity.class));
        items.add(new Category(R.string.system_settings, R.drawable.settings_on_button, SystemSettingsActivity.class));
    }

    /** The kiosk PIN when one exists, else the helper PIN the helper set on first entry. */
    @Nullable
    private String helperPin() {
        final String kioskPin = KioskManager.savedPin(this);
        return kioskPin != null ? kioskPin : sharedPreferences.getString(BPrefs.HELPER_PIN_KEY, null);
    }

    /** Entry gate — the list stays hidden until the PIN passes; back-out exits. */
    private void showHelperPinPage() {
        final String saved = helperPin();
        showPinPage(new PinRequest(R.string.helper_settings, R.string.helper_pin_enter_subtext,
                saved.length(),
                pin -> {
                    if (!saved.equals(pin.trim())) {
                        BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.kiosk_wrong_pin).show();
                        return false;
                    }
                    helperUnlockedAt = System.currentTimeMillis();
                    return true;
                },
                null, true));
    }

    /** Second step of PIN creation — a mistyped PIN would lock the helper out of their own settings. */
    private void showHelperPinConfirmPage(final String pin, @Nullable Runnable onCancel) {
        showPinPage(R.string.settings_lock, R.string.helper_pin_repeat_subtext, pin.length(),
                text -> {
                    if (!pin.equals(text.trim())) {
                        BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.helper_pin_mismatch).show();
                        return false;
                    }
                    editor.putString(BPrefs.HELPER_PIN_KEY, pin).apply();
                    helperUnlockedAt = System.currentTimeMillis();
                    return true;
                }, onCancel);
    }
}
