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

import android.content.Intent;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.pills.PillTimeSetterActivity;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.DialogScreen;

/** Ease of use & accessibility: touch behavior, hand preference, keyboard, sounds. */
public class ConvenienceSettingsActivity extends SettingsPageActivity {

    @Override
    protected int pageTitleRes() {
        return R.string.convenience_accessibility;
    }

    @Override
    protected void populateItems() {
        items.add(new RunnableSettingsItem(R.string.accessibility_level, v -> startActivity(new Intent(this, AccessibilityLevelChangerActivity.class)), R.drawable.accessibility_on_button) {
            @Override
            public CharSequence getSubtitle() {
                // same flag math as TutorialFragment2: 2=high, 1=medium, 0=regular
                final int flag = sharedPreferences.getBoolean(BPrefs.LONG_PRESSES_KEY, BPrefs.LONG_PRESSES_DEFAULT_VALUE)
                        ? (sharedPreferences.getBoolean(BPrefs.LONG_PRESSES_SHORTER_KEY, BPrefs.LONG_PRESSES_SHORTER_DEFAULT_VALUE) ? 1 : 2) : 0;
                return getText(flag == 2 ? R.string.high_level : flag == 1 ? R.string.medium_level : R.string.regular_level);
            }
        });
        items.add(new SwitchSettingsItem(R.string.accidental_touches, R.drawable.blocked_on_button,
                BPrefs.USE_ACCIDENTAL_GUARD_KEY, BPrefs.USE_ACCIDENTAL_GUARD_DEFAULT_VALUE));
        items.add(
                new RunnableSettingsItem(R.string.strong_hand,
                        v -> DialogScreen.select(this, R.string.strong_hand,
                                sharedPreferences.getBoolean(BPrefs.RIGHT_HANDED_KEY, BPrefs.RIGHT_HANDED_DEFAULT_VALUE) ? 1 : 0,
                                index -> {
                                    editor.putBoolean(BPrefs.RIGHT_HANDED_KEY, index == 1).apply();
                                    this.recreate();
                                },
                                R.string.left_handed, R.string.right_handed),
                        R.drawable.hand_on_button
                )
        );
        items.add(new RunnableSettingsItem(R.string.set_keyboard, v -> startActivity(new Intent(this, KeyboardChangerActivity.class)), R.drawable.keyboard_on_button));
        items.add(new SwitchSettingsItem(R.string.dialer_sounds, R.drawable.phone_on_button,
                BPrefs.DIALER_SOUNDS_KEY, BPrefs.DIALER_SOUNDS_DEFAULT_VALUE));
        items.add(new SwitchSettingsItem(R.string.low_battery_alert, R.drawable.low_battery_alert_on_button,
                BPrefs.LOW_BATTERY_ALERT_KEY, BPrefs.LOW_BATTERY_ALERT_DEFAULT_VALUE));
        items.add(new RunnableSettingsItem(R.string.time_changer, v -> startActivity(new Intent(this, PillTimeSetterActivity.class)), R.drawable.pill));
    }
}
