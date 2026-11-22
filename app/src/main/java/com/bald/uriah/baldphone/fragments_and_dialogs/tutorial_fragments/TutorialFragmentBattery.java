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

import android.os.Build;
import android.widget.ImageView;
import android.widget.TextView;

import com.bald.uriah.baldphone.R;

import java.util.Locale;

/**
 * Static page: OEM-specific background/battery instructions, chosen by
 * {@link Build#MANUFACTURER} with a generic fallback.
 */
public class TutorialFragmentBattery extends TutorialFragment {
    private ImageView icon;
    private TextView bald;

    @Override
    protected void attachXml() {
        icon = root.findViewById(R.id.icon);
        bald = root.findViewById(R.id.bald);
    }

    @Override
    protected void actualSetup() {
        icon.setImageResource(R.drawable.battery_full_on_background);
        bald.setText(batteryInstructionsRes());
        setupNavButtons();
    }

    private static int batteryInstructionsRes() {
        final String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase(Locale.US);
        if (manufacturer.contains("huawei") || manufacturer.contains("honor"))
            return R.string.tutorial_battery_huawei;
        if (manufacturer.contains("xiaomi"))
            return R.string.tutorial_battery_xiaomi;
        if (manufacturer.contains("samsung"))
            return R.string.tutorial_battery_samsung;
        return R.string.tutorial_battery_generic;
    }

    @Override
    protected int layoutRes() {
        return R.layout.tutorial_fragment_static;
    }
}
