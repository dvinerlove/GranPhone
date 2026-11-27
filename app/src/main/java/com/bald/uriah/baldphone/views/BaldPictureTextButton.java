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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.bald.uriah.baldphone.R;

public class BaldPictureTextButton extends BaldLinearLayoutButton {

    // Same values as the XML `variant` enum in attrs.xml — keep in sync.
    public static final int VARIANT_NEUTRAL = 0;
    public static final int VARIANT_BRAND = 1;
    public static final int VARIANT_DANGER = 2;
    public static final int VARIANT_INFO = 3;
    public static final int VARIANT_TINTED = 4;
    public static final int VARIANT_DANGER_OUTLINE = 5;

    protected final Context context;
    protected final LayoutInflater layoutInflater;
    protected ImageView imageView;
    protected TextView textView;
    private CharSequence textContentDescription;
    private ColorStateList defaultTextColors;

    public BaldPictureTextButton(Context context) {
        super(context);
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public BaldPictureTextButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        init(attrs);
    }

    public BaldPictureTextButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        init(attrs);
    }

    public BaldPictureTextButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        init(attrs);
    }

    private void init(AttributeSet attributeSet) {
        // android:orientation on the element is parsed by the LinearLayout ctor but
        // would be stomped here — honor it so narrow tiles can stack icon over text.
        final int orientation = attributeSet == null ? HORIZONTAL :
                attributeSet.getAttributeIntValue("http://schemas.android.com/apk/res/android", "orientation", HORIZONTAL);
        setOrientation(orientation);
        setGravity(Gravity.CENTER);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        // Uniform vertical inset for every button instance — keeps icon+label centered
        // and identical across rows. Horizontal padding stays caller-controlled.
        final int padV = getResources().getDimensionPixelSize(R.dimen.spacing_sm);
        setPadding(getPaddingLeft(), padV, getPaddingRight(), padV);
        final TypedArray styleAttributesArray = context.obtainStyledAttributes(attributeSet, R.styleable.BaldPictureTextButton);
        final float pxSize = styleAttributesArray.getDimension(R.styleable.BaldPictureTextButton__size, 0);
        final Drawable pic = styleAttributesArray.getDrawable(R.styleable.BaldPictureTextButton__src);
        final String text = styleAttributesArray.getString(R.styleable.BaldPictureTextButton__text);
        final int variant = styleAttributesArray.getInt(R.styleable.BaldPictureTextButton_variant, 0);
        styleAttributesArray.recycle();

        imageView = (ImageView) layoutInflater.inflate(R.layout.bald_picture_text_button_picture, this, false);
        imageView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setImageDrawable(pic);
        if (orientation == VERTICAL) {
            final int iconSize = getResources().getDimensionPixelSize(R.dimen.icon_md);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        }
        addView(imageView);

        textView = (TextView) layoutInflater.inflate(R.layout.bald_picture_text_button_text, this, false);
        textView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setText(text);
        if (orientation == HORIZONTAL)
            // Icon+label must center as one group — weight-1 text centers only itself.
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        if (pxSize != 0)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, pxSize);
        // AppCompat 1.1.0 autosize is unreliable here (ellipsize+weighted width), so
        // shrink manually once real width exists — same pattern as HomePage1.shrinkToFit.
        textView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            final float avail = v.getWidth() - v.getPaddingStart() - v.getPaddingEnd();
            final CharSequence s = textView.getText();
            if (avail <= 0 || s == null || s.length() == 0)
                return;
            // Shrink only when the longest single word can't fit — multi-word
            // labels should wrap (maxLines=2), not shrink.
            float wanted = 0;
            for (String word : s.toString().split("\\s+"))
                wanted = Math.max(wanted, textView.getPaint().measureText(word));
            final float floor = 8 * getResources().getDisplayMetrics().scaledDensity;
            if (wanted > avail && textView.getTextSize() > floor)
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        Math.max(floor, textView.getTextSize() * (avail / wanted) * 0.95f));
        });
        if (orientation == VERTICAL)
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        addView(textView);

        // captured before any variant ink so setVariant(VARIANT_NEUTRAL) can restore it
        defaultTextColors = textView.getTextColors();
        // constructor neutral is a no-op: XML pairing like ghost backgrounds must survive
        // (single_media_activity); the reset semantics of setVariant are for runtime switches.
        if (variant != VARIANT_NEUTRAL)
            applyVariant(variant);
    }

    /**
     * Applies a BigButton variant at runtime; same values as the XML {@code variant} enum.
     * {@link #VARIANT_NEUTRAL} fully restores the neutral button look: default stateful
     * background, original text color, no icon tint.
     */
    public void setVariant(int variant) {
        if (variant == VARIANT_NEUTRAL) {
            setBackgroundResource(R.drawable.bald_button_background);
            if (defaultTextColors != null)
                textView.setTextColor(defaultTextColors);
            imageView.setImageTintList(null);
            return;
        }
        applyVariant(variant);
    }

    /**
     * Tints the stateful background solid with the variant color and the icon/text with
     * {@code bald_text_on_variant} — {@code bald_text_primary} for the light {@code tinted}
     * variant. variant: 1=brand, 2=danger, 3=info, 4=tinted (0=neutral never gets here).
     */
    private void applyVariant(int variant) {
        if (variant == VARIANT_DANGER_OUTLINE) {
            // danger_outline: neutral surface (keeps the current button bg), red ink.
            // Reserved for secondary destructive actions like Delete.
            final TypedArray c = context.obtainStyledAttributes(new int[]{R.attr.bald_danger, R.attr.bald_text_primary});
            final int danger = c.getColor(0, 0);
            final int disabledText = c.getColor(1, danger);
            c.recycle();
            final ColorStateList ink = new ColorStateList(
                    new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}},
                    new int[]{disabledText, danger});
            textView.setTextColor(ink);
            imageView.setImageTintList(ink);
            return;
        }
        final int variantAttr = variant == 1 ? R.attr.bald_brand : variant == 2 ? R.attr.bald_danger : variant == 3 ? R.attr.bald_info : R.attr.bald_brand_tint;
        final int onAttr = variant == 4 ? R.attr.bald_text_primary : R.attr.bald_text_on_variant;
        final TypedArray themeColors = context.obtainStyledAttributes(new int[]{variantAttr, R.attr.bald_button_disabled, onAttr, R.attr.bald_text_primary});
        final int color = themeColors.getColor(0, 0);
        final int disabledColor = themeColors.getColor(1, color);
        final int onColor = themeColors.getColor(2, Color.WHITE);
        final int disabledTextColor = themeColors.getColor(3, onColor);
        themeColors.recycle();
        // focus stroke = the variant's own foreground color, so it contrasts with the
        // colored fill (bald_decoration_on_background is invisible on brand/danger fills)
        final int focusStrokeColor = onColor;

        // Pressed = darkened variant fill so onColor keeps its contrast (neutral
        // bald_button_clicked gave ~1.4-1.9:1 label flashes on variant buttons).
        final int pressedColor = Color.rgb(
                (int) (Color.red(color) * 0.8f), (int) (Color.green(color) * 0.8f), (int) (Color.blue(color) * 0.8f));

        // backgroundTintList on a <selector> only reaches the <stroke> on old APIs,
        // leaving a white fill + colored frame — build explicit state shapes instead.
        final float radius = getResources().getDimension(R.dimen.radius_button);
        final android.graphics.drawable.StateListDrawable bg = new android.graphics.drawable.StateListDrawable();
        bg.addState(new int[]{-android.R.attr.state_enabled}, variantShape(disabledColor, radius, 0, 0));
        bg.addState(new int[]{android.R.attr.state_pressed}, variantShape(pressedColor, radius, 0, 0));
        bg.addState(new int[]{android.R.attr.state_focused}, variantShape(color, radius,
                getResources().getDimensionPixelSize(R.dimen.border_width_selected), focusStrokeColor));
        bg.addState(new int[]{}, variantShape(color, radius, 0, 0));
        setBackground(bg);
        textView.setTextColor(new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}},
                new int[]{disabledTextColor, onColor}));
        imageView.setImageTintList(new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}},
                new int[]{disabledTextColor, onColor}));
    }

    private static android.graphics.drawable.Drawable variantShape(int color, float radius, int strokeWidth, int strokeColor) {
        final android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setCornerRadius(radius);
        d.setColor(color);
        if (strokeWidth > 0)
            d.setStroke(strokeWidth, strokeColor);
        return d;
    }

    // Empty label/icon is GONE, not blank-VISIBLE — a zero-content weighted child
    // would still eat half the height and shove the other child off-center.
    public void setText(CharSequence text) {
        textView.setText(text);
        textView.setVisibility(text == null || text.length() == 0 ? GONE : VISIBLE);
        if (getContentDescription() == null || getContentDescription().equals(textContentDescription)) {
            textContentDescription = text;
            setContentDescription(text);
        }
    }

    public void setText(@StringRes int resId) {
        setText(getContext().getText(resId));
    }

    public void setImageDrawable(@Nullable Drawable drawable) {
        imageView.setImageDrawable(drawable);
        imageView.setVisibility(drawable == null ? GONE : VISIBLE);
    }

    public void setImageBitmap(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
        imageView.setVisibility(bitmap == null ? GONE : VISIBLE);
    }

    public void setImageResource(@DrawableRes int resId) {
        imageView.setImageResource(resId);
        imageView.setVisibility(resId == 0 ? GONE : VISIBLE);
    }

    public ImageView getImageView() {
        return imageView;
    }

    public TextView getTextView() {
        return textView;
    }

}
