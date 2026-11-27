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

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.BuildConfig;
import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.databases.apps.AppsDatabase;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.AppUtils;

import java.io.File;

public class TechnicalInfoActivity extends BaldActivity {
    public static void deleteCache(Context context) {
        try {
            deleteDir(context.getCacheDir());
        } catch (Exception e) {
            e.printStackTrace();
        }
        AppsDatabase.getInstance(context).appsDatabaseDao().deleteAll();
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    public static String getTechnicalInfo() {
        return
                String.format("Api Level: %s\nVersion Name: %s\nVersion Code: %s\nFlavor: %s\nManufacturer: %s\nBrand: %s\nDevice: %s\nModel: %s\n",
                        AppUtils.str(Build.VERSION.SDK_INT),
                        AppUtils.str(BuildConfig.VERSION_NAME),
                        AppUtils.str(BuildConfig.VERSION_CODE),
                        AppUtils.str(BuildConfig.FLAVOR),
                        AppUtils.str(Build.MANUFACTURER),
                        AppUtils.str(Build.BRAND),
                        AppUtils.str(Build.DEVICE),
                        AppUtils.str(Build.MODEL))
                ;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkPermissions(this, requiredPermissions()))
            return;
        setContentView(R.layout.activity_tech_info);
        ((TextView) findViewById(R.id.tv_info)).setText(getTechnicalInfo());
        findViewById(R.id.bt_clear_cache).setOnClickListener(v ->
                DialogScreen.confirm(this,
                        R.string.clear_cache,
                        R.string.clear_cache_subtext,
                        R.string.delete_temp_files,
                        () -> {
                            deleteCache(this);
                            BPrefs.get(this).edit().remove(BPrefs.CUSTOM_APP_KEY).apply();
                            if (BuildConfig.FLAVOR.equals("baldUpdates"))
                                UpdatesActivity.removeUpdatesInfo(this);
                            BaldToast.simple(this, R.string.cache_cleared_successfully);
                        }));

        findViewById(R.id.bt_clear_data).setOnClickListener(v ->
                DialogScreen.confirm(this,
                        R.string.clear_data,
                        R.string.clear_data_subtext,
                        R.string.delete_all_data,
                        () -> DialogScreen.confirm(this,
                                R.string.clear_data,
                                R.string.clear_data_subtext2,
                                R.string.delete_all_data,
                                () -> DialogScreen.confirm(this,
                                        R.string.clear_data,
                                        R.string.clear_data_subtext3,
                                        R.string.delete_all_data,
                                        () -> ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData()))));
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_WRITE_SETTINGS | PERMISSION_WRITE_EXTERNAL_STORAGE;
    }
}
