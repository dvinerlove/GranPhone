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
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.BaldActivity;
import com.bald.uriah.baldphone.adapters.ContactRecyclerViewAdapter;
import com.bald.uriah.baldphone.utils.BaldExecutors;
import com.bald.uriah.baldphone.utils.GenerationGuard;
import com.bald.uriah.baldphone.utils.SoftInputAssist;

import java.util.Collections;
import java.util.List;
import com.bald.uriah.baldphone.views.BaldTitleBar;
import com.bald.uriah.baldphone.views.ScrollingHelper;

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
    private final Handler filterHandler = new Handler(Looper.getMainLooper());
    private final GenerationGuard contactsGuard = new GenerationGuard();
    private Runnable pendingFilter;
    private SoftInputAssist softInputAssist;
    private ScrollingHelper scrollingHelper;

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

        scrollingHelper = findViewById(R.id.include);
        if (scrollingHelper == null)
            scrollingHelper = findViewById(R.id.whatsapp_container);

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
        contactsGuard.invalidate();
        if (pendingFilter != null)
            filterHandler.removeCallbacks(pendingFilter);
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
        setFavorite(savedInstanceState.getBoolean(FAVORITE_STATE));
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
        if (bt_favorite != null) {
            bt_favorite.setSelected(favorite);
            bt_favorite.setOnClickListener(v -> setFavorite(!favorite));
        }
    }

    private void setFavorite(boolean favorite) {
        this.favorite = favorite;
        if (bt_favorite != null)
            bt_favorite.setSelected(favorite);
        if (baldTitleBar != null)
            baldTitleBar.setGold(favorite);
        if (contentResolver != null)
            applyFilter();
    }

    protected void setFilter(String filter) {
        this.filter = filter == null ? "" : filter;
        if (pendingFilter != null)
            filterHandler.removeCallbacks(pendingFilter);
        pendingFilter = () -> applyFilter();
        filterHandler.postDelayed(pendingFilter, 250);
    }

    protected abstract Cursor getCursorForFilter(String filter, boolean favorite);

    public void applyFilter() {
        if (scrollingHelper != null) {
            scrollingHelper.setEmptyText(getString(filter.isEmpty()
                    ? R.string.no_contacts : R.string.no_contacts_found));
        }
        if (contactRecyclerViewAdapter == null) {
            contactRecyclerViewAdapter =
                    new ContactRecyclerViewAdapter(
                            BaseContactsActivity.this,
                            Collections.<ContactRecyclerViewAdapter.ContactRow>emptyList(),
                            recyclerView,
                            mode);
            recyclerView.setAdapter(contactRecyclerViewAdapter);
        }
        final String filterSnapshot = filter;
        final boolean favoriteSnapshot = favorite;
        final int generation = contactsGuard.next();
        BaldExecutors.io().execute(() -> {
            List<ContactRecyclerViewAdapter.ContactRow> rows;
            try {
                rows = ContactRecyclerViewAdapter.loadRows(
                        getCursorForFilter(filterSnapshot, favoriteSnapshot), contentResolver);
            } catch (Exception e) {
                rows = Collections.emptyList();
            }
            final List<ContactRecyclerViewAdapter.ContactRow> result = rows;
            BaldExecutors.postToMain(() -> {
                if (!contactsGuard.isCurrent(generation) || isFinishing() || isDestroyed())
                    return;
                contactRecyclerViewAdapter.submitList(result);
            });
        });
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
        return PERMISSION_READ_CONTACTS;
    }
}
