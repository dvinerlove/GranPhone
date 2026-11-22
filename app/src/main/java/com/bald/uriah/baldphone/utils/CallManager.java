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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.R;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps track of the calls handed to {@link com.bald.uriah.baldphone.services.BaldInCallService}
 * (only when GranPhone is the default phone app) and offers simple actions for the call screen.
 * All methods must be called on the main thread.
 */
@TargetApi(Build.VERSION_CODES.M)
public final class CallManager {
    public static final int REQUEST_CODE_DEFAULT_DIALER = 4242;
    private static final List<Call> calls = new CopyOnWriteArrayList<>();
    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            notifyListeners();
        }

        @Override
        public void onDetailsChanged(Call call, Call.Details details) {
            notifyListeners();
        }
    };
    @Nullable
    private static InCallService inCallService;

    private CallManager() {
    }

    public static void onServiceCreated(InCallService service) {
        inCallService = service;
    }

    public static void onServiceDestroyed() {
        inCallService = null;
        for (final Call call : calls)
            call.unregisterCallback(callCallback);
        calls.clear();
        notifyListeners();
    }

    public static void addCall(Call call) {
        calls.add(call);
        call.registerCallback(callCallback);
        notifyListeners();
    }

    public static void removeCall(Call call) {
        call.unregisterCallback(callCallback);
        calls.remove(call);
        notifyListeners();
    }

    public static void addListener(Listener listener) {
        listeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public static void notifyListeners() {
        for (final Listener listener : listeners)
            listener.onCallStateChanged();
    }

    @SuppressWarnings("deprecation")
    public static int getState(Call call) {
        return call.getState(); // getDetails().getState() needs API 31+; compileSdk is 29 and getState() still works
    }

    /**
     * @return the call the screen should show: a ringing call first, then the active one, then the rest.
     */
    @Nullable
    public static Call getPrimaryCall() {
        Call best = null;
        int bestRank = Integer.MAX_VALUE;
        for (final Call call : calls) {
            final int rank = rank(getState(call));
            if (rank < bestRank) {
                best = call;
                bestRank = rank;
            }
        }
        return best;
    }

    /**
     * @return true if there is a call that is not ending / ended.
     */
    public static boolean hasOngoingCall() {
        final Call call = getPrimaryCall();
        if (call == null)
            return false;
        final int state = getState(call);
        return state != Call.STATE_DISCONNECTED && state != Call.STATE_DISCONNECTING;
    }

    private static int rank(int state) {
        switch (state) {
            case Call.STATE_RINGING:
                return 0;
            case Call.STATE_ACTIVE:
                return 1;
            case Call.STATE_NEW:
            case Call.STATE_DIALING:
            case Call.STATE_CONNECTING:
            case Call.STATE_SELECT_PHONE_ACCOUNT:
            case Call.STATE_PULLING_CALL:
                return 2;
            case Call.STATE_HOLDING:
                return 3;
            case Call.STATE_DISCONNECTING:
                return 4;
            default:
                return 5;
        }
    }

    public static void answer(Call call) {
        call.answer(VideoProfile.STATE_AUDIO_ONLY);
    }

    /**
     * Rejects a ringing call, hangs up any other call.
     */
    public static void hangUp(Call call) {
        if (getState(call) == Call.STATE_RINGING)
            call.reject(false, null);
        else
            call.disconnect();
    }

    @SuppressWarnings("deprecation")
    public static boolean isSpeakerOn() {
        if (inCallService == null)
            return false;
        final CallAudioState audioState = inCallService.getCallAudioState();
        return audioState != null && audioState.getRoute() == CallAudioState.ROUTE_SPEAKER;
    }

    @SuppressWarnings("deprecation")
    public static void setSpeaker(boolean on) {
        if (inCallService != null)
            inCallService.setAudioRoute(on ? CallAudioState.ROUTE_SPEAKER : CallAudioState.ROUTE_WIRED_OR_EARPIECE);
    }

    public static boolean isDefaultDialer(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;
        final TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        return telecomManager != null && context.getPackageName().equals(telecomManager.getDefaultDialerPackage());
    }

    /**
     * Asks the user to make GranPhone the default phone app, so incoming and outgoing calls use {@link com.bald.uriah.baldphone.activities.CallActivity}.
     */
    public static void requestDefaultDialer(Activity activity) {
        if (isDefaultDialer(activity)) {
            BaldToast.from(activity).setText(R.string.already_default_dialer).show();
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                final RoleManager roleManager = activity.getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    activity.startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER), REQUEST_CODE_DEFAULT_DIALER);
                    return;
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.startActivityForResult(new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                        .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, activity.getPackageName()), REQUEST_CODE_DEFAULT_DIALER);
                return;
            }
        } catch (Exception ignored) {
        }
        BaldToast.error(activity);
    }

    public interface Listener {
        void onCallStateChanged();
    }
}
