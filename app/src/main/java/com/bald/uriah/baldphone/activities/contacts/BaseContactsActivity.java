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

package com.bald.uriah.baldphone.activities.contacts;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.BaldActivity;
import com.bald.uriah.baldphone.adapters.ContactRecyclerViewAdapter;
import com.bald.uriah.baldphone.utils.SoftInputAssist;
import com.bald.uriah.baldphone.utils.Toggeler;
import com.bald.uriah.baldphone.views.BaldTitleBar;

/**
 * Base activity for {@link ShareActivity} and {@link ContactsActivity}.
 * both has {@link R.layout#contacts_search} in their layout.
 */
abstract class BaseContactsActivity extends BaldActivity {
    public static final String INTENT_EXTRA_CONTACT_ADAPTER_MODE = "INTENT_EXTRA_CONTACT_ADAPTER_MODE";
    private static final String FAVORITE_STATE = "FAVORITE_STATE";
    //<views>
    protected ImageView bt_favorite;
    //</views>
    protected RecyclerView recyclerView;
    protected BaldTitleBar baldTitleBar;
    protected ContentResolver contentResolver;
    protected ContactRecyclerViewAdapter contactRecyclerViewAdapter;
    protected int mode = ContactRecyclerViewAdapter.MODE_DEFAULT;

    private String filter = "";
    private boolean favorite = false;
    private SoftInputAssist softInputAssist;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkPermissions(this, requiredPermissions()))
            return;
        setContentView(layout());

        softInputAssist = new SoftInputAssist(this);
        contentResolver = getContentResolver();

        final Intent callingIntent = getIntent();
        if (callingIntent != null) {
            mode = callingIntent.getIntExtra(INTENT_EXTRA_CONTACT_ADAPTER_MODE, mode);
        }

        attachXml();
        viewsInit();

        applyFilter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (softInputAssist != null)
            softInputAssist.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (softInputAssist != null)
            softInputAssist.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (softInputAssist != null)
            softInputAssist.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(FAVORITE_STATE, favorite);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        favorite = savedInstanceState.getBoolean(FAVORITE_STATE);
    }

    @LayoutRes
    protected abstract int layout();

    protected void attachXml() {
        baldTitleBar = findViewById(R.id.bald_title_bar);
        recyclerView = findViewById(R.id.contacts_recycler_view);
        bt_favorite = findViewById(R.id.bt_favorite);
        final DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getDrawable(R.drawable.ll_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);

    }

    protected void viewsInit() {
        if (bt_favorite != null)
            Toggeler.newBackgroundToggeler(bt_favorite, bt_favorite, new int[]{
                    R.drawable.btn_selected, R.drawable.style_for_buttons
            }, new android.view.View.OnClickListener[]{
                    v -> {
                        favorite = true;
                        if (baldTitleBar != null)
                            baldTitleBar.setGold(true);
                        applyFilter();
                    },
                    v -> {
                        favorite = false;
                        if (baldTitleBar != null)
                            baldTitleBar.setGold(false);
                        applyFilter();
                    }
            });
    }

    protected void setFilter(String filter) {
        this.filter = filter == null ? "" : filter;
        applyFilter();
    }

    protected abstract Cursor getCursorForFilter(String filter, boolean favorite);

    public void applyFilter() {
        if (contactRecyclerViewAdapter != null) {
            contactRecyclerViewAdapter.changeCursor(getCursorForFilter(filter, favorite));
        } else {
            contactRecyclerViewAdapter =
                    new ContactRecyclerViewAdapter(
                            BaseContactsActivity.this,
                            getCursorForFilter(filter, favorite),
                            recyclerView,
                            mode);
            recyclerView.setAdapter(contactRecyclerViewAdapter);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SingleContactActivity.REQUEST_CHECK_CHANGE && resultCode == RESULT_OK) {
            applyFilter();
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_WRITE_CALL_LOG | PERMISSION_WRITE_CONTACTS | PERMISSION_CALL_PHONE | PERMISSION_READ_CONTACTS;
    }
}
