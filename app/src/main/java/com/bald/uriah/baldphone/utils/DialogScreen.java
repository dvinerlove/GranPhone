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

package com.bald.uriah.baldphone.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.BaldActivity;
import com.bald.uriah.baldphone.views.BaldButton;

/**
 * The single redesign dialog component, three modes:
 * SELECT - vertical option list, applies instantly on tap, "Назад" text button at bottom. No OK/Cancel/X.
 * CONFIRM - message + one verb-labeled action + one "Отмена".
 * INFO - message + one "Понятно".
 */
public class DialogScreen extends Dialog {
    private static final float DIM_LEVEL = 0.9f;

    private final Context context;
    private CharSequence title, message;
    private CharSequence[] options;
    private int selectedIndex;
    private CharSequence primaryText, secondaryText;
    private Runnable onPrimary;
    private OnOptionSelectedListener onOptionSelected;
    private boolean primaryIsTextButton;

    private TextView tv_title, tv_message;
    private LinearLayout optionsContainer;
    private BaldButton bt_primary, bt_secondary;

    private DialogScreen(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    /**
     * INFO mode: title + message + one "Понятно" button.
     */
    public static DialogScreen info(@NonNull Context context, @StringRes int titleRes, @StringRes int messageRes) {
        final DialogScreen dialog = new DialogScreen(context);
        dialog.title = context.getText(titleRes);
        dialog.message = context.getText(messageRes);
        dialog.primaryText = context.getText(R.string.understood);
        return dialog.present();
    }

    /**
     * CONFIRM mode: title + message + one verb-labeled action + one "Отмена".
     */
    public static DialogScreen confirm(@NonNull Context context, @StringRes int titleRes, @StringRes int messageRes,
                                       @StringRes int actionRes, @NonNull Runnable onAction) {
        return confirm(context, context.getText(titleRes), context.getText(messageRes), actionRes, onAction);
    }

    public static DialogScreen confirm(@NonNull Context context, @NonNull CharSequence title, @NonNull CharSequence message,
                                       @StringRes int actionRes, @NonNull Runnable onAction) {
        final DialogScreen dialog = new DialogScreen(context);
        dialog.title = title;
        dialog.message = message;
        dialog.primaryText = context.getText(actionRes);
        dialog.secondaryText = context.getText(R.string.cancel);
        dialog.onPrimary = onAction;
        return dialog.present();
    }

    /**
     * SELECT mode: title + vertical option list (instant apply on tap) + "Назад" text button at bottom.
     */
    public static DialogScreen select(@NonNull Context context, @StringRes int titleRes, int selectedIndex,
                                      @NonNull OnOptionSelectedListener listener, @StringRes int... optionRes) {
        final CharSequence[] options = new CharSequence[optionRes.length];
        for (int i = 0; i < optionRes.length; i++)
            options[i] = context.getText(optionRes[i]);
        return select(context, titleRes, selectedIndex, listener, options);
    }

    public static DialogScreen select(@NonNull Context context, @StringRes int titleRes, int selectedIndex,
                                      @NonNull OnOptionSelectedListener listener, @NonNull CharSequence... options) {
        return select(context, context.getText(titleRes), selectedIndex, listener, options);
    }

    public static DialogScreen select(@NonNull Context context, @NonNull CharSequence title, int selectedIndex,
                                      @NonNull OnOptionSelectedListener listener, @NonNull CharSequence... options) {
        final DialogScreen dialog = new DialogScreen(context);
        dialog.title = title;
        dialog.options = options;
        dialog.selectedIndex = selectedIndex;
        dialog.onOptionSelected = listener;
        dialog.primaryText = context.getText(R.string.back);
        dialog.primaryIsTextButton = true;
        return dialog.present();
    }

    private DialogScreen present() {
        show();
        final Window window = getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); // required, otherwise setDimAmount has no effect
        window.setDimAmount(DIM_LEVEL);
        if (context instanceof BaldActivity)
            ((BaldActivity) context).autoDismiss(this);
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_screen);
        tv_title = findViewById(R.id.dialog_screen_title);
        tv_message = findViewById(R.id.dialog_screen_message);
        optionsContainer = findViewById(R.id.dialog_screen_options);
        bt_primary = findViewById(R.id.dialog_screen_button_primary);
        bt_secondary = findViewById(R.id.dialog_screen_button_secondary);

        tv_title.setText(title);
        if (message == null)
            tv_message.setVisibility(View.GONE);
        else
            tv_message.setText(message);

        if (options != null)
            inflateOptions();

        if (secondaryText == null) {
            bt_secondary.setVisibility(View.GONE);
        } else {
            bt_secondary.setText(secondaryText);
            // cancel() (not dismiss()) so a set OnCancelListener fires — same as a back-press cancel
            bt_secondary.setOnClickListener(v -> cancel());
        }

        bt_primary.setText(primaryText);
        bt_primary.setOnClickListener(v -> {
            if (onPrimary != null) {
                onPrimary.run();
                dismiss();
            } else {
                // SELECT's «Назад» / INFO's «Понятно» are dismissals, not actions — route through cancel()
                cancel();
            }
        });
        if (primaryIsTextButton) {
            bt_primary.setBackgroundResource(R.drawable.style_for_buttons_transparent);
            bt_primary.setTextColor(resolveThemeColor(R.attr.bald_brand));
        }
    }

    private void inflateOptions() {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final int brandColor = resolveThemeColor(R.attr.bald_brand);
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            final View row = inflater.inflate(R.layout.dialog_screen_option, optionsContainer, false);
            final ImageView check = row.findViewById(R.id.dialog_screen_option_check);
            final TextView text = row.findViewById(R.id.dialog_screen_option_text);
            text.setText(options[i]);
            if (i == selectedIndex) {
                check.setVisibility(View.VISIBLE);
                text.setTextColor(brandColor);
            }
            row.setOnClickListener(v -> {
                onOptionSelected.onOptionSelected(index);
                dismiss();
            });
            optionsContainer.addView(row);
        }
    }

    private int resolveThemeColor(int attr) {
        final TypedArray typedArray = context.obtainStyledAttributes(new int[]{attr});
        try {
            return typedArray.getColor(0, 0);
        } finally {
            typedArray.recycle();
        }
    }

    public interface OnOptionSelectedListener {
        void onOptionSelected(int index);
    }
}
