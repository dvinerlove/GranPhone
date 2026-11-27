package com.bald.uriah.baldphone.databases.reminders;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ReminderSchedulerTest {
    @Test
    public void snoozeRequestCodeIsStableAndDistinctFromRegularReminder() {
        assertEquals(-7, ReminderScheduler.snoozeRequestCode(7));
        assertNotEquals(7, ReminderScheduler.snoozeRequestCode(7));
    }
}
