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

package com.bald.uriah.baldphone.fragments_and_dialogs.tutorial_fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.TutorialActivity;
import com.bald.uriah.baldphone.utils.BPrefs;

public class TutorialFragment2 extends TutorialFragment {
    private View[] presses = new View[3];
    private View[] selected = new View[3];

    private SharedPreferences sharedPreferences;
    private int longPressesFlag = 2;//2 is most 1 is medium 0 is regular
    private boolean recreateActivityOnSelect = true;

    /** Inside TutorialActivity a recreate would bounce the pager to page 1. */
    public void setRecreateActivityOnSelect(boolean recreateActivityOnSelect) {
        this.recreateActivityOnSelect = recreateActivityOnSelect;
    }

    @Override
    protected void actualSetup() {
        setupNavButtons();
        sharedPreferences = BPrefs.get(getContext());
        longPressesFlag = sharedPreferences.getBoolean(BPrefs.LONG_PRESSES_KEY, BPrefs.LONG_PRESSES_DEFAULT_VALUE) ? sharedPreferences.getBoolean(BPrefs.LONG_PRESSES_SHORTER_KEY, BPrefs.LONG_PRESSES_SHORTER_DEFAULT_VALUE) ? 1 : 2 : 0;
        applyDuoToFlag();

        // Cards are BaldFrameLayoutButtons: they already gate taps behind the
        // currently-saved press mode and show the "press longer" toast themselves.
        for (final View press : presses)
            press.setOnClickListener(this::clickedEffect);
    }

    @Override
    protected void attachXml() {

        presses[2] = root.findViewById(R.id.long_presses);
        presses[1] = root.findViewById(R.id.medium_presses);
        presses[0] = root.findViewById(R.id.short_presses);

        for (int i = 0; i < presses.length; i++)
            presses[i].setTag(i);

        selected[2] = root.findViewById(R.id.long_presses_selected);
        selected[1] = root.findViewById(R.id.medium_presses_selected);
        selected[0] = root.findViewById(R.id.short_presses_selected);
    }

    private void clickedEffect(View v) {
        longPressesFlag = (int) v.getTag();
        applyDuoToFlag();
        sharedPreferences.edit()
                .putBoolean(BPrefs.VIBRATION_FEEDBACK_KEY, longPressesFlag > 0)
                .putBoolean(BPrefs.LONG_PRESSES_KEY, longPressesFlag > 0)
                .putBoolean(BPrefs.LONG_PRESSES_SHORTER_KEY, longPressesFlag == 1)
                .putBoolean(BPrefs.TOUCH_NOT_HARD_KEY, longPressesFlag == 0)
                .apply();
        Activity activity = getActivity();
        if (recreateActivityOnSelect && activity != null) {
            activity.recreate();
        }
        if (activity instanceof TutorialActivity)
            v.postDelayed(((TutorialActivity) activity)::advancePage, 700);

    }

    private void applyDuoToFlag() {
        for (int i = 0; i < presses.length; i++) {
            final boolean isSelected = longPressesFlag == i;
            selected[i].setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
            presses[i].setSelected(isSelected);
        }

    }

    @Override
    protected int layoutRes() {
        return R.layout.tutorial_fragment_2;
    }
}
