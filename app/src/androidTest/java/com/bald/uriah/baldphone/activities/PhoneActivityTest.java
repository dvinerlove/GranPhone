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

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.bald.uriah.baldphone.R;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class PhoneActivityTest extends BaseActivityTest {
    @Rule
    public ActivityTestRule<HomeScreenActivity> mActivityTestRule = new ActivityTestRule<>(HomeScreenActivity.class, true, false);

    @Test
    public void phoneActivityTest() {
        mActivityTestRule.launchActivity(new Intent());
        // home -> dialer, type 054
        onView(withId(R.id.bt_dialer)).perform(scrollTo(), longClick());
        sleep();
        onView(withId(R.id.b_0)).perform(longClick());
        sleep();
        onView(withId(R.id.b_5)).perform(longClick());
        sleep();
        onView(withId(R.id.b_4)).perform(longClick());
        sleep();
        pressBack();
        sleep();
        // home -> contacts -> add contact
        onView(withId(R.id.bt_contacts)).perform(scrollTo(), longClick());
        sleep();
        onView(withId(R.id.bt_add_contact)).perform(longClick());
        sleep();
        onView(withId(R.id.et_name)).perform(replaceText("v"), closeSoftKeyboard());
        onView(withId(R.id.save)).perform(longClick());
        // save() rebuilds the stack: Home -> Contacts -> SingleContact
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // single contact -> edit mail
        onView(withId(R.id.bt_edit)).perform(scrollTo(), longClick());
        sleep();
        onView(withId(R.id.et_mail)).perform(scrollTo(), replaceText("hh"), closeSoftKeyboard());
        onView(withId(R.id.save)).perform(longClick());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // delete contact, confirm in the DialogScreen dialog
        onView(withId(R.id.bt_delete)).perform(scrollTo(), longClick());
        sleep();
        // a real longClick can't finish: onLongClick deletes the contact and dismisses
        // the dialog, so the UP event has no window left to land in
        onView(withId(R.id.dialog_screen_button_primary)).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "performLongClick";
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.performLongClick();
            }
        });
        sleep();
        pressBack();
    }
}
