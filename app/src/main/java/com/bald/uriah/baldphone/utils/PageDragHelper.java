package com.bald.uriah.baldphone.utils;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;

/**
 * Finger-following horizontal "page turn" drag for the Settings back path
 * (Settings &rarr; Home, and settings sub-page &rarr; parent category):
 * the page translates 1:1 under the finger; once it crosses ~30%
 * (or a fast fling) it commits mid-drag so the other activity's window slide
 * completes the turn while the finger is still down. Short drags spring back.
 * One translationX per frame — deliberately cheap for weak devices.
 * <p>
 * Call {@link #dispatch} from {@link Activity#dispatchTouchEvent} before super;
 * it only consumes events once a real horizontal drag starts (sending CANCEL to
 * children so taps and scrolls don't misfire).
 */
public class PageDragHelper {
    private static final int VELOCITY_COMMIT = 800; // px/s
    private static final float COMMIT_FRACTION = 0.3f;
    private final Window window;
    private final View target;
    private final int dir;                 // +1 page leaves toward +x, -1 toward -x
    private final int touchSlop;
    private final Runnable onCommit;
    private boolean dragging;
    private boolean committed;
    private float downX, downY;
    private VelocityTracker tracker;

    public PageDragHelper(Activity activity, View target, int direction, Runnable onCommit) {
        this.window = activity.getWindow();
        this.target = target;
        this.dir = direction;
        this.onCommit = onCommit;
        this.touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();
    }

    /**
     * @param active the gesture is currently meaningful (e.g. only on settings root page);
     *               horizontal drags are still swallowed when inactive so they can't
     *               land as accidental taps inside a full-width row
     * @param follow translate with the finger; false = swipe still commits but nothing
     *               moves (animations disabled)
     * @return true when the event was consumed by the drag
     */
    public boolean dispatch(MotionEvent ev, boolean active, boolean follow) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                dragging = false;
                committed = false;
                tracker = VelocityTracker.obtain();
                tracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                if (tracker != null)
                    tracker.addMovement(ev);
                final float dx = ev.getX() - downX;
                if (!dragging) {
                    // one commit per gesture: after a mid-drag commit the rest of
                    // the stream is swallowed, else UP would fire onCommit again
                    // (e.g. sub-page→main, then the same swipe exits the activity)
                    if (committed)
                        return true;
                    if (Math.abs(dx) <= touchSlop || Math.abs(dx) <= Math.abs(ev.getY() - downY))
                        return false;
                    dragging = true;
                    // steal the gesture: children see CANCEL so no click/scroll fires
                    final MotionEvent cancel = MotionEvent.obtain(ev);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    window.superDispatchTouchEvent(cancel);
                    cancel.recycle();
                }
                if (committed)
                    return true;
                if (active) {
                    float t = dx * dir < 0 ? 0 : dx;
                    final int w = target.getWidth();
                    if (Math.abs(t) > w)
                        t = dir * w;
                    if (follow)
                        target.setTranslationX(t);
                    if (t * dir > w * COMMIT_FRACTION) {
                        // commit mid-drag: the incoming window slide finishes the page turn
                        committed = true;
                        dragging = false;
                        if (follow)
                            target.animate().translationX(dir * w).rotationY(0f)
                                    .setDuration(200).start();
                        onCommit.run();
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!dragging) {
                    recycleTracker();
                    return committed;
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
                if (active && (settled * dir > w / 3f || vx * dir > VELOCITY_COMMIT)) {
                    committed = true;
                    if (follow)
                        target.animate().translationX(dir * w).setDuration(200).start();
                    onCommit.run();
                } else if (follow && (target.getTranslationX() != 0 || target.getRotationY() != 0)) {
                    target.animate().translationX(0).rotationY(0).setDuration(200).start();
                }
                return true;
        }
        return false;
    }

    private void recycleTracker() {
        if (tracker != null) {
            tracker.recycle();
            tracker = null;
        }
    }
}
