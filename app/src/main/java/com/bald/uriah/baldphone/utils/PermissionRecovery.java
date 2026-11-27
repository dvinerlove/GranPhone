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

package com.bald.uriah.baldphone.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks runtime-permission denials observed by one permission flow and tells the UI
 * when another system request is a dead end. shortcut: state is in-memory; after a
 * process death Android cannot distinguish «never asked» from «permanently denied»
 * until one more denied request, so upgrade to persisted state only if Wave 3b needs it.
 */
public final class PermissionRecovery {
    private final Set<String> permanentlyDenied = new HashSet<>();

    public void onRequestPermissionsResult(final Activity activity, final String[] permissions, final int[] grantResults) {
        for (int i = 0; i < permissions.length && i < grantResults.length; i++)
            onRequestResult(permissions[i],
                    grantResults[i] == PackageManager.PERMISSION_GRANTED,
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i]));
    }

    void onRequestResult(final String permission, final boolean granted, final boolean shouldShowRationale) {
        if (granted || shouldShowRationale)
            permanentlyDenied.remove(permission);
        else
            permanentlyDenied.add(permission);
    }

    /** Conservative: settings replace the runtime request only when every missing permission is a known dead end. */
    public boolean shouldOpenAppSettings(final Activity activity, final String[] permissions) {
        final boolean[] granted = new boolean[permissions.length];
        final boolean[] shouldShowRationale = new boolean[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            granted[i] = ActivityCompat.checkSelfPermission(activity, permissions[i]) == PackageManager.PERMISSION_GRANTED;
            shouldShowRationale[i] = ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i]);
        }
        return shouldOpenAppSettings(permissions, granted, shouldShowRationale);
    }

    boolean shouldOpenAppSettings(final String[] permissions, final boolean[] granted, final boolean[] shouldShowRationale) {
        boolean anyMissing = false;
        for (int i = 0; i < permissions.length; i++) {
            if (granted[i])
                continue;
            anyMissing = true;
            if (!permanentlyDenied.contains(permissions[i]) || shouldShowRationale[i])
                return false;
        }
        return anyMissing;
    }

    public static Intent appSettingsIntent(final Context context) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + context.getPackageName()));
    }
}
