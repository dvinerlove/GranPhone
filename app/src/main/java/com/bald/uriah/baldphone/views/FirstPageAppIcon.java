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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.bald.uriah.baldphone.R;

public class FirstPageAppIcon extends BaldFrameLayoutButton {
    protected final Context context;
    protected final LayoutInflater layoutInflater;
    public ImageView imageView;
    public TextView textView;
    public View badge;
    private Drawable defaultDrawable;
    private CharSequence defaultText;

    public FirstPageAppIcon(Context context) {
        super(context);
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        init(null);
    }

    public FirstPageAppIcon(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        init(attrs);
    }

    public FirstPageAppIcon(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attributeSet) {
        layoutInflater.inflate(R.layout.first_page_app_icon, this, true);
        imageView = findViewById(R.id.iv);
        textView = findViewById(R.id.tv);
        badge = findViewById(R.id.notifications_counter);

        if (attributeSet != null) {
            final TypedArray styleAttributesArray = context.obtainStyledAttributes(attributeSet, R.styleable.FirstPageAppIcon);
            defaultDrawable = styleAttributesArray.getDrawable(R.styleable.FirstPageAppIcon___src);
            defaultText = styleAttributesArray.getString(R.styleable.FirstPageAppIcon___text);
            setImageDrawable(defaultDrawable);
            setText(defaultText);
            styleAttributesArray.recycle();
        }

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

    public void setBadgeVisibility(boolean visible) {
        badge.setVisibility(visible ? VISIBLE : INVISIBLE);
        badge.setContentDescription(visible ? context.getString(R.string.new_notifications) : null);
        badge.setImportantForAccessibility(visible ? IMPORTANT_FOR_ACCESSIBILITY_YES : IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public CharSequence getText() {
        return textView.getText();
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public void setText(@StringRes int resId) {
        textView.setText(resId);
    }

    public CharSequence getDefaultText() {
        return defaultText;
    }

    /** Restores the icon/label declared in XML — used when a slot reverts from a custom app/contact to its default. */
    public void resetToDefault() {
        imageView.setImageDrawable(defaultDrawable);
        textView.setText(defaultText);
    }
}
