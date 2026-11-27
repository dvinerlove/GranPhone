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

package com.bald.uriah.baldphone.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.bald.uriah.baldphone.databases.alarms.AlarmScheduler;
import com.bald.uriah.baldphone.databases.reminders.ReminderScheduler;

/** System-tracked reconciliation of Room rows with AlarmManager state. */
public class AlarmRecoveryJobService extends JobService {
    private static final String TAG = AlarmRecoveryJobService.class.getSimpleName();
    private static final int JOB_ID = 4101;
    private static final long OVERRIDE_DEADLINE_MS = 60_000;
    private static final long BACKOFF_MS = 10_000;

    private volatile Thread workerThread;

    public static void schedule(final Context context) {
        final Context appContext = context.getApplicationContext();
        final JobScheduler jobScheduler =
                (JobScheduler) appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null)
            return;

        final ComponentName componentName = new ComponentName(appContext, AlarmRecoveryJobService.class);
        try {
            if (Build.VERSION.SDK_INT >= 31
                    && jobScheduler.schedule(buildJob(componentName, true)) == JobScheduler.RESULT_SUCCESS)
                return;

            // Expedited jobs have a quota; the regular job is the required API 21-30 path too.
            jobScheduler.schedule(buildJob(componentName, false));
        } catch (RuntimeException e) {
            // Recovery is best-effort and must never crash a boot/permission/home entry point.
            Log.e(TAG, "could not schedule alarm recovery", e);
        }
    }

    private static JobInfo buildJob(final ComponentName componentName, final boolean expedited) {
        final JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName)
                .setBackoffCriteria(BACKOFF_MS, JobInfo.BACKOFF_POLICY_LINEAR);
        if (expedited && Build.VERSION.SDK_INT >= 31)
            builder.setExpedited(true); // expedited jobs cannot also have an override deadline
        else
            builder.setOverrideDeadline(OVERRIDE_DEADLINE_MS);
        return builder.build();
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        final Context appContext = getApplicationContext();
        workerThread = new Thread(() -> {
            final boolean alarmsFinished = runAlarms(appContext);
            final boolean remindersFinished = runReminders(appContext);
            workerThread = null;
            jobFinished(params, !(alarmsFinished && remindersFinished));
        }, "AlarmRecoveryJob");
        workerThread.start();
        return true;
    }

    private boolean runAlarms(final Context context) {
        try {
            return AlarmScheduler.reStartAlarms(context);
        } catch (RuntimeException e) {
            Log.e(TAG, "alarm recovery failed", e);
            return false;
        }
    }

    private boolean runReminders(final Context context) {
        try {
            return ReminderScheduler.reStartReminders(context);
        } catch (RuntimeException e) {
            Log.e(TAG, "reminder recovery failed", e);
            return false;
        }
    }

    @Override
    public boolean onStopJob(final JobParameters params) {
        final Thread thread = workerThread;
        if (thread != null)
            thread.interrupt();
        // The same job id dedupes a replacement with this retry. Recovery re-reads every row,
        // so a superseded worker which is already inside AlarmManager cannot overwrite newer state.
        return true;
    }
}
