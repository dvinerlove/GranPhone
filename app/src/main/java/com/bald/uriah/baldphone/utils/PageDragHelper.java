package com.bald.uriah.baldphone.utils;

import android.app.Activity;
import android.app.ActivityOptions;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import java.lang.reflect.Method;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsSeekBar;

/**
 * Finger-following horizontal "page turn" back-drag, iOS/predictive-back style:
 * the page translates 1:1 under the finger while the window goes translucent so
 * the real previous activity is revealed underneath (with a slight parallax);
 * the commit decision happens on release — a short drag springs back and can be
 * re-grabbed mid-animation. One translationX per frame — cheap on weak devices.
 * <p>
 * Call {@link #dispatch} from {@link Activity#dispatchTouchEvent} before super;
 * it only consumes events once a real horizontal drag starts (sending CANCEL to
 * children so taps and scrolls don't misfire).
 */
public class PageDragHelper {
    private static final int VELOCITY_COMMIT = 800; // px/s
    private static final float COMMIT_FRACTION = 0.4f;
    private static final float PARALLAX = 0.25f;    // revealed page trails like iOS
    private final Activity activity;
    private final Window window;
    private final View target;
    private final int dir;                 // +1 page leaves toward +x, -1 toward -x
    private final int touchSlop;
    private final Runnable onCommit;
    private boolean dragging;
    private boolean skipped;         // gesture started on a horizontally-scrolling child
    private boolean revealed;        // window converted to translucent for this drag
    private View underneath;         // previous activity's decor — parallaxes in
    private Drawable savedWindowBg;
    private Drawable savedContentBg;
    // window translucency: public (ActivityOptions) before 29, hidden (Bundle)
    // since 29 — same trick androidx SlidingPaneLayout uses; sTranslucentBroken
    // on a ROM that has neither → no reveal, everything still works
    private static Method sToTranslucent;
    private static Method sFromTranslucent;
    private static boolean sTakesOptions;
    private static boolean sTranslucentBroken;
    private float downX, downY;
    private float rawDownX;          // untouched touch point for the child hit test
    private VelocityTracker tracker;

    public PageDragHelper(Activity activity, View target, int direction, Runnable onCommit) {
        this.activity = activity;
        this.window = activity.getWindow();
        this.target = target;
        this.dir = direction;
        this.onCommit = onCommit;
        this.touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();
    }

    /** The decor view of the activity below this one, revealed during the drag. */
    public void setUnderneath(View underneath) {
        this.underneath = underneath;
    }

    /**
     * @param active the gesture is currently meaningful (e.g. only on settings root page);
     *               horizontal drags are still swallowed when inactive so they can't
     *               land as accidental taps inside a full-width row
     * @param follow translate with the finger; false = swipe still commits but nothing
     *               moves (animations disabled)
     * @param reveal make the window translucent so the previous activity shows
     *               underneath while dragging; false for in-app page turns
     * @return true when the event was consumed by the drag
     */
    public boolean dispatch(MotionEvent ev, boolean active, boolean follow, boolean reveal) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // a page caught mid-slide belongs to the new gesture:
                // keep its offset so tracking continues without a jump
                target.animate().cancel();
                if (underneath != null)
                    underneath.animate().cancel();
                downX = ev.getX() - target.getTranslationX();
                downY = ev.getY();
                rawDownX = ev.getX();
                dragging = false;
                skipped = false;
                tracker = VelocityTracker.obtain();
                tracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // multi-touch aborts the gesture, like the platform's edge swipe
                if (dragging) {
                    dragging = false;
                    recycleTracker();
                    springBack(follow);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                if (tracker != null)
                    tracker.addMovement(ev);
                final float dx = ev.getX() - downX;
                if (!dragging) {
                    if (Math.abs(dx) <= touchSlop || Math.abs(dx) <= Math.abs(ev.getY() - downY))
                        return false;
                    // let children that scroll horizontally (pagers, carousels)
                    // keep their gesture instead of stealing it for back
                    if (skipped || (skipped = scrollsHorizontally(window.getDecorView(), rawDownX, downY)))
                        return false;
                    dragging = true;
                    // steal the gesture: children see CANCEL so no click/scroll fires
                    final MotionEvent cancel = MotionEvent.obtain(ev);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    window.superDispatchTouchEvent(cancel);
                    cancel.recycle();
                    if (active && follow && reveal)
                        enterReveal();
                }
                if (active) {
                    // tracking only — the user can still reverse until release;
                    // the commit decision happens on UP like the platform gesture
                    float t = dx * dir < 0 ? 0 : dx;
                    final int w = target.getWidth();
                    if (Math.abs(t) > w)
                        t = dir * w;
                    if (follow) {
                        target.setTranslationX(t);
                        if (revealed && underneath != null)
                            underneath.setTranslationX((t * dir - w) * dir * PARALLAX);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!dragging) {
                    recycleTracker();
                    return false;
                }
                dragging = false;
                float vx = 0;
                if (tracker != null) {
                    tracker.addMovement(ev);
                    tracker.computeCurrentVelocity(1000);
                    vx = tracker.getXVelocity();
                }
                recycleTracker();
                final int w = target.getWidth();
                final float settled = follow ? target.getTranslationX() : ev.getX() - downX;
                if (active && (settled * dir > w * COMMIT_FRACTION || vx * dir > VELOCITY_COMMIT)) {
                    if (follow) {
                        target.animate().translationX(dir * w).setDuration(200).start();
                        settleUnderneath(true);
                    }
                    onCommit.run();
                } else {
                    springBack(follow);
                }
                return true;
        }
        return false;
    }

