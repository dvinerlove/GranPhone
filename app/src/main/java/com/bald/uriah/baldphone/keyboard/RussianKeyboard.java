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

package com.bald.uriah.baldphone.keyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.bald.uriah.baldphone.R;

@SuppressLint("ViewConstructor")
public class RussianKeyboard extends BaldKeyboard implements BaldKeyboard.Capitalised {
    public static final int LANGUAGE_ID = 3;
    public static final char[] ruKeyboardCodes = new char[]{
            'й', 'ц', 'у', 'к', 'е', 'н', 'г', 'ш', 'щ', 'з', 'х', 'ъ',
            'ф', 'ы', 'в', 'а', 'п', 'р', 'о', 'л', 'д', 'ж', 'э',
            SHIFT, 'я', 'ч', 'с', 'м', 'и', 'т', 'ь', 'б', 'ю', BACKSPACE,
            NUMBERS, LANGUAGE, SPEECH_TO_TEXT, ' ', HIDE, '.', ENTER,
    };
    public static final char[] ruKeyboardCodesCAPS = new char[]{
            'Й', 'Ц', 'У', 'К', 'Е', 'Н', 'Г', 'Ш', 'Щ', 'З', 'Х', 'Ъ',
            'Ф', 'Ы', 'В', 'А', 'П', 'Р', 'О', 'Л', 'Д', 'Ж', 'Э',
            SHIFT, 'Я', 'Ч', 'С', 'М', 'И', 'Т', 'Ь', 'Б', 'Ю', BACKSPACE,
            NUMBERS, LANGUAGE, SPEECH_TO_TEXT, ' ', HIDE, '.', ENTER,
    };

    private boolean caps;

    public RussianKeyboard(Context context, OnClickListener onClickListener, Runnable backspace, int imeOptions) {
        super(context, onClickListener, backspace, imeOptions);
    }

    public void setCaps() {
        caps = !caps;
        final char[] codes = codes();
        // children has one trailing null slot (sized by getChildCount); bound by codes
        for (int i = 0; i < codes.length; i++) {
            children[i].setTag(codes[i]);
            if (children[i] instanceof TextView)
                ((TextView) children[i]).setText(new char[]{codes[i]}, 0, 1);
        }
    }

    @Override
    protected int layout() {
        return R.layout.ru_keyboard_layout;
    }

    @Override
    int nextLanguage() {
        return EnglishKeyboard.LANGUAGE_ID;
    }

    @Override
    protected char[] codes() {
        return caps ? ruKeyboardCodesCAPS : ruKeyboardCodes;
    }
}
