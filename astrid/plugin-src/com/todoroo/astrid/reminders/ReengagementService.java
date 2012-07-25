/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.utility.Constants;

public final class ReengagementService {

    private static final int REQUEST_CODE = 10;


    public static final String PREF_REENGAGEMENT_COUNT = "pref_reengagement_count"; //$NON-NLS-1$

    public static final String BROADCAST_SHOW_REENGAGEMENT = Constants.PACKAGE + ".SHOW_REENGAGEMENT"; //$NON-NLS-1$

    public static void scheduleReengagementAlarm(Context context) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(BROADCAST_SHOW_REENGAGEMENT);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, 0);
        am.cancel(pendingIntent);

        long time = getNextReminderTime();
        am.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }

    private static long getNextReminderTime() {
        int reengagementReminders = Preferences.getInt(PREF_REENGAGEMENT_COUNT, 1);
        int days;
        if (reengagementReminders >= 4)
            days = 9;
        else
            days = 1 + reengagementReminders * 2;

        Date date = new Date(DateUtilities.now() + DateUtilities.ONE_DAY * days / 1000L * 1000L);
        date.setHours(18);
        date.setMinutes(0);
        date.setSeconds(0);

        return date.getTime();
    }

}
