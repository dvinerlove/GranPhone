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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bald.uriah.baldphone.BuildConfig;
import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.DialogScreen;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.AppUtils;
import com.bald.uriah.baldphone.utils.CallManager;
import com.bald.uriah.baldphone.utils.PermissionContract;
import com.bald.uriah.baldphone.utils.PermissionRecovery;
import com.bald.uriah.baldphone.views.BaldPictureTextButton;
import com.bald.uriah.baldphone.views.BaldTitleBar;
import com.bald.uriah.baldphone.views.ModularRecyclerView;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_CALL_LOG;
import static android.Manifest.permission.WRITE_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES;

public class PermissionActivity extends BaldActivity {
    public static final String
            EXTRA_REQUIRED_PERMISSIONS = "EXTRA_REQUIRED_PERMISSIONS",
            EXTRA_INTENT = "EXTRA_INTENT";
    public static final int[] REQUEST_CODES = {789, 788, 787, 786, 785};
    private static final int REQUEST_CODE_APP_SETTINGS = 785;

    private final PermissionRecovery permissionRecovery = new PermissionRecovery();
    private List<PermissionItem> permissionItemList = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView progress;
    private int requiredPermissions;
    private int totalSteps;
    private Intent ancestorCallingIntent;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        final Intent callingIntent = getIntent();
        requiredPermissions = PermissionContract.normalizeRequiredPermissions(
                callingIntent.getIntExtra(EXTRA_REQUIRED_PERMISSIONS, PERMISSION_ALL));
        ancestorCallingIntent = callingIntent.getParcelableExtra(EXTRA_INTENT);

