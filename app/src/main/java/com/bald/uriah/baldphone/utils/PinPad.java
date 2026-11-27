package com.bald.uriah.baldphone.utils;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bald.uriah.baldphone.R;

/**
 * Binds a pin_pad layout: digits accumulate into the dot readout and auto-submit
 * at pinLength. Replaces the system IME — no keyboard anywhere a PIN is asked.
 */
public final class PinPad {

    public interface OnSubmit {
        /** true = PIN accepted (caller owns what happens next); false = rejected, pad resets. */
        boolean onSubmit(@NonNull String pin);
    }

    public PinPad(@NonNull View root, int pinLength, @NonNull OnSubmit onSubmit) {
        final TextView dots = root.findViewById(R.id.tv_pin_dots);
        final StringBuilder digits = new StringBuilder(pinLength);
        final Runnable redraw = () -> {
            final int n = digits.length();
            final StringBuilder s = new StringBuilder(pinLength);
            for (int i = 0; i < pinLength; i++)
                s.append(i < n ? '●' : '○');
            dots.setText(s);
            dots.setContentDescription(dots.getResources().getString(R.string.pin_digits_entered, n, pinLength));
        };
        redraw.run();
        final Runnable submit = () -> {
            if (!onSubmit.onSubmit(digits.toString())) {
                digits.setLength(0);
                redraw.run();
            }
        };
        final View.OnClickListener digit = v -> {
            digits.append(((TextView) v).getText().charAt(0));
            redraw.run();
            if (digits.length() == pinLength)
                submit.run();
        };
        for (final int id : new int[]{R.id.b_1, R.id.b_2, R.id.b_3, R.id.b_4, R.id.b_5,
                R.id.b_6, R.id.b_7, R.id.b_8, R.id.b_9, R.id.b_0})
            root.findViewById(id).setOnClickListener(digit);
        root.findViewById(R.id.b_backspace).setOnClickListener(v -> {
            if (digits.length() == 0)
                return;
            digits.setLength(digits.length() - 1);
            redraw.run();
        });
    }
}
