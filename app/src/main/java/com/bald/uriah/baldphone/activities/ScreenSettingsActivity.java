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
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.DialogScreen;

/** Screen & appearance: home layout, app list shape, status bar, colors, language. */
public class ScreenSettingsActivity extends SettingsPageActivity {
    private static final String[] LANGUAGE_CODES = {
            "", "ar", "bg", "ca", "cs", "da", "de", "el", "en", "es", "et", "eu", "fi", "fr",
            "ha", "hi", "hr", "hu", "id", "it", "iw", "ka", "ko", "nl", "pl", "pt", "pt-rBR",
            "ro", "ru", "sl", "sr", "sv", "tr", "zh"
    };

    @Override
    protected int pageTitleRes() {
        return R.string.screen_and_appearance;
    }

    @Override
    protected void populateItems() {
        items.add(new RunnableSettingsItem(R.string.edit_home_screen, v -> startActivity(new Intent(this, Page1EditorActivity.class)), R.drawable.edit_on_button));
        items.add(new SwitchSettingsItem(R.string.hide_bottom_row, R.drawable.keyboard_hide_on_button,
                BPrefs.HIDE_BOTTOM_ROW_KEY, BPrefs.HIDE_BOTTOM_ROW_DEFAULT_VALUE));
        items.add(
                new RunnableSettingsItem(R.string.apps_sort_method,
                        v -> DialogScreen.select(this, R.string.apps_sort_method,
                                sharedPreferences.getBoolean(BPrefs.APPS_ONE_GRID_KEY, BPrefs.APPS_ONE_GRID_DEFAULT_VALUE) ? 0 : 1,
                                index -> {
                                    editor.putBoolean(BPrefs.APPS_ONE_GRID_KEY, index == 0).apply();
                                    this.recreate();
                                },
                                R.string.one_grid, R.string.groups),
                        R.drawable.apps_on_button));
        items.add(new RunnableSettingsItem(R.string.status_bar_settings,
                v -> DialogScreen.select(this, R.string.status_bar_settings,
                        sharedPreferences.getInt(BPrefs.STATUS_BAR_KEY, BPrefs.STATUS_BAR_DEFAULT_VALUE),
                        index -> {
                            editor.putInt(BPrefs.STATUS_BAR_KEY, index).apply();
                            this.recreate();
                        },
                        R.string.nowhere, R.string.only_home_screen, R.string.everywhere),
                R.drawable.status_bar_on_button
        ));
        items.add(new SwitchSettingsItem(R.string.colorful, R.drawable.colors_on_button,
                BPrefs.COLORFUL_KEY, BPrefs.COLORFUL_DEFAULT_VALUE));
        items.add(new RunnableSettingsItem(R.string.language_settings, v -> showLanguageDialog(), R.drawable.translate_on_button) {
            @Override
            public CharSequence getSubtitle() {
                final String code = sharedPreferences.getString(BPrefs.LANGUAGE_KEY, BPrefs.LANGUAGE_DEFAULT_VALUE);
                if (code.isEmpty())
                    return getText(R.string.system_default);
                final java.util.Locale locale = java.util.Locale.forLanguageTag(code.replace("-r", "-"));
                return locale.getDisplayName(locale);
            }
        });
    }

    private void showLanguageDialog() {
        final String current = sharedPreferences.getString(BPrefs.LANGUAGE_KEY, BPrefs.LANGUAGE_DEFAULT_VALUE);
        final CharSequence[] options = new CharSequence[LANGUAGE_CODES.length];
        int selected = 0;
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (i == 0) {
                options[i] = getText(R.string.system_default);
            } else {
                final java.util.Locale locale = java.util.Locale.forLanguageTag(LANGUAGE_CODES[i].replace("-r", "-"));
                options[i] = locale.getDisplayName(locale);
            }
            if (LANGUAGE_CODES[i].equals(current))
                selected = i;
        }
        DialogScreen.select(this, R.string.language_settings, selected,
                index -> {
                    editor.putString(BPrefs.LANGUAGE_KEY, LANGUAGE_CODES[index]).apply();
                    this.recreate();
                }, options);
    }
}
