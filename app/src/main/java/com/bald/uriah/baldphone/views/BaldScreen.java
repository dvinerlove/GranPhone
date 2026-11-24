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
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.R;

/**
 * Screen scaffold: vertical container that prepends a {@link BaldTitleBar}
 * (attrs {@code app:title} / {@code app:show_back} are forwarded to it) and
 * stacks children below it, so content can never slide under the title bar.
 * Background is {@code ?bald_background}; scrollable content should still use
 * {@link ScrollingHelper} inside.
 */
public class BaldScreen extends LinearLayout {
    private BaldTitleBar titleBar;

    public BaldScreen(Context context) {
        super(context);
        init(context, null);
    }

    public BaldScreen(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BaldScreen(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setOrientation(VERTICAL);
        titleBar = new BaldTitleBar(context, attrs);
        final TypedArray themeColors = context.obtainStyledAttributes(
                new int[]{R.attr.bald_background, R.attr.bald_brand});
        setBackgroundColor(themeColors.getColor(0, 0));
        titleBar.setBackgroundTintList(ColorStateList.valueOf(themeColors.getColor(1, 0)));
        themeColors.recycle();
        addView(titleBar, 0, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public BaldTitleBar getTitleBar() {
        return titleBar;
    }
}
