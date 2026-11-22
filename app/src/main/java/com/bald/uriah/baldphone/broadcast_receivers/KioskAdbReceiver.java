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

package com.bald.uriah.baldphone.broadcast_receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bald.uriah.baldphone.BuildConfig;
import com.bald.uriah.baldphone.utils.KioskManager;

/**
 * Recovery from a computer (e.g. forgotten kiosk password). Protected by android.permission.DUMP,
 * so only the adb shell / system can send it:
 * adb shell am broadcast -a com.granphone.launcher.action.KIOSK_OFF -n com.granphone.launcher/.broadcast_receivers.KioskAdbReceiver
 * adb shell am broadcast -a com.granphone.launcher.action.REMOVE_DEVICE_OWNER -n com.granphone.launcher/.broadcast_receivers.KioskAdbReceiver
 */
public class KioskAdbReceiver extends BroadcastReceiver {
    public static final String ACTION_KIOSK_OFF = BuildConfig.APPLICATION_ID + ".action.KIOSK_OFF";
    public static final String ACTION_REMOVE_DEVICE_OWNER = BuildConfig.APPLICATION_ID + ".action.REMOVE_DEVICE_OWNER";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (ACTION_KIOSK_OFF.equals(action)) {
            KioskManager.disable(context, null);
        } else if (ACTION_REMOVE_DEVICE_OWNER.equals(action)) {
            KioskManager.removeDeviceOwner(context);
        }
    }
}
