package com.bald.uriah.baldphone.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/**
 * API 33+ predictive-back wiring (the app opts in via the manifest flag
 * android:enableOnBackInvokedCallback). An enabled callback makes the system
 * deliver back events to it instead of performing the default back, so it is
 * only registered where back is actually intercepted — everywhere else the
 * system plays its own predictive animation on Android 15+.
 * On API <33 register() is a no-op and the legacy onBackPressed() path stands.
 */
public final class PredictiveBackHelper {
    private PredictiveBackHelper() {
    }

    /** Custom predictive preview for an intercepted back (settings sub-pages). */
    public interface BackAnimation {
        void onStarted();

        /** Signed progress 0..1: + toward +x (left-edge swipe), - toward -x (right-edge). */
        void onProgressed(float progress);

        void onInvoked();

        void onCancelled();
    }

    /** @return a Runnable that unregisters the callback (no-op before API 33). */
    public static Runnable register(Activity activity, Runnable onBack) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            return () -> {
            };
        return register33(activity, onBack);
    }

    public static Runnable register(Activity activity, BackAnimation animation) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            return () -> {
            };
        return register33(activity, animation);
    }

    @TargetApi(33)
    private static Runnable register33(Activity activity, Runnable onBack) {
        final OnBackInvokedCallback callback = onBack::run;
        final OnBackInvokedDispatcher dispatcher = activity.getOnBackInvokedDispatcher();
        dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
        return () -> dispatcher.unregisterOnBackInvokedCallback(callback);
    }

    @TargetApi(33)
    private static Runnable register33(Activity activity, BackAnimation animation) {
        final OnBackAnimationCallback callback = new OnBackAnimationCallback() {
            @Override
            public void onBackStarted(BackEvent backEvent) {
                animation.onStarted();
            }

            @Override
            public void onBackProgressed(BackEvent backEvent) {
                animation.onProgressed(backEvent.getProgress()
                        * (backEvent.getSwipeEdge() == BackEvent.EDGE_RIGHT ? -1f : 1f));
            }

            @Override
            public void onBackInvoked() {
                animation.onInvoked();
            }

            @Override
            public void onBackCancelled() {
                animation.onCancelled();
            }
        };
        final OnBackInvokedDispatcher dispatcher = activity.getOnBackInvokedDispatcher();
        dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
        return () -> dispatcher.unregisterOnBackInvokedCallback(callback);
    }
}
