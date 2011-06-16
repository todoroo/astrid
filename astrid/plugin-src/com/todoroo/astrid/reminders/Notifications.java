package com.todoroo.astrid.reminders;

import java.util.Date;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.voice.VoiceOutputService;

public class Notifications extends BroadcastReceiver {

    // --- constants

    /** task id extra */
    public static final String ID_KEY = "id"; //$NON-NLS-1$

    /** notification type extra */
    public static final String TYPE_KEY = "type"; //$NON-NLS-1$

    /** preference values */
    public static final int ICON_SET_PINK = 0;
    public static final int ICON_SET_BORING = 1;
    public static final int ICON_SET_ASTRID = 2;

    // --- instance variables

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private ExceptionService exceptionService;

    public static NotificationManager notificationManager = null;

    // --- alarm handling

    static {
        AstridDependencyInjector.initialize();
    }

    public Notifications() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    /** Alarm intent */
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);

        long id = intent.getLongExtra(ID_KEY, 0);
        int type = intent.getIntExtra(TYPE_KEY, (byte) 0);

        Resources r = context.getResources();
        String reminder;

        if(type == ReminderService.TYPE_ALARM)
            reminder = getRandomReminder(r.getStringArray(R.array.reminders_alarm));
        else if(Preferences.getBoolean(R.string.p_rmd_nagging, true)) {
            if(type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_OVERDUE)
                reminder = getRandomReminder(r.getStringArray(R.array.reminders_due));
            else if(type == ReminderService.TYPE_SNOOZE)
                reminder = getRandomReminder(r.getStringArray(R.array.reminders_snooze));
            else
                reminder = getRandomReminder(r.getStringArray(R.array.reminders));
        } else
            reminder = ""; //$NON-NLS-1$

        synchronized(Notifications.class) {
            if(notificationManager == null)
                notificationManager = new AndroidNotificationManager(context);
        }

        if(!showTaskNotification(id, type, reminder)) {
            notificationManager.cancel((int)id);
        }

        try {
            VoiceOutputService.getVoiceOutputInstance().onDestroy();
        } catch (VerifyError e) {
            // unavailable
        }
    }

    // --- notification creation

    /** @return a random reminder string */
    static String getRandomReminder(String[] reminders) {
        int next = ReminderService.random.nextInt(reminders.length);
        String reminder = reminders[next];
        return reminder;
    }

    /**
     * Show a new notification about the given task. Returns false if there was
     * some sort of error or the alarm should be disabled.
     */
    public boolean showTaskNotification(long id, int type, String reminder) {
        Task task;
        try {
            task = taskDao.fetch(id, Task.ID, Task.TITLE, Task.HIDE_UNTIL, Task.COMPLETION_DATE,
                        Task.DUE_DATE, Task.DELETION_DATE, Task.REMINDER_FLAGS, Task.USER_ID);
            if(task == null)
                throw new IllegalArgumentException("cound not find item with id"); //$NON-NLS-1$

        } catch (Exception e) {
            exceptionService.reportError("show-notif", e); //$NON-NLS-1$
            return false;
        }

        // you're done, or not yours - don't sound, do delete
        if(task.isCompleted() || task.isDeleted() || task.getValue(Task.USER_ID) != 0)
            return false;

        // it's hidden - don't sound, don't delete
        if(task.isHidden() && type == ReminderService.TYPE_RANDOM)
            return true;

        // task due date was changed, but alarm wasn't rescheduled
        if((type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_OVERDUE) &&
                (!task.hasDueDate() || task.getValue(Task.DUE_DATE) > DateUtilities.now()))
            return true;

        // read properties
        String taskTitle = task.getValue(Task.TITLE);
        boolean nonstopMode = task.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_MODE_NONSTOP);
        boolean ringFiveMode = task.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_MODE_FIVE);
        int ringTimes = nonstopMode ? -1 : (ringFiveMode ? 5 : 1);

        // update last reminder time
        task.setValue(Task.REMINDER_LAST, DateUtilities.now());
        taskDao.saveExisting(task);

        Context context = ContextManager.getContext();
        String title = context.getString(R.string.app_name);
        String text = reminder + " " + taskTitle; //$NON-NLS-1$

        Intent notifyIntent = new Intent(context, NotificationActivity.class);
        notifyIntent.setAction("NOTIFY" + id); //$NON-NLS-1$
        notifyIntent.putExtra(NotificationActivity.TOKEN_ID, id);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        showNotification((int)id, notifyIntent, type, title, text, ringTimes);
        return true;
    }

    /**
     * Shows an Astrid notification. Pulls in ring tone and quiet hour settings
     * from preferences. You can make it say anything you like.
     * @param ringTimes number of times to ring (-1 = nonstop)
     */
    public static void showNotification(int notificationId, Intent intent, int type, String title,
            String text, int ringTimes) {
        Context context = ContextManager.getContext();
        if(notificationManager == null)
            notificationManager = new AndroidNotificationManager(context);

        // quiet hours? unless alarm clock
        boolean quietHours = ringTimes < 0 ? false : isQuietHours();

        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // set up properties (name and icon) for the notification
        int icon;
        switch(Preferences.getIntegerFromString(R.string.p_rmd_icon,
                ICON_SET_ASTRID)) {
        case ICON_SET_PINK:
            icon = R.drawable.notif_pink_alarm;
            break;
        case ICON_SET_BORING:
            icon = R.drawable.notif_boring_alarm;
            break;
        default:
            icon = R.drawable.notif_astrid;
        }

        // create notification object
        Notification notification = new Notification(
                icon, text, System.currentTimeMillis());
        notification.setLatestEventInfo(context,
                title,
                text,
                pendingIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if(Preferences.getBoolean(R.string.p_rmd_persistent, true)) {
            notification.flags |= Notification.FLAG_NO_CLEAR |
                Notification.FLAG_SHOW_LIGHTS;
            notification.ledOffMS = 5000;
            notification.ledOnMS = 700;
            notification.ledARGB = Color.YELLOW;
        }
        else
            notification.defaults = Notification.DEFAULT_LIGHTS;

        AudioManager audioManager = (AudioManager)context.getSystemService(
                Context.AUDIO_SERVICE);

        // detect call state
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int callState = tm.getCallState();

        boolean voiceReminder = Preferences.getBoolean(R.string.p_voiceRemindersEnabled, false);

        // if multi-ring is activated, set up the flags for insistent
        // notification, and increase the volume to full volume, so the user
        // will actually pay attention to the alarm
        if(ringTimes != 1 && (type != ReminderService.TYPE_RANDOM)) {
            notification.audioStreamType = AudioManager.STREAM_ALARM;
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);

            // insistent rings until notification is disabled
            if(ringTimes < 0) {
                notification.flags |= Notification.FLAG_INSISTENT;
                voiceReminder = false;
            }

        } else {
            notification.audioStreamType = AudioManager.STREAM_NOTIFICATION;
        }


        // quiet hours = no sound
        if(quietHours || callState != TelephonyManager.CALL_STATE_IDLE) {
            notification.sound = null;
            voiceReminder = false;
        } else {
            String notificationPreference = Preferences.getStringValue(R.string.p_rmd_ringtone);
            if(audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                notification.sound = null;
                voiceReminder = false;
            } else if(notificationPreference != null) {
                if(notificationPreference.length() > 0) {
                    Uri notificationSound = Uri.parse(notificationPreference);
                    notification.sound = notificationSound;
                } else {
                    notification.sound = null;
                }
            } else {
                notification.defaults |= Notification.DEFAULT_SOUND;
            }
        }

        // quiet hours && ! due date or snooze = no vibrate
        if(quietHours && !(type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_SNOOZE)) {
            notification.vibrate = null;
        } else if(callState != TelephonyManager.CALL_STATE_IDLE) {
            notification.vibrate = null;
        } else {
            if (Preferences.getBoolean(R.string.p_rmd_vibrate, true)
                    && audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION)) {
                notification.vibrate = new long[] {0, 1000, 500, 1000, 500, 1000};
            } else {
                notification.vibrate = null;
            }
        }

        if(Constants.DEBUG)
            Log.w("Astrid", "Logging notification: " + text); //$NON-NLS-1$ //$NON-NLS-2$

        for(int i = 0; i < Math.max(ringTimes, 1); i++) {
            notificationManager.notify(notificationId, notification);
            AndroidUtilities.sleepDeep(500);
        }

        if (voiceReminder) {
            AndroidUtilities.sleepDeep(2000);
            for(int i = 0; i < 50; i++) {
                AndroidUtilities.sleepDeep(500);
                if(audioManager.getMode() != AudioManager.MODE_RINGTONE)
                    break;
            }
            try {
                VoiceOutputService.getVoiceOutputInstance().queueSpeak(text);
            } catch (VerifyError e) {
                // unavailable
            }
        }
    }

    /**
     * @return whether we're in quiet hours
     */
    public static boolean isQuietHours() {
        int quietHoursStart = Preferences.getIntegerFromString(R.string.p_rmd_quietStart, -1);
        int quietHoursEnd = Preferences.getIntegerFromString(R.string.p_rmd_quietEnd, -1);
        if(quietHoursStart != -1 && quietHoursEnd != -1) {
            int hour = new Date().getHours();
            if(quietHoursStart <= quietHoursEnd) {
                if(hour >= quietHoursStart && hour < quietHoursEnd)
                    return true;
            } else { // wrap across 24/hour boundary
                if(hour >= quietHoursStart || hour < quietHoursEnd)
                    return true;
            }
        }
        return false;
    }

    /**
     * Schedules alarms for a single task
     *
     * @param shouldPerformPropertyCheck
     *            whether to check if task has requisite properties
     */
    public static void cancelNotifications(long taskId) {
        if(notificationManager == null)
            synchronized(Notifications.class) {
                if(notificationManager == null)
                    notificationManager = new AndroidNotificationManager(
                            ContextManager.getContext());
            }

        notificationManager.cancel((int)taskId);
    }

    // --- notification manager

    public static void setNotificationManager(
            NotificationManager notificationManager) {
        Notifications.notificationManager = notificationManager;
    }

}