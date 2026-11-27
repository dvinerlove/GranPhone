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
import android.widget.LinearLayout;

import com.bald.uriah.baldphone.R;

/** Dialog root capped inside the viewport; its weighted scroll child absorbs the overflow. */
public class DialogMaxHeightLayout extends LinearLayout {
    public DialogMaxHeightLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxHeight = getResources().getDisplayMetrics().heightPixels
                - 2 * getResources().getDimensionPixelSize(R.dimen.spacing_xl);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.UNSPECIFIED)
            maxHeight = Math.min(maxHeight, MeasureSpec.getSize(heightMeasureSpec));
        if (heightMode == MeasureSpec.EXACTLY && MeasureSpec.getSize(heightMeasureSpec) <= maxHeight) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // AT_MOST still lets LinearLayout shrink the weighted scroll child when the cap is hit.
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
    }
}
