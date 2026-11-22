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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
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
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        final TypedArray styleAttributesArray = context.obtainStyledAttributes(attributeSet, R.styleable.BaldPictureTextButton);
        final float pxSize = styleAttributesArray.getDimension(R.styleable.BaldPictureTextButton__size, 0);
        final Drawable pic = styleAttributesArray.getDrawable(R.styleable.BaldPictureTextButton__src);
        final String text = styleAttributesArray.getString(R.styleable.BaldPictureTextButton__text);
        final int variant = styleAttributesArray.getInt(R.styleable.BaldPictureTextButton_variant, 0);
        styleAttributesArray.recycle();

        imageView = (ImageView) layoutInflater.inflate(R.layout.bald_picture_text_button_picture, this, false);
        imageView.setImageDrawable(pic);
        addView(imageView);

        textView = (TextView) layoutInflater.inflate(R.layout.bald_picture_text_button_text, this, false);
        textView.setText(text);
        addView(textView);

        if (variant != 0)
            applyVariant(variant);
    }

    /**
     * Tints the stateful background solid with the variant color and the icon/text with
     * {@code bald_text_on_button}. variant: 1=brand, 2=danger, 3=info (0=neutral never gets here).
     */
    private void applyVariant(int variant) {
        final int variantAttr = variant == 1 ? R.attr.bald_brand : variant == 2 ? R.attr.bald_danger : R.attr.bald_info;
        final TypedArray themeColors = context.obtainStyledAttributes(new int[]{variantAttr, R.attr.bald_button_clicked, R.attr.bald_button_disabled, R.attr.bald_text_on_button});
        final int color = themeColors.getColor(0, 0);
        final int clickedColor = themeColors.getColor(1, color);
        final int disabledColor = themeColors.getColor(2, color);
        final int onColor = themeColors.getColor(3, Color.WHITE);
        themeColors.recycle();

        setBackgroundTintList(new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_pressed},
                        new int[]{}},
                new int[]{disabledColor, clickedColor, color}));
        textView.setTextColor(onColor);
        imageView.setImageTintList(ColorStateList.valueOf(onColor));
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
