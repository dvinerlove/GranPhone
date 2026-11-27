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

import android.content.Context;
import android.content.Intent;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.databases.home_screen_pins.HomeScreenPinHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SOSActivityTest extends BaseActivityTest {
    @Rule
    public ActivityTestRule<HomeScreenActivity> mActivityTestRule = new ActivityTestRule<>(HomeScreenActivity.class, true, false);

    @Test
    public void sOSActivityTest() {
        // bt_add_contact hides once 2 contacts are pinned to SOS; clear pins so the path is deterministic
        getInstrumentation().getTargetContext()
                .getSharedPreferences(HomeScreenPinHelper.PinnedContactPreferences.KEY, Context.MODE_PRIVATE)
                .edit()
                .remove(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY)
                .commit();
        mActivityTestRule.launchActivity(new Intent());
        onView(withId(R.id.bt_home_sos)).perform(longClick());
        sleep();
        // quick tap on the hold-to-call button only asks for confirmation; a long hold would really dial 112
        onView(withId(R.id.bt_sos)).perform(click());
        sleep();
        onView(withId(R.id.dialog_screen_button_primary)).check(matches(isDisplayed()));
        pressBack();
        sleep();
        onView(withId(R.id.bt_add_contact)).perform(scrollTo(), longClick());
        sleep();
        pressBack();
        pressBack();
        sleep();
    }
}
