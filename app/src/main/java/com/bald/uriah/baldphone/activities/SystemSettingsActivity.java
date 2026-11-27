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
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bald.uriah.baldphone.BuildConfig;
import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.CallManager;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.KioskManager;
import com.bald.uriah.baldphone.utils.UpdatingUtil;

/** System: permissions, defaults, kiosk mode, device info, updates. */
public class SystemSettingsActivity extends SettingsPageActivity {

    @Override
    protected int pageTitleRes() {
        return R.string.system_settings;
    }

    @Override
    protected void populateItems() {
        if (!checkPermissions(this, PERMISSION_ALL))
            items.add(
                    new RunnableSettingsItem(R.string.grant_all_permissions,
                            v -> startActivity(new Intent(this, PermissionActivity.class)),
                            R.drawable.grant_all_permissions_on_button)
            );
        items.add(
                new RunnableSettingsItem(R.string.set_home_screen,
                        v -> FakeLauncherActivity.resetPreferredLauncherAndOpenChooser(this)
                        , R.drawable.home_on_button)
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            items.add(
                    new RunnableSettingsItem(R.string.use_granphone_for_calls,
                            v -> {
                                if (checkPermissions(this, PERMISSION_CALL_SCREEN))
                                    CallManager.requestDefaultDialer(this);
                                else
                                    startActivity(new Intent(this, PermissionActivity.class)
                                            .putExtra(PermissionActivity.EXTRA_REQUIRED_PERMISSIONS, PERMISSION_CALL_SCREEN));
                            }
                            , R.drawable.phone_on_button)
            );
        items.add(new RunnableSettingsItem(R.string.kiosk_mode, v -> showKioskDialog(), R.drawable.blocked_on_button));
        if (KioskManager.isDeviceOwner(this) && !KioskManager.isEnabled(this))
            items.add(new RunnableSettingsItem(R.string.remove_device_owner, v -> DialogScreen.confirm(this,
                    R.string.remove_device_owner,
                    R.string.remove_device_owner_subtext,
                    R.string.remove_admin_access,
                    () -> {
                        KioskManager.removeDeviceOwner(this);
                        this.recreate();
                    }), R.drawable.blocked_on_button));
        items.add(new RunnableSettingsItem(R.string.advanced_options, v -> startActivity(new Intent(Settings.ACTION_SETTINGS)), R.drawable.settings_on_button));
        items.add(
                new RunnableSettingsItem(R.string.device_memory,
                        v -> DialogScreen.info(this, R.string.device_memory, R.string.device_memory_subtext,
                                buildMemoryStatusView(), R.string.understood)
                        , R.drawable.storage_on_button)
        );
        items.add(
                new RunnableSettingsItem(R.string.technical_information,
                        v -> startActivity(new Intent(this, TechnicalInfoActivity.class)),
                        R.drawable.tech_info_on_button)
        );
        if (BuildConfig.FLAVOR.equals("baldUpdates"))
            items.add(
                    new RunnableSettingsItem(R.string.check_for_updates,
                            v -> UpdatingUtil.checkForUpdates(this, true),
                            R.drawable.updates_on_button)
            );
    }

    private void showKioskDialog() {
        if (!KioskManager.isDeviceOwner(this)) {
            DialogScreen.info(this, R.string.kiosk_mode, R.string.kiosk_not_device_owner_subtext);
        } else if (!KioskManager.isEnabled(this)) {
            showPinPage(R.string.kiosk_mode, R.string.kiosk_enable_subtext,
                    KioskManager.MIN_PIN_LENGTH,
                    text -> {
                        final String pin = text.trim();
                        if (pin.length() < KioskManager.MIN_PIN_LENGTH || !android.text.TextUtils.isDigitsOnly(pin)) {
                            BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.kiosk_pin_too_short).show();
                            return false;
                        }
                        showKioskPinConfirmPage(pin);
                        return true;
                    }, null);
        } else {
            showPinPage(R.string.kiosk_mode, R.string.kiosk_disable_subtext,
                    KioskManager.savedPin(this).length(),
                    text -> {
                        if (!KioskManager.checkPin(this, text.trim())) {
                            BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.kiosk_wrong_pin).show();
                            return false;
                        }
                        KioskManager.disable(this, this);
                        BaldToast.from(this).setText(R.string.kiosk_disabled).show();
                        this.recreate();
                        return true;
                    }, null);
        }
    }

    /** Second step of kiosk PIN creation — a mistyped PIN could lock the device itself. */
    private void showKioskPinConfirmPage(final String pin) {
        showPinPage(R.string.kiosk_mode, R.string.helper_pin_repeat_subtext, pin.length(),
                text -> {
                    if (!pin.equals(text.trim())) {
                        BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.helper_pin_mismatch).show();
                        return false;
                    }
                    if (KioskManager.enable(this, pin))
                        BaldToast.from(this).setText(R.string.kiosk_enabled).show();
                    this.recreate();
                    return true;
                }, null);
    }

    private View buildMemoryStatusView() {
        final View view = LayoutInflater.from(this).inflate(R.layout.storage_status, null, false);

        final StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        final long storageTotal = statFs.getTotalBytes();
        final long storageFree = statFs.getAvailableBytes();
        ((ProgressBar) view.findViewById(R.id.pb_storage))
                .setProgress((int) ((storageTotal - storageFree) * 100 / storageTotal));
        ((TextView) view.findViewById(R.id.tv_storage))
                .setText(getString(R.string.free_of_total,
                        Formatter.formatFileSize(this, storageFree),
                        Formatter.formatFileSize(this, storageTotal)));

        final ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(memoryInfo);
        ((ProgressBar) view.findViewById(R.id.pb_ram))
                .setProgress((int) ((memoryInfo.totalMem - memoryInfo.availMem) * 100 / memoryInfo.totalMem));
        ((TextView) view.findViewById(R.id.tv_ram))
                .setText(getString(R.string.free_of_total,
                        Formatter.formatFileSize(this, memoryInfo.availMem),
                        Formatter.formatFileSize(this, memoryInfo.totalMem)));
        return view;
    }
}
