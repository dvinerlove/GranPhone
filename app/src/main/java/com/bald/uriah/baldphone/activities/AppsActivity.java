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

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.adapters.AppsRecyclerViewAdapter;
import com.bald.uriah.baldphone.databases.apps.App;
import com.bald.uriah.baldphone.databases.apps.AppsDatabase;
import com.bald.uriah.baldphone.databases.apps.AppsDatabaseHelper;
import com.bald.uriah.baldphone.databases.home_screen_pins.HomeScreenPinHelper;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.DropDownRecyclerViewAdapter;
import com.bald.uriah.baldphone.utils.AppUtils;

import java.util.List;
import java.util.Objects;

import static com.bald.uriah.baldphone.adapters.AppsRecyclerViewAdapter.TYPE_HEADER;
import static com.bald.uriah.baldphone.databases.apps.AppsDatabaseHelper.baldComponentNameBeginning;

public class AppsActivity extends com.bald.uriah.baldphone.activities.BaldActivity {
    private static final String TAG = AppsActivity.class.getSimpleName();
    public static final int REQUEST_SELECT_CUSTOM_APP = 88;
    public static final String CHOOSE_MODE = "CHOOSE_MODE";
    public static final int UNINSTALL_REQUEST_CODE = 52;
    private static final String SELECTED_APP_INDEX = "SELECTED_APP_INDEX";

    private AppsDatabase appsDatabase;

    private RecyclerView recyclerView;

    private AppsRecyclerViewAdapter appsRecyclerViewAdapter;
    private String chooseKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);

        AppsDatabaseHelper.updateDB(this);

        appsDatabase = AppsDatabase.getInstance(AppsActivity.this);
        final List<App> appList = appsDatabase.appsDatabaseDao().getAllOrderedByABC();
        recyclerView = findViewById(R.id.rc_apps);
        chooseKey = getIntent().getStringExtra(CHOOSE_MODE);
        if (chooseKey != null)
            ((com.bald.uriah.baldphone.views.BaldTitleBar) findViewById(R.id.bald_title_bar)).setTitle(R.string.choose_another_app);
        appsRecyclerViewAdapter = new AppsRecyclerViewAdapter(appList, this, chooseKey != null ? this::appChosen : this::showDropDown, recyclerView);
        recyclerView.setAdapter(appsRecyclerViewAdapter);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_APP_INDEX, ((AppsRecyclerViewAdapter) recyclerView.getAdapter()).index);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final int index = savedInstanceState.getInt(SELECTED_APP_INDEX);
        final AppsRecyclerViewAdapter adapter = ((AppsRecyclerViewAdapter) recyclerView.getAdapter());
        if (index < adapter.dataList.size() && index > 0 && adapter.dataList.get(index).type() != TYPE_HEADER) {
            adapter.index = index;
            recyclerView.getLayoutManager().scrollToPosition(index);
            recyclerView.post(() -> showDropDown(index));
        }
    }

    private void uninstallApp(App app) {
        final String app_pkg_name = ComponentName.unflattenFromString(app.getFlattenComponentName()).getPackageName();
        startActivityForResult(new Intent(Intent.ACTION_UNINSTALL_PACKAGE)
                .setData(Uri.parse("package:" + app_pkg_name))
                .putExtra(Intent.EXTRA_RETURN_RESULT, true), UNINSTALL_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UNINSTALL_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                recreate();
            }
        }
    }

    private void showDropDown(final int index) {
        appsRecyclerViewAdapter.index = index;
        final App app = (App) appsRecyclerViewAdapter.dataList.get(index);
        final View view = Objects.requireNonNull(recyclerView.getLayoutManager()).findViewByPosition(index);
        if (view == null)
            return;
        AppUtils.showDropDownPopup(this, recyclerView.getWidth(), new DropDownRecyclerViewAdapter.DropDownListener() {
            @Override
            public void onUpdate(DropDownRecyclerViewAdapter.ViewHolder viewHolder, int position, PopupWindow popupWindow) {
                switch (position) {
                    case 0:
                        if (AppUtils.isValidContextForGlide(viewHolder.pic.getContext()))
                            AppsDatabaseHelper.loadPic(app, viewHolder.pic);
                        viewHolder.text.setText(R.string.open);
                        viewHolder.itemView.setOnClickListener(v1 -> {
                            final ComponentName componentName = ComponentName.unflattenFromString(app.getFlattenComponentName());
                            AppUtils.startComponentName(AppsActivity.this, componentName);
                            popupWindow.dismiss();
                        });
                        break;
                    case 1:
                        final boolean pinned = HomeScreenPinHelper.isAppPinned(AppsActivity.this, app.getFlattenComponentName());
                        viewHolder.pic.setImageResource(pinned ? R.drawable.remove_on_button : R.drawable.pin_on_button);
                        viewHolder.text.setText(pinned ? R.string.unpin_from_home : R.string.pin_to_home);
                        viewHolder.itemView.setOnClickListener(v1 -> {
                            if (pinned)
                                HomeScreenPinHelper.unpinApp(AppsActivity.this, app.getFlattenComponentName());
                            else if (!HomeScreenPinHelper.pinApp(AppsActivity.this, app.getFlattenComponentName()))
                                DialogScreen.info(AppsActivity.this, R.string.home_screen_is_full, R.string.home_screen_is_full_subtext);
                            popupWindow.dismiss();
                            showDropDown(index);
                        });
                        break;
                    case 2:
                        viewHolder.pic.setImageResource(R.drawable.delete_on_button);
                        viewHolder.text.setText(R.string.uninstall);
                        viewHolder.itemView.setOnClickListener(v1 -> {
                            DialogScreen.confirm(AppsActivity.this,
                                    String.format("%s %s", getText(R.string.uninstall), app.getLabel()),
                                    String.format(getString(R.string.uninstall_subtext), app.getLabel(), app.getLabel()),
                                    R.string.uninstall,
                                    () -> uninstallApp(app));
                            popupWindow.dismiss();
                        });
                        break;
                }
            }

            @Override
            public int size() {
                return app.getFlattenComponentName().startsWith(baldComponentNameBeginning) ? 2 : 3;
            }

            @Override
            public void onDismiss() {
                if (appsRecyclerViewAdapter.lastView != null)
                    appsRecyclerViewAdapter.lastView.setClicked(false);
                appsRecyclerViewAdapter.lastView = null;
                appsRecyclerViewAdapter.index = -1;

            }
        }, view);

        recyclerView.scrollToPosition(index);

    }

    private void appChosen(int index) {
        if (index != -1) {
            final App app = (App) appsRecyclerViewAdapter.dataList.get(index);
            setResult(RESULT_OK, new Intent().setComponent(ComponentName.unflattenFromString(app.getFlattenComponentName())).putExtra(CHOOSE_MODE, chooseKey));
            finish();
        }
    }

    public interface ChangeAppListener {
        void changeApp(int index);
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_NONE;
    }
}