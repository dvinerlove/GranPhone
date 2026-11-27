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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.FakeLauncherActivity;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.views.BaldButton;

public class TutorialFragment4 extends TutorialFragment {
    BaldButton bt_home;

    @Override
    protected void attachXml() {
        bt_home = root.findViewById(R.id.bt_home);
    }

    @Override
    protected void actualSetup() {
        setupNavButtons();
        bt_home.setOnClickListener(v -> {
            final Context context = v.getContext();
            context.getSharedPreferences(BPrefs.KEY, Context.MODE_PRIVATE).edit().putBoolean(BPrefs.AFTER_TUTORIAL_KEY, true).apply();
            // resetPreferredLauncherAndOpenChooser clears the default home app to
            // force the system chooser; when we already are default just go home.
            if (isDefaultLauncher(context))
                context.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            else
                FakeLauncherActivity.resetPreferredLauncherAndOpenChooser(context);
        });
    }

    private boolean isDefaultLauncher(Context context) {
        final ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && context.getPackageName().equals(resolveInfo.activityInfo.packageName);
    }

    @Override
    protected int layoutRes() {
        return R.layout.tutorial_fragment_4;
    }
}
