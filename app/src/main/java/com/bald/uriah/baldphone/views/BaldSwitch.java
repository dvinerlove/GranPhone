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

package com.bald.uriah.baldphone.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.bald.uriah.baldphone.R;

public class BaldSwitch extends FrameLayout {
    private static final String TAG = BaldSwitch.class.getSimpleName();
    private static final long THUMB_ANIM_MS = 160;
    private @ColorInt
    int textColorOnSelected;
    private @ColorInt
    int textColorOnButton;
    private float size;
    private boolean enabled;
    private boolean checked;
    private Context context;
    private OnChangeListener onChangeListener = new OnChangeListener() {
        @Override
        public void onChange(boolean isChecked) {
        }
    };
    private CharSequence yes, no;

    private TextView tv_yes, tv_no;
    private View thumb;
    private LinearLayout row;
    private int inset;

    public BaldSwitch(Context context) {
        super(context);
        init(context, null);
    }

    public BaldSwitch(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet);
    }

    public BaldSwitch(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet);
        init(context, attributeSet);
    }

    public BaldSwitch(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet);
        init(context, attributeSet);
    }

    private void init(Context context, @Nullable AttributeSet attributeSet) {
        this.context = context;

        final TypedArray styleAttributesArray = context.obtainStyledAttributes(attributeSet, R.styleable.BaldSwitch);
        size = styleAttributesArray.getDimension(R.styleable.BaldSwitch_size, 0);
        yes = styleAttributesArray.getString(R.styleable.BaldSwitch_yes);
        if (yes == null)
            yes = context.getText(R.string.yes);

        no = styleAttributesArray.getString(R.styleable.BaldSwitch_no);
        if (no == null)
            no = context.getText(R.string.no);

        //Dont change, trust me
        enabled = (styleAttributesArray.getBoolean(R.styleable.BaldSwitch_enabled, true));
        setEnabled(enabled);
        //Dont change, trust me
        setFocusable(enabled);
        setClickable(enabled);

        checked = styleAttributesArray.getBoolean(R.styleable.BaldSwitch_checked, false);
        styleAttributesArray.recycle();

        try {
            final TypedValue typedValue = new TypedValue();
            Resources.Theme theme = context.getTheme();
            theme.resolveAttribute(R.attr.bald_text_on_variant, typedValue, true);
            textColorOnSelected = typedValue.data;
            theme.resolveAttribute(R.attr.bald_text_primary, typedValue, true);
            textColorOnButton = typedValue.data;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
            textColorOnSelected = Color.BLACK;
            textColorOnButton = Color.BLACK;
        }
        updateView();

    }

    private void updateView() {
        if (tv_yes == null && tv_no == null)//First Call
        {
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            inset = getResources().getDimensionPixelSize(R.dimen.spacing_xs) / 2;

            thumb = new View(context);
            thumb.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(thumb);

            row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            tv_yes = (TextView) inflater.inflate(R.layout.bald_switch_yes, row, false);
            tv_no = (TextView) inflater.inflate(R.layout.bald_switch_no, row, false);
            tv_yes.setFocusable(false);
            tv_no.setFocusable(false);

            tv_yes.setText(yes);
            tv_yes.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (setCheckedFromUser(true))
                        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                }
            });

            tv_no.setText(no);
            tv_no.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (setCheckedFromUser(false))
                        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                }
            });

            if (size == 0) {
                tv_yes.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.small));
                tv_no.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.small));

            } else {
                tv_yes.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
                tv_no.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
            }

            // "Нет" слева, "Да" справа — подтверждающее действие у края
            row.addView(tv_no);
            row.addView(tv_yes);
        }

        tv_yes.setEnabled(enabled);
        tv_no.setEnabled(enabled);

        if (enabled) {
            setBackground(ContextCompat.getDrawable(context, R.drawable.btn_enabled));
            tv_yes.setBackground(ContextCompat.getDrawable(context, R.drawable.style_for_buttons_transparent));
            tv_no.setBackground(ContextCompat.getDrawable(context, R.drawable.style_for_buttons_transparent));
        } else {
            setBackground(ContextCompat.getDrawable(context, R.drawable.btn_disabled));
            tv_yes.setBackground(null);
            tv_no.setBackground(null);
        }

        final boolean animate = getWidth() > 0;
        syncThumbBackground(checked ? R.drawable.btn_selected_no_border : R.drawable.btn_disabled_no_border, animate);
        syncThumb(animate);
    }

    private boolean thumbBound;
    private int thumbBgRes;
    private Animator thumbAnim;
    private float txNo, txYes;

    private void syncThumbBackground(int resId, boolean animate) {
        if (resId == thumbBgRes)
            return;
        final android.graphics.drawable.Drawable next = ContextCompat.getDrawable(context, resId);
        if (animate && thumbBgRes != 0) {
            final android.graphics.drawable.Drawable prev = ContextCompat.getDrawable(context, thumbBgRes);
            final android.graphics.drawable.TransitionDrawable td =
                    new android.graphics.drawable.TransitionDrawable(new android.graphics.drawable.Drawable[]{prev, next});
            thumb.setBackground(td);
            td.startTransition((int) THUMB_ANIM_MS);
        } else {
            thumb.setBackground(next);
        }
        thumbBgRes = resId;
    }

    private void syncThumb(boolean animate) {
        if (thumb == null)
            return;
        if (getWidth() == 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (!thumbBound)
                        syncThumb(false);
                }
            });
            return;
        }
        final LayoutParams lp = (LayoutParams) thumb.getLayoutParams();
        final int w = getWidth() / 2 - 2 * inset;
        if (lp.width != w || lp.height != LayoutParams.MATCH_PARENT) {
            lp.width = w;
            lp.height = LayoutParams.MATCH_PARENT;
            lp.setMargins(inset, inset, inset, inset);
            thumb.setLayoutParams(lp);
        }
        // laid position comes from lp margins (gravity START), not getLeft() —
        // getLeft() is stale in the same pass that mutates the params
        final int laidLeft = getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? getWidth() - lp.getMarginEnd() - lp.width
                : lp.getMarginStart();
        txNo = row.getLeft() + tv_no.getLeft() + inset - laidLeft;
        txYes = row.getLeft() + tv_yes.getLeft() + inset - laidLeft;
        final float tx = checked ? txYes : txNo;
        tv_no.setTextColor(textColorOnButton);
        if (animate) {
            startThumbAnim(tx);
        } else {
            cancelThumbAnim();
            thumb.setTranslationX(tx);
            applyThumbState();
        }
        thumbBound = true;
    }

    private void startThumbAnim(final float tx) {
        cancelThumbAnim();
        // ObjectAnimator from current position so a mid-flight retarget keeps
        // velocity direction; ViewPropertyAnimator.cancel() snaps to end instead
        final ObjectAnimator anim = ObjectAnimator.ofFloat(thumb, TRANSLATION_X, thumb.getTranslationX(), tx);
        anim.setDuration(THUMB_ANIM_MS);
        anim.setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f));
        anim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator a) {
                applyThumbState();
            }
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                applyThumbState();
            }
        });
        anim.start();
        thumbAnim = anim;
    }

    private void cancelThumbAnim() {
        if (thumbAnim != null) {
            thumbAnim.cancel();
            thumbAnim = null;
        }
    }

    // Text color is driven by thumb *position*, not a parallel clock,
    // so "Yes" only turns light once the green fill is actually under it.
    private void applyThumbState() {
        final float span = txYes - txNo;
        final float p = span == 0 ? (checked ? 1f : 0f)
                : Math.min(1f, Math.max(0f, (thumb.getTranslationX() - txNo) / span));
        tv_yes.setTextColor(enabled
                ? ColorUtils.blendARGB(textColorOnButton, textColorOnSelected, p)
                : textColorOnButton);
    }

    private boolean setCheckedFromUser(boolean newChecked) {
        if (!enabled)
            throw new IllegalStateException("should not be clicked!");
        if (checked == newChecked)
            return false;
        checked = newChecked;
        onChangeListener.onChange(checked);
        updateView();
        return true;
    }

    @Override
    public boolean performClick() {
        if (!enabled)
            return false;
        setCheckedFromUser(!checked);
        // super sends the click event after the checked state has changed.
        super.performClick();
        return true;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(android.widget.Switch.class.getName());
        info.setCheckable(true);
        info.setChecked(checked);
        info.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled)
            return;

        this.enabled = enabled;
        super.setEnabled(enabled);
        setFocusable(enabled);
        setClickable(enabled);
//        for(int i=0;i<getChildCount();i++){
//            getChildAt(i).setEnabled(enabled);
//        }
    }

    public OnChangeListener getOnChangeListener() {
        return onChangeListener;
    }

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    public boolean isChecked() {
        return checked;
    }

    /**
     * WARNING
     * calling this method WILL NOT call yes/no Method
     *
     * @param checked
     */
    public void setChecked(boolean checked) {
        this.checked = checked;
        updateView();
    }

    public interface OnChangeListener {
        void onChange(boolean isChecked);
    }

}
