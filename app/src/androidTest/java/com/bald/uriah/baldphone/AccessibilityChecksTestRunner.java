package com.bald.uriah.baldphone;

import android.app.UiAutomation;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.runner.AndroidJUnitRunner;

import com.bald.uriah.baldphone.services.NotificationListenerService;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class AccessibilityChecksTestRunner extends AndroidJUnitRunner {
    {
        // wave 0: log violations instead of failing, until the baseline in
        // research/2026-11-22-granphone-ux-redesign/a11y-baseline.md is burned down.
        AccessibilityChecks.enable()
                .setRunChecksFromRootView(true)
                .setThrowExceptionForErrors(false);
    }

    @Override
    public void onStart() {
        healTestDevice();
        super.onStart();
    }

    /**
     * With no default HOME app every emptied task stack pops a ResolverActivity chooser
     * that steals focus and breaks Espresso touch injection; the grants below let the
     * activities under test skip their PermissionActivity redirects.
     */
    private void healTestDevice() {
        final UiAutomation automation = getUiAutomation();
        final String pkg = getTargetContext().getPackageName();
        shell(automation, "cmd role add-role-holder android.app.role.HOME com.android.launcher3");
        shell(automation, "appops set " + pkg + " WRITE_SETTINGS allow");
        shell(automation, "appops set " + pkg + " SYSTEM_ALERT_WINDOW allow");
        shell(automation, "settings put secure enabled_notification_listeners "
                + pkg + "/" + NotificationListenerService.class.getName());
        for (final String permission : new String[]{
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS",
                "android.permission.CALL_PHONE",
                "android.permission.READ_CALL_LOG",
                "android.permission.WRITE_CALL_LOG",
                "android.permission.READ_PHONE_STATE",
                "android.permission.CAMERA",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"})
            shell(automation, "pm grant " + pkg + " " + permission);
    }

    private static void shell(UiAutomation automation, String command) {
        try (ParcelFileDescriptor fd = automation.executeShellCommand(command);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(new FileInputStream(fd.getFileDescriptor())))) {
            while (reader.readLine() != null) ; // drain: the command must run to completion
        } catch (IOException e) {
            Log.w("BaldTestRunner", "device setup command failed: " + command, e);
        }
    }
}
