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

package com.bald.uriah.baldphone.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.AppUtils;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldPrefsUtils;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.Const;
import com.bald.uriah.baldphone.utils.PinPad;
import com.bald.uriah.baldphone.utils.PredictiveBackHelper;
import com.bald.uriah.baldphone.views.BaldFrameLayoutButton;
import com.bald.uriah.baldphone.views.BaldSwitch;
import com.bald.uriah.baldphone.views.BaldTitleBar;
import com.bald.uriah.baldphone.views.ModularRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * One settings screen = one activity in the task stack. Drill-in rows
 * ({@link Category}) launch the child activity; back is the system back —
 * on API 34+ the real cross-activity predictive-back animation plays for free,
 * on older APIs the normal activity transition does. No in-app page stack.
 *
 * Subclasses provide {@link #pageTitleRes()} and fill {@link #items} in
 * {@link #populateItems()}; the shared list, PIN overlay and back semantics
 * live here.
 */
public abstract class SettingsPageActivity extends BaldActivity {
    private static final String TAG = SettingsPageActivity.class.getSimpleName();

    protected final List<SettingsItem> items = new ArrayList<>();
    protected SharedPreferences sharedPreferences;
    protected SharedPreferences.Editor editor;
    private RecyclerView recyclerView;
    private BaldPrefsUtils baldPrefsUtils;
    private BaldTitleBar baldTitleBar;
    private View contentArea;
    private View pinPage;
    /** Views exist — onCreate ran past the permission gate; subclasses may show pins. */
    protected boolean viewsReady;
    /** Non-null while the PIN page covers the list — a page, not a modal (lock-screen idiom). */
    protected PinRequest activePin;
    /** Set while a non-gate pin is up — back cancels the pin instead of finishing. */
    private Runnable pinBackUnregister;

    @StringRes
    protected abstract int pageTitleRes();

    protected abstract void populateItems();

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkPermissions(this, requiredPermissions()))
            return;
        setContentView(R.layout.activity_settings);
        sharedPreferences = getSharedPreferences(Const.BALD_PREFS, MODE_PRIVATE);
        baldPrefsUtils = BaldPrefsUtils.newInstance(this);
        editor = sharedPreferences.edit();

        baldTitleBar = findViewById(R.id.bald_title_bar);
        baldTitleBar.setBackListener((v) -> onBackPressed());
        baldTitleBar.setTitle(pageTitleRes());
        recyclerView = findViewById(R.id.child);
        contentArea = findViewById(R.id.content_area);
        pinPage = findViewById(R.id.pin_page);

        populateItems();
        recyclerView.setAdapter(new SettingsRecyclerViewAdapter());
        if (!AppUtils.animationsDisabled(this))
            recyclerView.scheduleLayoutAnimation();
        viewsReady = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recyclerView == null)
            return;
        if (baldPrefsUtils.hasChanged(this))
            recreate();
        else // refresh live subtitles (system brightness, font scale…)
            recyclerView.getAdapter().notifyDataSetChanged();
    }

    /**
     * Back: a non-gate pin cancels in place; everything else is a plain activity
     * finish — the system transition (and on API 34+, the real cross-activity
     * predictive-back animation) plays itself.
     */
    @Override
    public void onBackPressed() {
        if (activePin != null && !activePin.exitsOnCancel)
            cancelPinPage();
        else
            super.onBackPressed();
    }

    /** An in-flight PIN request — what the pin page shows and where a back-out lands. */
    protected static class PinRequest {
        final int titleRes, messageRes, pinLength;
        final PinPad.OnSubmit onSubmit;
        /** Runs when the user backs out without submitting; null = just return to the list. */
        @Nullable
        final Runnable onCancel;
        /** True when back-out should leave the activity (entry gate over a hidden list). */
        final boolean exitsOnCancel;

        protected PinRequest(int titleRes, int messageRes, int pinLength,
                             PinPad.OnSubmit onSubmit, @Nullable Runnable onCancel, boolean exitsOnCancel) {
            this.titleRes = titleRes;
            this.messageRes = messageRes;
            this.pinLength = pinLength;
            this.onSubmit = onSubmit;
            this.onCancel = onCancel;
            this.exitsOnCancel = exitsOnCancel;
        }
    }

    protected void showPinPage(int titleRes, int messageRes, int pinLength,
                               PinPad.OnSubmit onSubmit, @Nullable Runnable onCancel) {
        showPinPage(new PinRequest(titleRes, messageRes, pinLength, onSubmit, onCancel, false));
    }

    protected void showPinPage(PinRequest req) {
        activePin = req;
        ((TextView) pinPage.findViewById(R.id.tv_pin_title)).setText(req.titleRes);
        ((TextView) pinPage.findViewById(R.id.tv_pin_message)).setText(req.messageRes);
        new PinPad(pinPage.findViewById(R.id.pin_pad), req.pinLength, pin -> {
            if (req.onSubmit.onSubmit(pin) && activePin == req) {
                hidePinPage();
                return true;
            }
            return false;
        });
        baldTitleBar.setTitle(req.titleRes);
        contentArea.setVisibility(View.GONE);
        pinPage.setVisibility(View.VISIBLE);
        updatePinBack();
    }

    protected void hidePinPage() {
        activePin = null;
        pinPage.setVisibility(View.GONE);
        contentArea.setVisibility(View.VISIBLE);
        baldTitleBar.setTitle(pageTitleRes());
        updatePinBack();
    }

    protected void cancelPinPage() {
        final Runnable onCancel = activePin.onCancel;
        hidePinPage();
        if (onCancel != null)
            onCancel.run();
    }

    /**
     * While a non-gate pin covers the list, the system back button must cancel
     * the pin rather than finish the activity — intercept it. Gate pins and
     * pin-free pages register nothing, so the system owns the back (and on API
     * 34+ plays its own real predictive animation).
     */
    private void updatePinBack() {
        final boolean intercept = activePin != null && !activePin.exitsOnCancel;
        if (intercept && pinBackUnregister == null)
            pinBackUnregister = PredictiveBackHelper.register(this, this::onBackPressed);
        else if (!intercept && pinBackUnregister != null) {
            pinBackUnregister.run();
            pinBackUnregister = null;
        }
    }

    public class SettingsRecyclerViewAdapter extends ModularRecyclerView.ModularAdapter<SettingsRecyclerViewAdapter.ViewHolder> {
        private static final int VIEW_TYPE_ITEM = 0;
        private static final int VIEW_TYPE_HEADER = 1;
        final LayoutInflater layoutInflater;

        SettingsRecyclerViewAdapter() {
            layoutInflater = (LayoutInflater) SettingsPageActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof Header ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(layoutInflater.inflate(
                    viewType == VIEW_TYPE_HEADER ? R.layout.settings_header : R.layout.settings_item, parent, false));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            holder.update(items.get(position));
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            TextView subtitleView;
            ImageView setting_icon;
            ViewGroup trailing;

            public ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.tv_setting_name);
                subtitleView = itemView.findViewById(R.id.tv_setting_subtitle);
                setting_icon = itemView.findViewById(R.id.setting_icon);
                trailing = itemView.findViewById(R.id.setting_trailing);
            }

            public void update(SettingsItem settingsItem) {
                if (settingsItem instanceof Header) {
                    ((TextView) this.itemView).setText(settingsItem.textResId);
                    return;
                }
                this.textView.setText(settingsItem.textResId);
                this.setting_icon.setImageResource(settingsItem.drawableResId);
                final CharSequence subtitle = settingsItem.getSubtitle();
                this.subtitleView.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
                if (subtitle != null)
                    this.subtitleView.setText(subtitle);
                this.trailing.removeAllViews();
                settingsItem.bindTrailing(this.trailing);
                this.itemView.setOnClickListener(settingsItem);
                bindAccessibility(settingsItem instanceof SwitchSettingsItem ? (SwitchSettingsItem) settingsItem : null);
            }

            private void bindAccessibility(@Nullable SwitchSettingsItem switchItem) {
                final boolean switchRow = switchItem != null;
                final BaldFrameLayoutButton row = (BaldFrameLayoutButton) itemView;
                textView.setImportantForAccessibility(switchRow ? View.IMPORTANT_FOR_ACCESSIBILITY_NO : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                subtitleView.setImportantForAccessibility(switchRow ? View.IMPORTANT_FOR_ACCESSIBILITY_NO : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                trailing.setImportantForAccessibility(switchRow ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                if (!switchRow) {
                    row.setAccessibilityClassName(null);
                    itemView.setContentDescription(null);
                    itemView.setAccessibilityDelegate(null);
                    return;
                }
                row.setAccessibilityClassName(android.widget.Switch.class.getName());
                itemView.setContentDescription(switchItem.accessibilityLabel());
                itemView.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        info.setCheckable(true);
                        info.setChecked(switchItem.baldSwitch == null ? switchItem.readChecked() : switchItem.baldSwitch.isChecked());
                        info.setContentDescription(switchItem.accessibilityLabel());
                    }
                });
            }
        }
    }

    public abstract class SettingsItem implements View.OnClickListener {
        public final @StringRes
        int textResId;
        public final @DrawableRes
        int drawableResId;

        protected SettingsItem(@StringRes int textResId, @DrawableRes int drawableResId) {
            this.textResId = textResId;
            this.drawableResId = drawableResId;
        }

        /** Current value shown under the title (e.g. "Large"); null = no subtitle. */
        @Nullable
        public CharSequence getSubtitle() {
            return null;
        }

        /** Add a trailing widget (e.g. BaldSwitch) into the row's end slot. Default: none. */
        public void bindTrailing(@NonNull ViewGroup container) {
        }

    }

    /** Row with an inline {@link BaldSwitch} in the trailing slot; row tap toggles it. */
    public class SwitchSettingsItem extends SettingsItem {
        private final String key;
        private final boolean defaultValue;
        private BaldSwitch baldSwitch;

        SwitchSettingsItem(@StringRes int textResId, @DrawableRes int drawableResId, String key, boolean defaultValue) {
            super(textResId, drawableResId);
            this.key = key;
            this.defaultValue = defaultValue;
        }

        /** Hook for side effects beyond the pref write (e.g. updating the vibrator field). */
        protected void onPrefChanged(boolean checked) {
        }

        protected boolean readChecked() {
            return sharedPreferences.getBoolean(key, defaultValue);
        }

        protected void apply(boolean checked) {
            editor.putBoolean(key, checked).apply();
            onPrefChanged(checked);
        }

        /** Pull readChecked() back into the switch — for rows whose state settles in a dialog. */
        protected void resync() {
            if (baldSwitch != null)
                baldSwitch.setChecked(readChecked());
        }

        private CharSequence accessibilityLabel() {
            final CharSequence label = SettingsPageActivity.this.getText(textResId);
            final CharSequence subtitle = getSubtitle();
            return subtitle == null ? label : label + "\n" + subtitle;
        }

        @Override
        public void onClick(View v) {
            if (baldSwitch != null)
                baldSwitch.performClick();
        }

        @Override
        public void bindTrailing(@NonNull ViewGroup container) {
            final Context ctx = container.getContext();
            baldSwitch = new BaldSwitch(ctx);
            baldSwitch.setLayoutParams(new ViewGroup.LayoutParams(
                    ctx.getResources().getDimensionPixelSize(R.dimen.switch_width),
                    ctx.getResources().getDimensionPixelSize(R.dimen.switch_height)));
            baldSwitch.setChecked(readChecked());
            baldSwitch.setOnChangeListener(this::apply);
            baldSwitch.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            baldSwitch.setFocusable(false);
            container.addView(baldSwitch);
        }
    }

    public class RunnableSettingsItem extends SettingsItem {
        private final View.OnClickListener onClickListener;

        RunnableSettingsItem(@StringRes int textResId, View.OnClickListener onClickListener, @DrawableRes int drawableResId) {
            super(textResId, drawableResId);
            this.onClickListener = onClickListener;
        }

        @Override
        public void onClick(View v) {
            try {
                onClickListener.onClick(v);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
                BaldToast.from(v.getContext()).setText(R.string.setting_does_not_exist).setType(BaldToast.TYPE_ERROR).show();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
                BaldToast.from(v.getContext()).setText(R.string.an_error_has_occurred).setType(BaldToast.TYPE_ERROR).show();
                BaldToast.from(v.getContext()).setText(e.getMessage()).setType(BaldToast.TYPE_ERROR).show();
            }
        }
    }

    /** Non-clickable section label («Общие» / «Дополнительные»). */
    public class Header extends SettingsItem {
        Header(@StringRes int textResId) {
            super(textResId, 0);
        }

        @Override
        public void onClick(View v) {
            // decorative
        }
    }

    /** Drill-in row: opens the child settings activity; a chevron marks it. */
    public class Category extends SettingsItem {
        private final Class<? extends Activity> target;

        public Category(@StringRes int textResId, @DrawableRes int drawableResId,
                        Class<? extends Activity> target) {
            super(textResId, drawableResId);
            this.target = target;
        }

        @Override
        public void onClick(View v) {
            startActivity(new Intent(SettingsPageActivity.this, target));
        }

        @Override
        public void bindTrailing(@NonNull ViewGroup container) {
            // chevron marks drill-in rows (same idiom as system Settings)
            final Context ctx = container.getContext();
            final ImageView chevron = new ImageView(ctx);
            final int size = ctx.getResources().getDimensionPixelSize(R.dimen.icon_sm);
            chevron.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            chevron.setImageResource(R.drawable.chevron_end_on_button);
            chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            container.addView(chevron);
        }
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_WRITE_SETTINGS;
    }
}
