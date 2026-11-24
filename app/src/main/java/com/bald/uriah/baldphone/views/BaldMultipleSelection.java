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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.Const;

import java.util.ArrayList;

public class BaldMultipleSelection extends LinearLayout {
    private @ColorInt
    int textColorOnSelected;
    private @ColorInt
    int textColorOnButton;
    private OnItemClickListener onItemClickListener = (v) -> {
    };
    private Drawable defaultDrawableSelected, defaultDrawable;
    private Context context;
    private LayoutInflater layoutInflater;
    private int selection = -1;
    private int size = 0;
    private float pxDimen = -1;
    private ArrayList<BaldButton> buttons = new ArrayList<>(5);

    public BaldMultipleSelection(Context context) {
        super(context);
        init(context, null);
    }

    public BaldMultipleSelection(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BaldMultipleSelection(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(value = 21)
    public BaldMultipleSelection(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @SuppressLint("WrongConstant")
    private void init(Context context, AttributeSet attrs) {
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);

        final TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.bald_text_on_selected, typedValue, true);
        textColorOnSelected = typedValue.data;
        theme.resolveAttribute(R.attr.bald_text_on_button, typedValue, true);
        textColorOnButton = typedValue.data;

        if (attrs == null) {
            defaultDrawable = ContextCompat.getDrawable(context, R.drawable.bald_button_background);
            defaultDrawableSelected = ContextCompat.getDrawable(context, R.drawable.btn_selected);
            setOrientation(HORIZONTAL);
        } else {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BaldMultipleSelection);
            setOrientation((typedArray.getBoolean(R.styleable.BaldMultipleSelection_is_vertical, false)) ? VERTICAL : HORIZONTAL);
            Drawable drawable = typedArray.getDrawable(R.styleable.BaldMultipleSelection_background_default);
            defaultDrawable = drawable != null ? drawable : ContextCompat.getDrawable(context, R.drawable.bald_button_background);
            Drawable drawableSelected = typedArray.getDrawable(R.styleable.BaldMultipleSelection_background_selected);
            if (drawableSelected != null)
                defaultDrawableSelected = drawableSelected;
            else
                defaultDrawableSelected = ContextCompat.getDrawable(context, R.drawable.btn_selected);
            pxDimen = typedArray.getDimension(R.styleable.BaldMultipleSelection_text_size, -1);
            textColorOnButton = typedArray.getColor(R.styleable.BaldMultipleSelection_text_color, textColorOnButton);
            textColorOnSelected = typedArray.getColor(R.styleable.BaldMultipleSelection_selected_text_color, textColorOnSelected);
            typedArray.recycle();
        }

    }

    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
        for (BaldButton button : buttons) {
            final LayoutParams layoutParams = new LayoutParams(orientation == VERTICAL ? ViewGroup.LayoutParams.MATCH_PARENT : 0, orientation != VERTICAL ? ViewGroup.LayoutParams.MATCH_PARENT : 0);
            int inPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());
            layoutParams.setMargins(orientation == VERTICAL ? 0 : inPx, orientation != VERTICAL ? 0 : inPx, orientation == VERTICAL ? 0 : inPx, orientation != VERTICAL ? 0 : inPx);
            layoutParams.weight = 1f;
            button.setLayoutParams(layoutParams);
        }

    }

    public void addSelection(@StringRes int resId) {
        addSelection(context.getText(resId));
    }

    public void setButtonsDrawable(@DrawableRes int drawable) {
        setButtonsDrawable(ContextCompat.getDrawable(context, drawable));

    }

    public void setButtonsDrawable(Drawable drawable) {
        for (final BaldButton button : buttons)
            button.setBackground(drawable);
        defaultDrawable = drawable;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void addSelection(CharSequence... charSequences) {
        for (CharSequence charSequence : charSequences)
            addSelection(charSequence);
    }

    public void addSelection(@StringRes int... resIds) {
        for (@StringRes int resId : resIds)
            addSelection(resId);
    }

    public void addSelection(CharSequence name) {

        final int index = size++;
        final BaldButton button = (BaldButton) layoutInflater.inflate(R.layout.multiple_vertical_selection_view_item, this, false);
        button.setBackground(defaultDrawable.getConstantState().newDrawable());

        button.setText(name);
        final float maxPx = pxDimen != -1 ? pxDimen
                : context.getResources().getDimension(R.dimen.medium);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, maxPx);
        // setTextSize clears autosize — re-enable so long labels shrink instead of clipping
        // shortcut: autosize can't see weighted widths on this appcompat — shrink once after layout
        button.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or2, int ob) {
                final float avail = button.getWidth() - button.getPaddingLeft() - button.getPaddingRight();
                if (avail <= 0) return;   // keep the listener — first pass can be 0-width
                v.removeOnLayoutChangeListener(this);
                final android.graphics.Paint paint = new android.graphics.Paint(button.getPaint());
                paint.setTextSize(maxPx);
                final float wanted = paint.measureText(button.getText().toString());
                if (wanted > avail) {
                    final float minPx = 12 * getResources().getDisplayMetrics().scaledDensity;
                    button.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            Math.max(minPx, maxPx * avail / wanted * 0.95f));
                }
            }
        });
        buttons.add(button);
        final LayoutParams layoutParams = new LayoutParams(getOrientation() == VERTICAL ? ViewGroup.LayoutParams.MATCH_PARENT : 0, getOrientation() != VERTICAL ? ViewGroup.LayoutParams.MATCH_PARENT : 0);
        int inPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());
        layoutParams.setMargins(getOrientation() == VERTICAL ? 0 : inPx, getOrientation() != VERTICAL ? 0 : inPx, getOrientation() == VERTICAL ? 0 : inPx, getOrientation() != VERTICAL ? 0 : inPx);
        layoutParams.weight = 1f;
        this.addView(button, layoutParams);
        button.setOnClickListener(Const.longer);
        button.setTextColor(textColorOnButton);
        button.setOnClickListener(v -> {
            if (selection == index)
                return;
            if (selection != -1)
                setClicked(buttons.get(selection), false);
            selection = index;
            setClicked(buttons.get(selection), true);
            button.announceForAccessibility(button.getText());
            onItemClickListener.onItemClick(getSelection());
        });
        if (selection == -1) {
            selection = index;
            setClicked(button, true);
        }

        //TODO DELETE
    }

    public int getSelection() {
        if (selection == -1)
            throw new RuntimeException("nothing to select from!");

        return selection;
    }

    public void setSelection(int selectionIndex) {
        setClicked(buttons.get(selection), false);
        this.selection = selectionIndex;
        setClicked(buttons.get(selection), true);

    }

    private void setClicked(BaldButton button, boolean state) {
        // no app drawable keys off state_selected — this flag is purely for TalkBack
        button.setSelected(state);
        if (state) {
            button.setBackground(defaultDrawableSelected.getConstantState().newDrawable());
            button.setTextColor(textColorOnSelected);
        } else {
            button.setBackground(defaultDrawable.getConstantState().newDrawable());
            button.setTextColor(textColorOnButton);

        }

    }

    @FunctionalInterface
    public interface OnItemClickListener {
        void onItemClick(int whichItem);
    }
}
