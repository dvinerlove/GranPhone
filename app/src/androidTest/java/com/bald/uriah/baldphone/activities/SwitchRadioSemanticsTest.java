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

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.views.BaldMultipleSelection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SwitchRadioSemanticsTest extends BaseActivityTest {
    @Rule
    public ActivityTestRule<SettingsActivity> activityRule = new ActivityTestRule<>(SettingsActivity.class, true, false);

    @Test
    public void settingsRowExposesSwitchSemantics() {
        final SettingsActivity activity = activityRule.launchActivity(new Intent());
        final boolean expected = BPrefs.get(activity).getBoolean(
                BPrefs.VIBRATION_FEEDBACK_KEY, BPrefs.VIBRATION_FEEDBACK_DEFAULT_VALUE);
        getInstrumentation().waitForIdleSync();
        final View row = findByContentDescription(activity.getWindow().getDecorView(), activity.getString(R.string.vibrate));
        assertNotNull(row);
        final AccessibilityNodeInfo node = row.createAccessibilityNodeInfo();
        assertEquals(android.widget.Switch.class.getName(), node.getClassName().toString());
        assertTrue(node.isCheckable());
        assertEquals(expected, node.isChecked());
    }

    @Test
    public void multipleSelectionExposesRadioSemantics() {
        final SettingsActivity activity = activityRule.launchActivity(new Intent());
        getInstrumentation().runOnMainSync(() -> {
            final ViewGroup root = activity.findViewById(android.R.id.content);
            final BaldMultipleSelection selection = new BaldMultipleSelection(activity);
            selection.addSelection("One", "Two");
            root.addView(selection);
            try {
                final AccessibilityNodeInfo group = selection.createAccessibilityNodeInfo();
                assertEquals(android.widget.RadioGroup.class.getName(), group.getClassName().toString());

                final View first = selection.getChildAt(0);
                final View second = selection.getChildAt(1);
                assertRadio(first, true);
                assertRadio(second, false);

                selection.setSelection(1);
                assertRadio(first, false);
                assertRadio(second, true);
            } finally {
                root.removeView(selection);
            }
        });
    }

    private static View findByContentDescription(View root, CharSequence contentDescription) {
        if (contentDescription.equals(root.getContentDescription()))
            return root;
        if (root instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                final View match = findByContentDescription(group.getChildAt(i), contentDescription);
                if (match != null)
                    return match;
            }
        }
        return null;
    }

    private static void assertRadio(View view, boolean checked) {
        final AccessibilityNodeInfo node = view.createAccessibilityNodeInfo();
        assertEquals(android.widget.RadioButton.class.getName(), node.getClassName().toString());
        assertTrue(node.isCheckable());
        assertEquals(checked, node.isChecked());
    }
}