        recyclerView = findViewById(R.id.child);
        progress = findViewById(R.id.progress);
        recyclerView.setAdapter(new PermissionRecyclerViewAdapter());
        BaldTitleBar baldTitleBar = findViewById(R.id.bald_title_bar);
        baldTitleBar.setBackListener(v -> calmyBDB());

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissions();
    }

    private void calmyBDB() {
        if (isTaskRoot()) {
            DialogScreen.select(this,
                    R.string.permissions_part,
                    R.string.permissions_calm,
                    -1,
                    index -> {
                        if (index == 1)
                            FakeLauncherActivity.resetPreferredLauncherAndOpenChooser(this);
                    },
                    getText(R.string.stay), getText(R.string.exit));
        } else
            finish();
    }

    private void refreshPermissions() {
        obtainPermissionList();
        if (permissionItemList.isEmpty()) {
            startActivity(ancestorCallingIntent == null ? new Intent(this, HomeScreenActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK) : ancestorCallingIntent);
            finish();
            return;
        }
        progress.setText(getString(R.string.permission_step, totalSteps - permissionItemList.size() + 1, totalSteps));
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private boolean requiresAtom(final int atom) {
        return PermissionContract.hasAtom(requiredPermissions, atom);
    }

    private PermissionItem runtimePermissionItem(final String title, final String explanation, final String... permissions) {
        if (permissionRecovery.shouldOpenAppSettings(this, permissions))
            return new PermissionItem(
                    v -> startActivityForResult(PermissionRecovery.appSettingsIntent(this), REQUEST_CODE_APP_SETTINGS),
                    title,
                    getString(R.string.permission_permanently_denied_subtext),
                    R.string.open_app_settings);
        return new SimplePermissionItem(title, explanation, permissions);
    }

    // totalSteps counts every permission card the flow can show, granted or not,
    // so the «Шаг N из M» header reflects real progress.
    private void obtainPermissionList() {
        permissionItemList.clear();
        totalSteps = 0;
        if (requiredPermissions == PERMISSION_NONE)
            return;

        final boolean defaultDialerRequired = requiresAtom(PermissionContract.PERMISSION_ATOM_DEFAULT_DIALER)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        if (defaultDialerRequired) {
            totalSteps++;
            if (!defaultDialerGranted(this)) {
                permissionItemList.add(
                        new PermissionItem(v -> CallManager.requestDefaultDialer(this),
                                getString(R.string.set_default_dialer),
                                getString(R.string.set_default_dialer_subtext)));
            }
        }

        final boolean writeSettingsRequired = requiresAtom(PermissionContract.PERMISSION_ATOM_WRITE_SETTINGS)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        if (writeSettingsRequired) {
            totalSteps++;
            if (!writeSettingsGranted(this)) {
                permissionItemList.add(
                        new PermissionItem(v -> PermissionActivity.this.startActivityForResult(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                .setData(Uri.parse("package:" + PermissionActivity.this.getPackageName())), 798),
                                getString(R.string.settings_permission),
                                getString(R.string.change_system_settings_subtext)));
            }
        }
        if (requiresAtom(PermissionContract.PERMISSION_ATOM_NOTIFICATION_LISTENER)) {
            totalSteps++;
            if (!notificationListenerGranted(this)) {
                permissionItemList.add(new PermissionItem((v) -> startActivityForResult(new Intent(
                        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), REQUEST_CODES[0]), getString(R.string.enable_notification_access), getString(R.string.enable_notification_access_subtext)));
            }
        }

        if (requiresAtom(PermissionContract.PERMISSION_ATOM_READ_CONTACTS)) {
            totalSteps++;
            if (ActivityCompat.checkSelfPermission(this, READ_CONTACTS) != PERMISSION_GRANTED)
                permissionItemList.add(
                        runtimePermissionItem(getString(R.string.read_contacts), getString(R.string.contacts_read_subtext), READ_CONTACTS));
        }
        if (requiresAtom(PermissionContract.PERMISSION_ATOM_WRITE_CONTACTS)) {
            totalSteps++;
            if (ActivityCompat.checkSelfPermission(this, WRITE_CONTACTS) != PERMISSION_GRANTED)
                permissionItemList.add(
                        runtimePermissionItem(getString(R.string.write_contacts), getString(R.string.contacts_subtext), WRITE_CONTACTS));
        }
        if (requiresAtom(PermissionContract.PERMISSION_ATOM_CALL_PHONE)) {
            totalSteps++;
            if (ActivityCompat.checkSelfPermission(this, CALL_PHONE) != PERMISSION_GRANTED)
                permissionItemList.add(
                        runtimePermissionItem(getString(R.string.calling), getString(R.string.call_subtext), CALL_PHONE));
        }
        if ((requiresAtom(PermissionContract.PERMISSION_ATOM_WRITE_CALL_LOG) || requiresAtom(PermissionContract.PERMISSION_ATOM_READ_CALL_LOG))
                && !BuildConfig.FLAVOR.equals("gPlay")) {
            totalSteps++;
            if (requiresAtom(PermissionContract.PERMISSION_ATOM_WRITE_CALL_LOG)
                    && ActivityCompat.checkSelfPermission(this, WRITE_CALL_LOG) != PERMISSION_GRANTED)
                permissionItemList.add(
                        runtimePermissionItem(getString(R.string.read_call_log), getString(R.string.write_call_log_subtext), WRITE_CALL_LOG));
            else if (requiresAtom(PermissionContract.PERMISSION_ATOM_READ_CALL_LOG)
                    && ActivityCompat.checkSelfPermission(this, READ_CALL_LOG) != PERMISSION_GRANTED)
                permissionItemList.add(
                        runtimePermissionItem(getString(R.string.read_call_log), getString(R.string.read_call_log_subtext), READ_CALL_LOG));
        }
        if (requiresAtom(PermissionContract.PERMISSION_ATOM_READ_PHONE_STATE)) {
            if (BPrefs.get(this).getBoolean(BPrefs.DUAL_SIM_KEY, BPrefs.DUAL_SIM_DEFAULT_VALUE)
                    && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                totalSteps++;
                if (ActivityCompat.checkSelfPermission(this, READ_PHONE_STATE) != PERMISSION_GRANTED)
                    permissionItemList.add(
                            runtimePermissionItem(getString(R.string.read_phone_state), getString(R.string.read_phone_state_subtext), READ_PHONE_STATE));
            }
        }
        if (requiresAtom(PermissionContract.PERMISSION_ATOM_CAMERA)) {
            totalSteps++;
            if (ActivityCompat.checkSelfPermission(this, CAMERA) != PERMISSION_GRANTED)
                permissionItemList.add(
                        runtimePermissionItem(getString(R.string.camera), getString(R.string.camera_subtext), CAMERA));
        }
        if (requiresAtom(PermissionContract.PERMISSION_ATOM_REQUEST_INSTALL_PACKAGES)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && BuildConfig.FLAVOR.equals("baldUpdates")) {
                totalSteps++;
                if (!getPackageManager().canRequestPackageInstalls())
                    permissionItemList.add(
                            new PermissionItem(v -> startActivityForResult(new Intent(
                                    ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", getPackageName()))), REQUEST_CODES[2]), getString(R.string.install_updates), getString(R.string.install_updates_subtext)));
            }
        }
        if (requiresAtom(PermissionContract.PERMISSION_ATOM_WRITE_EXTERNAL_STORAGE)
                && Build.VERSION.SDK_INT < 33) {
            totalSteps++;
            if (ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED)
                permissionItemList.add(
                        runtimePermissionItem(getString(R.string.write_external_storage), getString(R.string.external_storage_subtext), WRITE_EXTERNAL_STORAGE));
        }
        final boolean mediaImagesRequired = requiresAtom(PermissionContract.PERMISSION_ATOM_MEDIA_IMAGES);
        final boolean mediaVideoRequired = requiresAtom(PermissionContract.PERMISSION_ATOM_MEDIA_VIDEO);
        if (mediaImagesRequired || mediaVideoRequired) {
            totalSteps++;
            if (Build.VERSION.SDK_INT >= 33) {
                final List<String> permissions = new ArrayList<>(2);
                if (mediaImagesRequired && !mediaPermissionGranted(this, READ_MEDIA_IMAGES))
                    permissions.add(READ_MEDIA_IMAGES);
                if (mediaVideoRequired && !mediaPermissionGranted(this, READ_MEDIA_VIDEO))
                    permissions.add(READ_MEDIA_VIDEO);
                if (!permissions.isEmpty()) {
                    if (Build.VERSION.SDK_INT >= 34)
                        permissions.add(READ_MEDIA_VISUAL_USER_SELECTED);
                    permissionItemList.add(
                            runtimePermissionItem(getString(R.string.allow_media_access), getString(R.string.allow_media_access_subtext), permissions.toArray(new String[0])));
                }
            } else if (ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED)
                permissionItemList.add(
                        runtimePermissionItem(getString(R.string.write_external_storage), getString(R.string.external_storage_subtext), WRITE_EXTERNAL_STORAGE));
        }
        if (requiresAtom(PermissionContract.PERMISSION_ATOM_POST_NOTIFICATIONS)) {
            if (Build.VERSION.SDK_INT >= 33) {
                totalSteps++;
                if (ActivityCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PERMISSION_GRANTED)
                    permissionItemList.add(
                            runtimePermissionItem(getString(R.string.allow_notifications), getString(R.string.allow_notifications_subtext), POST_NOTIFICATIONS));
            }
        }
        if (requiresAtom(PermissionContract.PERMISSION_ATOM_SYSTEM_ALERT_WINDOW)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                totalSteps++;
                if (!Settings.canDrawOverlays(this)) {
                    final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    permissionItemList.add(
                            new PermissionItem(v -> startActivityForResult(intent, REQUEST_CODES[3]), getString(R.string.appear_on_top), getString(R.string.appear_on_top_subtext)));
                }
            }
        }
        if (requiresAtom(PermissionContract.PERMISSION_ATOM_EXACT_ALARM)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                totalSteps++;
                final android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
                if (!alarmManager.canScheduleExactAlarms()) {
                    final Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName()));
                    permissionItemList.add(
                            new PermissionItem(v -> startActivityForResult(intent, REQUEST_CODES[3]), getString(R.string.allow_exact_alarms), getString(R.string.allow_exact_alarms_subtext)));
                }
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (AppUtils.intArrayContains(REQUEST_CODES, requestCode)) {
            permissionRecovery.onRequestPermissionsResult(this, permissions, grantResults);
            refreshPermissions();
        }
    }

    @Override
    public void onBackPressed() {
        calmyBDB();
    }

    // same exit flow as the title-bar Back button, so keep the callback on API 33+
    @Override
    protected boolean backDispatchIntercepted() {
        return true;
    }

    private class PermissionRecyclerViewAdapter extends ModularRecyclerView.ModularAdapter<PermissionRecyclerViewAdapter.ViewHolder> {
        final LayoutInflater layoutInflater;

        PermissionRecyclerViewAdapter() {
            layoutInflater = PermissionActivity.this.getLayoutInflater();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(layoutInflater.inflate(R.layout.permission_item, parent, false));
        }

        @Override
        public int getItemCount() {
            return permissionItemList.size();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            final PermissionItem permissionItem = permissionItemList.get(position);
            holder.title.setText(permissionItem.title);
            holder.explanation.setText(permissionItem.explanation);
            ((BaldPictureTextButton) holder.allow).setText(permissionItem.actionTextRes);
            holder.allow.setOnClickListener(permissionItem.onClickListener);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView title, explanation;
            final View allow;

            public ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.title);
                explanation = itemView.findViewById(R.id.explanation);
                allow = itemView.findViewById(R.id.allow);
            }
        }
    }

    private class PermissionItem {
        final View.OnClickListener onClickListener;
        final String title, explanation;
        final int actionTextRes;

        PermissionItem(View.OnClickListener onClickListener, String title, String explanation) {
            this(onClickListener, title, explanation, R.string.allow);
        }

        PermissionItem(View.OnClickListener onClickListener, String title, String explanation, int actionTextRes) {
            this.onClickListener = onClickListener;
            this.title = title;
            this.explanation = explanation;
            this.actionTextRes = actionTextRes;
        }
    }

    private class SimplePermissionItem extends PermissionItem {
        SimplePermissionItem(String title, String explanation, String... permissions) {
            super((v) -> ActivityCompat.requestPermissions(PermissionActivity.this, permissions, 789),
                    title,
                    explanation);
        }
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_NONE;
    }
}
