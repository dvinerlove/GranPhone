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
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.widget.ScrollView;

/**
 * {@link ScrollView} capped at 40% of the screen height, so dialogs with long
 * content never push their pinned title/buttons off-screen.
 */
public class MaxHeightScrollView extends ScrollView {
    public MaxHeightScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // shortcut: hard 40%-of-screen cap; upgrade to a styleable attr if a second consumer needs a different cap
        final int max = getResources().getDisplayMetrics().heightPixels * 2 / 5;
        if (MeasureSpec.getSize(heightMeasureSpec) > max)
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