    private void springBack(boolean follow) {
        if (follow && target.getTranslationX() != 0) {
            target.animate().translationX(0).rotationY(0).setDuration(200)
                    .withEndAction(() -> {
                        // runs on cancel too — only restore when fully back
                        if (revealed && target.getTranslationX() == 0)
                            exitReveal();
                    }).start();
        } else {
            exitReveal();
        }
        settleUnderneath(false);
    }

    /** Committed: the revealed page glides in; aborted: it recedes back out. */
    private void settleUnderneath(boolean committed) {
        if (underneath != null && underneath.isAttachedToWindow())
            underneath.animate()
                    .translationX(committed ? 0 : -target.getWidth() * dir * PARALLAX)
                    .setDuration(200).start();
    }

    private void enterReveal() {
        if (revealed)
            return;
        if (!ensureTranslucentMethods()) {
            revealed = false;
            return;
        }
        revealed = true;
        try {
            final ActivityOptions options = ActivityOptions.makeCustomAnimation(activity, 0, 0);
            sToTranslucent.invoke(activity, null, sTakesOptions ? options : options.toBundle());
        } catch (Exception e) {
            revealed = false;
            return;
        }
        // the page must slide as an opaque card: borrow the window background
        savedWindowBg = window.getDecorView().getBackground();
        savedContentBg = target.getBackground();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        target.setBackground(savedWindowBg);
    }

    private void exitReveal() {
        if (!revealed)
            return;
        revealed = false;
        window.setBackgroundDrawable(savedWindowBg);
        target.setBackground(savedContentBg);
        try {
            sFromTranslucent.invoke(activity);
        } catch (Exception ignored) {
        }
    }

    private static boolean ensureTranslucentMethods() {
        if (sTranslucentBroken)
            return false;
        if (sToTranslucent == null) {
            try {
                final Class<?> listenerClass =
                        Class.forName("android.app.Activity$TranslucentConversionListener");
                try {
                    sToTranslucent = Activity.class.getDeclaredMethod(
                            "convertToTranslucent", listenerClass, ActivityOptions.class);
                    sTakesOptions = true;
                } catch (NoSuchMethodException e) {
                    sToTranslucent = Activity.class.getDeclaredMethod(
                            "convertToTranslucent", listenerClass, Bundle.class);
                }
                sToTranslucent.setAccessible(true);
                sFromTranslucent = Activity.class.getDeclaredMethod("convertFromTranslucent");
                sFromTranslucent.setAccessible(true);
            } catch (Throwable t) {
                sTranslucentBroken = true;
                return false;
            }
        }
        return true;
    }

    private void recycleTracker() {
        if (tracker != null) {
            tracker.recycle();
            tracker = null;
        }
    }

    /** Deepest-first hit test: true when the touch point sits on a view that wants horizontal drags. */
    private static boolean scrollsHorizontally(View parent, float x, float y) {
        if (parent.getVisibility() != View.VISIBLE || !parent.isShown())
            return false;
        // SeekBar thumbs aren't "scrolling" but eat horizontal drags all the same
        if (parent instanceof AbsSeekBar
                || parent.canScrollHorizontally(-1) || parent.canScrollHorizontally(1))
            return true;
        if (!(parent instanceof ViewGroup))
            return false;
        final ViewGroup group = (ViewGroup) parent;
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            final View child = group.getChildAt(i);
            if (x >= child.getLeft() && x <= child.getRight()
                    && y >= child.getTop() && y <= child.getBottom()
                    && scrollsHorizontally(child, x - child.getLeft(), y - child.getTop()))
                return true;
        }
        return false;
    }
}
