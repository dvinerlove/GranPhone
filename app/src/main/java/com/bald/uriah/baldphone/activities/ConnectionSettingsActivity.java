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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.DialogScreen;

import java.util.List;

/** Connection: radios and SIMs — mostly deep links into the system settings. */
public class ConnectionSettingsActivity extends SettingsPageActivity {
    private static final String TAG = ConnectionSettingsActivity.class.getSimpleName();

    @Override
    protected int pageTitleRes() {
        return R.string.connection_settings;
    }

    @Override
    protected void populateItems() {
        items.add(new RunnableSettingsItem(R.string.wifi, v -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)), R.drawable.wifi_on_button));
        items.add(new RunnableSettingsItem(R.string.bluetooth, v -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)), R.drawable.bluetooth_on_button));
        items.add(new RunnableSettingsItem(R.string.airplane_mode, v -> startActivity(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)), R.drawable.airplane_mode_on_button));
        items.add(new RunnableSettingsItem(R.string.location, v -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)), R.drawable.location_on_button));

        final NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        final NfcAdapter adapter = manager == null ? null : manager.getDefaultAdapter();
        if (adapter != null)
            items.add(new RunnableSettingsItem(R.string.nfc, v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                    BaldToast.from(this).setText(R.string.setting_does_not_exist).setType(BaldToast.TYPE_ERROR).show();

                }
            }, R.drawable.nfc_on_button));
        if (hasMultipleSims())
            items.add(new RunnableSettingsItem(R.string.dual_sim,
                    v -> DialogScreen.select(this, R.string.dual_sim,
                            sharedPreferences.getBoolean(BPrefs.DUAL_SIM_KEY, BPrefs.DUAL_SIM_DEFAULT_VALUE) ? 0 : 1,
                            index -> {
                                editor.putBoolean(BPrefs.DUAL_SIM_KEY, index == 0).apply();
                                this.recreate();
                            },
                            R.string.show_options, R.string.use_default),
                    R.drawable.sim_on_button
            ));
    }

    /** true when the device provably has 2+ active SIMs; no permission or API<22 = treated as single-SIM. */
    private boolean hasMultipleSims() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
            return false;
        if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED)
            return false;
        try {
            final android.telephony.SubscriptionManager sm = (android.telephony.SubscriptionManager)
                    getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            final List<android.telephony.SubscriptionInfo> list = sm == null ? null : sm.getActiveSubscriptionInfoList();
            return list != null && list.size() > 1;
        } catch (SecurityException e) {
            return false;
        }
    }
}
