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

    protected final Context context;
    protected final LayoutInflater layoutInflater;
    protected ImageView imageView;
    protected TextView textView;

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
        final TypedArray styleAttributesArray = context.obtainStyledAttributes(attributeSet, R.styleable.BaldPictureTextButton);
        final float pxSize = styleAttributesArray.getDimension(R.styleable.BaldPictureTextButton__size, 0);
        final Drawable pic = styleAttributesArray.getDrawable(R.styleable.BaldPictureTextButton__src);
        final String text = styleAttributesArray.getString(R.styleable.BaldPictureTextButton__text);
        final int variant = styleAttributesArray.getInt(R.styleable.BaldPictureTextButton_variant, 0);
        styleAttributesArray.recycle();

        imageView = (ImageView) layoutInflater.inflate(R.layout.bald_picture_text_button_picture, this, false);
        imageView.setImageDrawable(pic);
        if (orientation == VERTICAL)
            imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        addView(imageView);

        textView = (TextView) layoutInflater.inflate(R.layout.bald_picture_text_button_text, this, false);
        textView.setText(text);
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
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        addView(textView);

        if (variant != 0)
            applyVariant(variant);
    }

    /**
     * Tints the stateful background solid with the variant color and the icon/text with
     * {@code bald_text_on_variant}. variant: 1=brand, 2=danger, 3=info (0=neutral never gets here).
     */
    private void applyVariant(int variant) {
        final int variantAttr = variant == 1 ? R.attr.bald_brand : variant == 2 ? R.attr.bald_danger : R.attr.bald_info;
        final TypedArray themeColors = context.obtainStyledAttributes(new int[]{variantAttr, R.attr.bald_button_disabled, R.attr.bald_text_on_variant, R.attr.bald_text_primary});
        final int color = themeColors.getColor(0, 0);
        final int disabledColor = themeColors.getColor(1, color);
        final int onColor = themeColors.getColor(2, Color.WHITE);
        final int disabledTextColor = themeColors.getColor(3, onColor);
        themeColors.recycle();

        // Pressed = darkened variant fill so onColor keeps its contrast (neutral
        // bald_button_clicked gave ~1.4-1.9:1 label flashes on variant buttons).
        final int pressedColor = Color.rgb(
                (int) (Color.red(color) * 0.8f), (int) (Color.green(color) * 0.8f), (int) (Color.blue(color) * 0.8f));

        setBackgroundTintList(new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_pressed},
                        new int[]{}},
                new int[]{disabledColor, pressedColor, color}));
        textView.setTextColor(new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}},
                new int[]{disabledTextColor, onColor}));
        imageView.setImageTintList(new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}},
                new int[]{disabledTextColor, onColor}));
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public void setText(@StringRes int resId) {
        textView.setText(resId);
    }

    public void setImageDrawable(@Nullable Drawable drawable) {
        imageView.setImageDrawable(drawable);
    }

    public void setImageBitmap(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
    }

    public void setImageResource(@DrawableRes int resId) {
        imageView.setImageResource(resId);
    }

    public ImageView getImageView() {
        return imageView;
    }

    public TextView getTextView() {
        return textView;
    }

}
