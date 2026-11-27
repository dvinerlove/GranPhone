package com.bald.uriah.baldphone.databases.alarms;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AlarmSchedulerTest {
    @Test
    public void snoozeRequestCodeIsStableAndDistinctFromRegularAlarm() {
        assertEquals(-7, AlarmScheduler.snoozeRequestCode(7));
        assertNotEquals(7, AlarmScheduler.snoozeRequestCode(7));
    }
}
