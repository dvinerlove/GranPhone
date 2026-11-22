/*
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

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.UserManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.activities.HomeScreenActivity;
import com.bald.uriah.baldphone.broadcast_receivers.BaldDeviceAdminReceiver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kiosk mode: keeps the phone inside GranPhone using Android's lock task mode.
 * Needs GranPhone to be the device owner (set once from a computer with adb, see docs/INSTALACAO.md).
 * <p>
 * While enabled:
 * - the notification shade, recents and the Android settings are not reachable
 * - only GranPhone and the apps that appear in it (minus settings, app stores and other launchers) can be opened
 * - apps cannot be uninstalled
 * - GranPhone is always the home screen and has all of its permissions granted
 */
public final class KioskManager {
    private static final String TAG = KioskManager.class.getSimpleName();
    private static final String KIOSK_KEY = "KIOSK_KEY";
    private static final String KIOSK_PIN_KEY = "KIOSK_PIN_KEY";
    public static final int MIN_PIN_LENGTH = 4;
    private static final String[] OWN_RUNTIME_PERMISSIONS = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private KioskManager() {
    }

    private static ComponentName admin(Context context) {
        return new ComponentName(context, BaldDeviceAdminReceiver.class);
    }

    @Nullable
    private static DevicePolicyManager dpm(Context context) {
        return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    public static boolean isDeviceOwner(Context context) {
        final DevicePolicyManager dpm = dpm(context);
        return dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
    }

    public static boolean isEnabled(Context context) {
        return isDeviceOwner(context) && BPrefs.get(context).getBoolean(KIOSK_KEY, false);
    }

    @Nullable
    public static String savedPin(Context context) {
        return BPrefs.get(context).getString(KIOSK_PIN_KEY, null);
    }

    public static boolean checkPin(Context context, CharSequence pin) {
        final String saved = savedPin(context);
        return saved != null && saved.equals(String.valueOf(pin));
    }

    /**
     * @return false if GranPhone is not the device owner.
     */
    public static boolean enable(Activity activity, CharSequence pin) {
        if (!isDeviceOwner(activity))
            return false;
        BPrefs.get(activity).edit()
                .putBoolean(KIOSK_KEY, true)
                .putString(KIOSK_PIN_KEY, String.valueOf(pin))
                .apply();
        enterIfNeeded(activity);
        return true;
    }

    /**
     * Turns kiosk mode off. GranPhone stays device owner, so it can be turned on again without a computer.
     *
     * @param activity an activity of the locked task, if available, to stop lock task right away.
     */
    public static void disable(Context context, @Nullable Activity activity) {
        // drop the PIN too: a leftover PIN would shadow the helper PIN in settings,
        // and enable() always writes a fresh one anyway
        BPrefs.get(context).edit().putBoolean(KIOSK_KEY, false).remove(KIOSK_PIN_KEY).apply();
        if (activity != null) {
            try {
                activity.stopLockTask();
            } catch (Exception e) {
                Log.e(TAG, "stopLockTask failed", e);
            }
        }
        final DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context))
            return;
        final ComponentName admin = admin(context);
        try {
            dpm.setLockTaskPackages(admin, new String[0]); // also ends lock task mode if still active
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                dpm.setStatusBarDisabled(admin, false);
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_UNINSTALL_APPS);
            dpm.clearPackagePersistentPreferredActivities(admin, context.getPackageName());
        } catch (Exception e) {
            Log.e(TAG, "disable failed", e);
        }
    }

    /**
     * Turns kiosk mode off and gives up device owner, so GranPhone can be uninstalled.
     */
    @SuppressWarnings("deprecation")
    public static void removeDeviceOwner(Context context) {
        disable(context, null);
        final DevicePolicyManager dpm = dpm(context);
        if (dpm != null && isDeviceOwner(context)) {
            try {
                dpm.clearDeviceOwnerApp(context.getPackageName());
            } catch (Exception e) {
                Log.e(TAG, "clearDeviceOwnerApp failed", e);
            }
        }
    }

    /**
     * Call from the home screen's onResume: refreshes the allowed apps (new apps may have been installed) and locks the task.
     */
    public static void enterIfNeeded(Activity activity) {
        if (!isEnabled(activity))
            return;
        final ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null && isInLockTask(activityManager)) {
            updateAllowedPackages(activity);
        } else {
            applyPolicies(activity);
            try {
                activity.startLockTask();
            } catch (Exception e) {
                Log.e(TAG, "startLockTask failed", e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isInLockTask(ActivityManager activityManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return activityManager.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE;
        return activityManager.isInLockTaskMode();
    }

    /**
     * Only touches the allowlist when it changed (an app was installed or removed).
     */
    private static void updateAllowedPackages(Context context) {
        final DevicePolicyManager dpm = dpm(context);
        if (dpm == null)
            return;
        try {
            final ComponentName admin = admin(context);
            final String[] allowed = allowedPackages(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final Set<String> current = new HashSet<>(java.util.Arrays.asList(dpm.getLockTaskPackages(admin)));
                if (current.equals(new HashSet<>(java.util.Arrays.asList(allowed))))
                    return;
            }
            dpm.setLockTaskPackages(admin, allowed);
        } catch (Exception e) {
            Log.e(TAG, "updateAllowedPackages failed", e);
        }
    }

    private static void applyPolicies(Context context) {
        final DevicePolicyManager dpm = dpm(context);
        if (dpm == null)
            return;
        final ComponentName admin = admin(context);
        final String packageName = context.getPackageName();
        try {
            dpm.setLockTaskPackages(admin, allowedPackages(context));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Home button (goes back to GranPhone), power menu (to restart / turn off) and the clock & battery.
                // No notification shade, no recents, no lock screen.
                dpm.setLockTaskFeatures(admin,
                        DevicePolicyManager.LOCK_TASK_FEATURE_HOME
                                | DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS
                                | DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dpm.setStatusBarDisabled(admin, true);
            }
            dpm.addUserRestriction(admin, UserManager.DISALLOW_UNINSTALL_APPS);

            final IntentFilter homeFilter = new IntentFilter(Intent.ACTION_MAIN);
            homeFilter.addCategory(Intent.CATEGORY_HOME);
            homeFilter.addCategory(Intent.CATEGORY_DEFAULT);
            dpm.addPersistentPreferredActivity(admin, homeFilter, new ComponentName(context, HomeScreenActivity.class));
        } catch (Exception e) {
            Log.e(TAG, "applyPolicies failed", e);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (final String permission : OWN_RUNTIME_PERMISSIONS) {
                try {
                    dpm.setPermissionGrantState(admin, packageName, permission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                } catch (Exception e) {
                    Log.e(TAG, "could not grant " + permission, e);
                }
            }
        }
    }

    /**
     * GranPhone, every app with a launcher icon, the camera, the SMS app and the permission dialogs;
     * but never the Android settings, app stores or other home screens.
     */
    private static String[] allowedPackages(Context context) {
        final PackageManager pm = context.getPackageManager();
        final Set<String> blocked = new HashSet<>();
        blocked.addAll(packagesHandling(pm, new Intent(Settings.ACTION_SETTINGS)));
        blocked.addAll(packagesHandling(pm, new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName()))));
        blocked.addAll(packagesHandling(pm, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)));

        final Set<String> allowed = new HashSet<>();
        allowed.addAll(packagesHandling(pm, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)));
        allowed.addAll(packagesHandling(pm, new Intent(MediaStore.ACTION_IMAGE_CAPTURE)));
        allowed.addAll(packagesHandling(pm, new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)));
        allowed.addAll(packagesHandling(pm, new Intent("android.content.pm.action.REQUEST_PERMISSIONS")));
        final String smsPackage = Telephony.Sms.getDefaultSmsPackage(context);
        if (smsPackage != null)
            allowed.add(smsPackage);
        allowed.removeAll(blocked);
        allowed.add(context.getPackageName());
        return allowed.toArray(new String[0]);
    }

    private static Set<String> packagesHandling(PackageManager pm, Intent intent) {
        final Set<String> packages = new HashSet<>();
        final List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        for (final ResolveInfo resolveInfo : resolveInfos)
            if (resolveInfo.activityInfo != null)
                packages.add(resolveInfo.activityInfo.packageName);
        return packages;
    }
}
