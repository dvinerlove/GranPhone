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

package com.bald.uriah.baldphone.services;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.CallActivity;
import com.bald.uriah.baldphone.utils.CallManager;

/**
 * Bound by the system when GranPhone is the default phone app.
 * Every incoming / outgoing call is shown on {@link CallActivity} (big buttons) instead of the stock call screen.
 * Ringing itself is still done by the system.
 */
@TargetApi(Build.VERSION_CODES.M)
public class BaldInCallService extends InCallService {
    private static final String TAG = BaldInCallService.class.getSimpleName();
    private static final String CHANNEL_ID = "granphone_calls";
    private static final int NOTIFICATION_ID = 4210;
    private final CallManager.Listener listener = this::updateNotification;

    @Override
    public void onCreate() {
        super.onCreate();
        CallManager.onServiceCreated(this);
        CallManager.addListener(listener);
    }

    @Override
    public void onDestroy() {
        CallManager.removeListener(listener);
        CallManager.onServiceDestroyed();
        cancelNotification();
        super.onDestroy();
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        CallManager.addCall(call);
        try {
            // InCallService is allowed to start activities from the background.
            startActivity(CallActivity.newIntent(this));
        } catch (Exception e) {
            Log.e(TAG, "could not open the call screen", e);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        CallManager.removeCall(call);
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        super.onCallAudioStateChanged(audioState);
        CallManager.notifyListeners();
    }

    private void cancelNotification() {
        final NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null)
            notificationManager.cancel(NOTIFICATION_ID);
    }

    /**
     * Ongoing notification that brings the user back to the call screen.
     * While ringing it also carries a full screen intent, as a fallback for devices that block the activity start.
     */
    private void updateNotification() {
        final NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null)
            return;
        final Call call = CallManager.getPrimaryCall();
        if (call == null || !CallManager.hasOngoingCall()) {
            notificationManager.cancel(NOTIFICATION_ID);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.calls_channel), NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null, null); // the system already rings
            notificationManager.createNotificationChannel(channel);
        }
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                CallActivity.newIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        final boolean ringing = CallManager.getState(call) == Call.STATE_RINGING;
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call_white)
                .setContentTitle(getString(ringing ? R.string.incoming_call : R.string.ongoing_call))
                .setContentText(getString(R.string.tap_to_return_to_call))
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);
        if (ringing)
            builder.setFullScreenIntent(pendingIntent, true);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
