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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
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
 * The single redesign dialog component, four modes:
 * SELECT - vertical option list, applies instantly on tap, "Back" text button at bottom. No OK/Cancel/X.
 * CONFIRM - message + one verb-labeled action + one "Cancel".
 * INFO - message + one "Got it" (optional extraView payload).
 * PIN - numeric field, lock-screen style: auto-submits at pinLength, no OK button;
 *        the Cancel text hides while the IME is open.
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
    private OnSubmitListener onSubmit;
    private boolean primaryIsTextButton;
    private View extraView;
    private int pinLength;

    private TextView tv_title, tv_message;
    private LinearLayout optionsContainer;
    private BaldButton bt_primary, bt_secondary;
    private EditText input;
    private FrameLayout extraSlot;

    private DialogScreen(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    /**
     * INFO mode: title + message + one "Got it" button.
     */
    public static DialogScreen info(@NonNull Context context, @StringRes int titleRes, @StringRes int messageRes) {
        return info(context, titleRes, messageRes, null, R.string.understood);
    }

    /**
     * INFO carrying a payload view (e.g. volume controls, memory status).
     */
    public static DialogScreen info(@NonNull Context context, @StringRes int titleRes, @StringRes int messageRes,
                                    @Nullable View extraView, @StringRes int primaryRes) {
        final DialogScreen dialog = new DialogScreen(context);
        dialog.title = context.getText(titleRes);
        dialog.message = context.getText(messageRes);
        dialog.extraView = extraView;
        dialog.primaryText = context.getText(primaryRes);
        return dialog.present();
    }

    /**
     * PIN mode: fixed-length digits, auto-submit on the last one — no OK button;
     * the Cancel text stays but hides while the IME is open.
     */
    public static DialogScreen pin(@NonNull Context context, @StringRes int titleRes, @StringRes int messageRes,
                                   int pinLength, @NonNull OnSubmitListener listener) {
        final DialogScreen dialog = new DialogScreen(context);
        dialog.title = context.getText(titleRes);
        dialog.message = context.getText(messageRes);
        dialog.pinLength = pinLength;
        dialog.onSubmit = listener;
        dialog.secondaryText = context.getText(R.string.cancel);
        return dialog.present();
    }

    /**
     * CONFIRM mode: title + message + one verb-labeled action + one "Cancel".
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
     * SELECT mode: title + vertical option list (instant apply on tap) + "Back" text button at bottom.
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

    /**
     * SELECT with a message line — for choices that need a sentence of explanation.
     */
    public static DialogScreen select(@NonNull Context context, @StringRes int titleRes, @StringRes int messageRes,
                                      int selectedIndex, @NonNull OnOptionSelectedListener listener,
                                      @NonNull CharSequence... options) {
        final DialogScreen dialog = new DialogScreen(context);
        dialog.title = context.getText(titleRes);
        dialog.message = context.getText(messageRes);
        dialog.options = options;
        dialog.selectedIndex = selectedIndex;
        dialog.onOptionSelected = listener;
        dialog.primaryText = context.getText(R.string.back);
        dialog.primaryIsTextButton = true;
        return dialog.present();
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
        input = findViewById(R.id.dialog_screen_input);
        extraSlot = findViewById(R.id.dialog_screen_extra);

        tv_title.setText(title);
        if (message == null)
            tv_message.setVisibility(View.GONE);
        else
            tv_message.setText(message);

        if (options != null)
            inflateOptions();

        if (extraView != null) {
            if (extraView.getParent() != null)
                ((ViewGroup) extraView.getParent()).removeView(extraView);
            extraSlot.addView(extraView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            extraSlot.setVisibility(View.VISIBLE);
        }

        if (pinLength > 0)
            setupPinInput();

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
                // SELECT's Back / INFO's Got it are dismissals, not actions — route through cancel()
                cancel();
            }
        });
        if (primaryIsTextButton) {
            bt_primary.setBackgroundResource(R.drawable.bald_ghost_button_background);
            bt_primary.setTextColor(resolveThemeColor(R.attr.bald_brand));
        }
    }

    private void setupPinInput() {
        bt_primary.setVisibility(View.GONE);
        input.setVisibility(View.VISIBLE);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(pinLength)});
        final Runnable submit = () -> {
            if (onSubmit.onSubmit(input.getText()))
                cancel();
            else
                input.setText("");
        };
        input.setOnEditorActionListener((v, actionId, event) -> {
            submit.run();
            return true;
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= pinLength)
                    submit.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        if (context instanceof Activity) {
            final View activityDecor = ((Activity) context).getWindow().getDecorView();
            final int imeThreshold = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.15);
            final ViewTreeObserver.OnGlobalLayoutListener imeListener = () -> {
                final Rect r = new Rect();
                activityDecor.getWindowVisibleDisplayFrame(r);
                bt_secondary.setVisibility(activityDecor.getRootView().getHeight() - r.bottom > imeThreshold
                        ? View.GONE : View.VISIBLE);
            };
            activityDecor.getViewTreeObserver().addOnGlobalLayoutListener(imeListener);
            setOnDismissListener(d ->
                    activityDecor.getViewTreeObserver().removeOnGlobalLayoutListener(imeListener));
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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
                row.setSelected(true); // TalkBack announces the selected state
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

    public interface OnSubmitListener {
        /** @return true when the input is accepted and the dialog should close; false clears the field */
        boolean onSubmit(@NonNull CharSequence text);
    }
}
