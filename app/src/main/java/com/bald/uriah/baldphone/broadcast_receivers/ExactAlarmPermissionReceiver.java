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

package com.bald.uriah.baldphone.broadcast_receivers;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bald.uriah.baldphone.databases.alarms.AlarmScheduler;
import com.bald.uriah.baldphone.databases.reminders.ReminderScheduler;
import com.bald.uriah.baldphone.utils.AppUtils;

/**
 * Reschedules alarms and reminders after the user grants SCHEDULE_EXACT_ALARM.
 * Android does not send this broadcast when the permission is revoked; revocation deletes
 * exact alarms, so recovery intentionally happens only after a later grant/boot/app start.
 */
public class ExactAlarmPermissionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (!AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(intent.getAction()))
            return;
        final Context appContext = context.getApplicationContext();
        final AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        // the permission can be revoked again before this foreground broadcast is delivered
        if (alarmManager == null || !AppUtils.canScheduleExactAlarms(alarmManager))
            return;
        AlarmScheduler.reStartAlarms(appContext);
        ReminderScheduler.reStartReminders(appContext);
    }
}
