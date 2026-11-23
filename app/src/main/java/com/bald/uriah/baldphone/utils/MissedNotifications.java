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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.bald.uriah.baldphone.R;

/**
 * Posts low-priority "missed" notifications when a ring screen times out unacknowledged.
 * Called from the main process (the timed activities) — never from the :remote receivers.
 */
public final class MissedNotifications {
    private static final String CHANNEL_MISSED = "granphone_missed";

    private MissedNotifications() {
    }

    public static void post(@NonNull Context context, @NonNull String tag, int id,
                            @StringRes int titleRes, @Nullable CharSequence text,
                            @DrawableRes int iconRes, @NonNull Class<?> targetActivity) {
        final NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // low importance on purpose — the alarm already rang for minutes
            notificationManager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_MISSED, context.getString(R.string.missed), NotificationManager.IMPORTANCE_LOW));
        }
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, targetActivity),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notificationManager.notify(tag, id, new NotificationCompat.Builder(context, CHANNEL_MISSED)
                .setSmallIcon(iconRes)
                .setContentTitle(context.getString(titleRes))
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build());
    }
}
