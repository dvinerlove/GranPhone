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

package com.bald.uriah.baldphone.databases.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.bald.uriah.baldphone.activities.HomeScreenActivity;
import com.bald.uriah.baldphone.activities.pills.PillsActivity;
import com.bald.uriah.baldphone.broadcast_receivers.ReminderReceiver;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.Const;
import com.bald.uriah.baldphone.utils.AppUtils;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;

import java.util.List;

/**
 *
 */
public class ReminderScheduler {
    public static final Object LOCK = new Object();
    public static final int SNOOZE_MILLIS = 5 * Const.MINUTE;
    public static final String REMINDER_IS_SNOOZE_VIA_INTENTS = "reminder_is_snooze";

    /**
     * helper class should not be instantiate
     */
    private ReminderScheduler() {
    }

    static int snoozeRequestCode(int key) {
        return -key;
    }

    /** Cancels both the regular reminder and any pending snooze for user-visible cancellation. */
    public static void cancelReminder(int key, Context context) {
        synchronized (LOCK) {
            _cancelReminder(key, context);
            _cancelSnooze(key, context);
        }
    }

    public static void cancelSnooze(int key, Context context) {
        synchronized (LOCK) {
            _cancelSnooze(key, context);
        }
    }
    // BaldDay -
    /*
          sunday = 1
          saturday = 64
          monday =2
          baldDay = 1<<israeliDay...
     */

    // JodaDay -
    /*
        sunday =7
        saturday =6
        monday =1
        jodaDay = day in cristian countries
     */

    private static int getBaldDay() {
        int today = DateTime.now().getDayOfWeek();
        if (today == 7)
            today = 0;
        today = 1 << today;
        return today;
    }

    private static int baldDayToJodaDay(int baldDay) {
        int day = 0;
        while (baldDay != 0) {
            day++;
            baldDay >>= 1;
        }
        day -= 1;

        if (day == 0)
            day = 7;
        return day;
    }

    private static long nextTimeReminderWillWorkInMs(@NonNull Reminder reminder, Context context) {
        final MutableDateTime mDateTime = MutableDateTime.now();

        {   //creating a date of today with the hours and minutes of the alarm
            mDateTime.setMillisOfSecond(0);
            mDateTime.setSecondOfMinute(0);

            mDateTime.setHourOfDay(BPrefs.getHour(reminder.getStartingTime(), context));
            mDateTime.setMinuteOfHour(BPrefs.getMinute(reminder.getStartingTime(), context));
        }

        final int baldDay = getBaldDay();
        final int days = reminder.getDays();
        {   //today or one time
            if ((days & baldDay) == baldDay) {//today may have an alarm
                if ((days == baldDay)) {
                    if (mDateTime.isBeforeNow())
                        mDateTime.addWeeks(1);  //next week if today's time already passed
                    return mDateTime.getMillis();
                } else {
                    if (mDateTime.isAfterNow())
                        return mDateTime.getMillis();
                }
            } else if (days == -1) {
                if (mDateTime.isBeforeNow())
                    mDateTime.addDays(1);
                return mDateTime.getMillis();
            }
        }

        int selectedBaldDay = baldDay;

        {   //find next day
            for (int i = baldDay << 1; i != baldDay; i <<= 1) {
                if (i > Const.Days.SATURDAY)
                    i = Const.Days.SUNDAY;

                if ((days & i) == i) {
                    selectedBaldDay = i;
                    break;
                }

            }
        }

        int day = baldDayToJodaDay(selectedBaldDay);
        mDateTime.setDayOfWeek(day);
        if (mDateTime.isBeforeNow()) {
            mDateTime.addWeeks(1);
        }
        return mDateTime.getMillis();
    }

    private static void _cancelReminder(int key, Context context) {
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(getIntent(context, key));
    }

    private static void _cancelSnooze(int key, Context context) {
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(getSnoozeIntent(context, key));
    }

    public static void scheduleReminder(@NonNull Reminder reminder, @NonNull Context context) throws IllegalArgumentException {
        synchronized (LOCK) {
            final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // exact alarms need SCHEDULE_EXACT_ALARM on API 31+ (enforced at least since Android 15);
            // skipping must never crash the boot chain — the permission wizard re-gates on next UI entry
            if (!AppUtils.canScheduleExactAlarms(alarmManager)) {
                AppUtils.logImportant("cannot schedule exact alarms, skipping");
                return;
            }
            final long nextTimeReminderWillWorkInMs = nextTimeReminderWillWorkInMs(reminder, context);
            alarmManager.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(
                            nextTimeReminderWillWorkInMs,
                            PendingIntent.getActivity(context, 0, new Intent(context, PillsActivity.class), PendingIntent.FLAG_IMMUTABLE)
                    ),
                    getIntent(context, reminder.getId())
            );
        }
    }

    private static PendingIntent getIntent(Context context, int alarmKey) {
        Intent intent = new Intent(context, ReminderReceiver.class).putExtra(Reminder.REMINDER_KEY_VIA_INTENTS, alarmKey);
        return PendingIntent.getBroadcast(context, alarmKey, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent getSnoozeIntent(Context context, int alarmKey) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .putExtra(Reminder.REMINDER_KEY_VIA_INTENTS, alarmKey)
                .putExtra(REMINDER_IS_SNOOZE_VIA_INTENTS, true);
        return PendingIntent.getBroadcast(context, snoozeRequestCode(alarmKey), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void scheduleSnooze(@NonNull Reminder alarm, Context context) throws IllegalArgumentException {
        synchronized (LOCK) {
            final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // exact alarms need SCHEDULE_EXACT_ALARM on API 31+ (enforced at least since Android 15);
            // skipping must never crash the boot chain — the permission wizard re-gates on next UI entry
            if (!AppUtils.canScheduleExactAlarms(alarmManager)) {
                AppUtils.logImportant("cannot schedule exact alarms, skipping");
                return;
            }
            alarmManager.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(
                            DateTime.now().getMillis() + SNOOZE_MILLIS,
                            PendingIntent.getActivity(context, alarm.getId(), new Intent(context, HomeScreenActivity.class), PendingIntent.FLAG_IMMUTABLE)//TODO??
                    ),
                    getSnoozeIntent(context, alarm.getId())
            );
        }
    }

    /** Resolves the next regular reminder state after a regular, non-snooze reminder fired. */
    public static void rescheduleAfterFired(final int id, final Context context) {
        synchronized (LOCK) {
            final RemindersDatabaseDao dao = RemindersDatabase.getInstance(context).remindersDatabaseDao();
            final Reminder current = dao.getById(id);
            if (current == null) {
                cancelReminder(id, context);
                return;
            }
            scheduleReminder(current, context);
        }
    }

    public static boolean reStartReminders(final Context context) {
        AppUtils.logImportant("reStartReminders was called!");
        synchronized (LOCK) {
            AppUtils.logImportant("reStartReminders was started!");
            final RemindersDatabaseDao dao = RemindersDatabase.getInstance(context).remindersDatabaseDao();
            final List<Reminder> alarmList = dao.getAllReminders();
            for (Reminder snapshot : alarmList) {
                if (Thread.currentThread().isInterrupted())
                    return false;
                final Reminder current = dao.getById(snapshot.getId());
                if (current == null)
                    cancelReminder(snapshot.getId(), context);
                else
                    scheduleReminder(current, context);
            }
            AppUtils.logImportant("reStartReminders has finished!");
            return true;
        }
    }
}



