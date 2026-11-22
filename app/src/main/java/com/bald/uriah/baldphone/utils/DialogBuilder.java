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

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.bald.uriah.baldphone.activities.BaldActivity;

/**
 * Builder for {@link DialogFlags}
 */
public class DialogBuilder {
    public Context context;

    @NonNull
    CharSequence title = "";
    CharSequence subText;
    CharSequence[] options;
    int inputType;
    DialogFlags.DialogBoxListener positiveButtonListener = DialogFlags.DialogBoxListener.EMPTY;
    DialogFlags.DialogBoxListener negativeButtonListener = DialogFlags.DialogBoxListener.EMPTY;
    @NonNull
    DialogFlags.StartingIndexChooser startingIndexChooser = () -> 0;
    @Nullable
    BaldActivity baldActivityToAutoDismiss;
    @Nullable
    View extraView;
    @Nullable
    CharSequence negativeCustomText;
    @Nullable
    CharSequence positiveCustomText;
    int flags;

    private DialogBuilder() {
    }

    public static DialogBuilder from(@Nullable Context context) {
        DialogBuilder bdb = new DialogBuilder();
        bdb.context = context;
        if (context instanceof BaldActivity) {
            bdb.baldActivityToAutoDismiss = (BaldActivity) context;
        }
        return bdb;
    }

    public DialogBuilder setTitle(CharSequence title) {
        this.title = title;
        return this;
    }

    public DialogBuilder setTitle(@StringRes int titleId) {
        return setTitle(context.getText(titleId));
    }

    public DialogBuilder setSubText(CharSequence subText) {
        this.subText = subText;
        return this;
    }

    public DialogBuilder setSubText(@StringRes int subTextId) {
        return setSubText(context.getText(subTextId));
    }

    public DialogBuilder setOptions(CharSequence... options) {
        flags |= DialogFlags.FLAG_OPTIONS;
        this.options = options;
        return this;
    }

    public DialogBuilder setOptions(@StringRes int... options) {
        final CharSequence[] charSequences = new CharSequence[options.length];
        for (int i = 0; i < options.length; i++)
            charSequences[i] = context.getText(options[i]);

        return setOptions(charSequences);
    }

    public DialogBuilder setPositiveButtonListener(@Nullable DialogFlags.DialogBoxListener dialogBoxListener) {
        this.positiveButtonListener = dialogBoxListener;
        return addFlag(DialogFlags.FLAG_POSITIVE);
    }

    public DialogBuilder setNegativeButtonListener(@Nullable DialogFlags.DialogBoxListener dialogBoxListener) {
        this.negativeButtonListener = dialogBoxListener;
        return addFlag(DialogFlags.FLAG_NEGATIVE);
    }

    public DialogBuilder setInputType(int inputType) {
        this.inputType = inputType;
        return addFlag(DialogFlags.FLAG_INPUT);
    }

    public DialogBuilder setOptionsStartingIndex(DialogFlags.StartingIndexChooser startingIndexChooser) {
        this.startingIndexChooser = startingIndexChooser;
        return this;
    }

    public DialogBuilder setExtraView(@Nullable View extraView) {
        this.extraView = extraView;
        return this;
    }

    public DialogBuilder setBaldActivityToAutoDismiss(BaldActivity baldActivityToAutoDismiss) {
        this.baldActivityToAutoDismiss = baldActivityToAutoDismiss;
        return this;
    }

    public DialogBuilder setNegativeCustomText(@Nullable CharSequence negativeCustomText) {
        this.negativeCustomText = negativeCustomText;
        return addFlag(DialogFlags.FLAG_CUSTOM_NEGATIVE);
    }

    public DialogBuilder setPositiveCustomText(@Nullable CharSequence positiveCustomText) {
        this.positiveCustomText = positiveCustomText;
        return addFlag(DialogFlags.FLAG_CUSTOM_POSITIVE);
    }

    public DialogBuilder addFlag(int flags) {
        this.flags |= flags;
        return this;
    }

    public DialogFlags show() {
        return DialogFlags.newInstance(this);
    }
}