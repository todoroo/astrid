package com.todoroo.astrid.reminders;

import java.util.Date;

import android.app.Notification;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

public class NotificationTests extends DatabaseTestCase {

    @Autowired
    TaskDao taskDao;

    public class MutableBoolean {
        boolean value = false;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Notifications.forceNotificationManager(true);
    }

    @Override
    protected void tearDown() throws Exception {
        Notifications.setNotificationManager(null);
    }

    /** test that a normal task gets a notification */
    public void testAlarmToNotification() {
        final Task task = new Task();
        task.setValue(Task.TITLE, "rubberduck");
        task.setValue(Task.DUE_DATE, DateUtilities.now() - DateUtilities.ONE_DAY);
        taskDao.persist(task);

        final MutableBoolean triggered = new MutableBoolean();

        Notifications.setNotificationManager(new TestNotificationManager() {
            public void notify(int id, Notification notification) {
                assertNotNull(notification.contentIntent);
                triggered.value = true;
            }

        });

        Intent intent = new Intent();
        intent.putExtra(Notifications.ID_KEY, task.getId());
        intent.putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_DUE);
        new Notifications().onReceive(getContext(), intent);
        assertTrue(triggered.value);
    }

    /** test that a deleted task doesn't get a notification */
    public void testDeletedTask() {
        final Task task = new Task();
        task.setValue(Task.TITLE, "gooeyduck");
        task.setValue(Task.DELETION_DATE, DateUtilities.now());
        taskDao.persist(task);

        Notifications.setNotificationManager(new NotificationManager() {

            public void cancel(int id) {
                // allowed
            }

            public void cancelAll() {
                fail("wtf cancel all?");
            }

            public void notify(int id, Notification notification) {
                fail("sent a notification, you shouldn't have...");
            }

        });

        Intent intent = new Intent();
        intent.putExtra(Notifications.ID_KEY, task.getId());
        intent.putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_DUE);
        new Notifications().onReceive(getContext(), intent);
    }

    /** test that a completed task doesn't get a notification */
    public void testCompletedTask() {
        final Task task = new Task();
        task.setValue(Task.TITLE, "rubberduck");
        task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        taskDao.persist(task);

        Notifications.setNotificationManager(new NotificationManager() {

            public void cancel(int id) {
                // allowed
            }

            public void cancelAll() {
                fail("wtf cancel all?");
            }

            public void notify(int id, Notification notification) {
                fail("sent a notification, you shouldn't have...");
            }

        });

        Intent intent = new Intent();
        intent.putExtra(Notifications.ID_KEY, task.getId());
        intent.putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_DUE);
        new Notifications().onReceive(getContext(), intent);
    }

    /** test of quiet hours */
    public void testQuietHours() {
        final Task task = new Task();
        task.setValue(Task.TITLE, "rubberduck");
        taskDao.persist(task);
        Intent intent = new Intent();
        intent.putExtra(Notifications.ID_KEY, task.getId());

        int hour = new Date().getHours();
        Preferences.setStringFromInteger(R.string.p_rmd_quietStart, hour - 1);
        Preferences.setStringFromInteger(R.string.p_rmd_quietEnd, hour + 1);

        // due date notification has vibrate
        Notifications.setNotificationManager(new TestNotificationManager() {
            public void notify(int id, Notification notification) {
                assertNull(notification.sound);
                assertTrue((notification.defaults & Notification.DEFAULT_SOUND) == 0);
                assertNotNull(notification.vibrate);
                assertTrue(notification.vibrate.length > 0);
            }
        });
        intent.putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_DUE);
        new Notifications().onReceive(getContext(), intent);

        // random notification does not
        Notifications.setNotificationManager(new TestNotificationManager() {
            public void notify(int id, Notification notification) {
                assertNull(notification.sound);
                assertTrue((notification.defaults & Notification.DEFAULT_SOUND) == 0);
                assertTrue(notification.vibrate == null ||
                        notification.vibrate.length == 0);
            }
        });
        intent.removeExtra(Notifications.EXTRAS_TYPE);
        intent.putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_RANDOM);
        new Notifications().onReceive(getContext(), intent);

        // wrapping works
        Preferences.setStringFromInteger(R.string.p_rmd_quietStart, hour + 2);
        Preferences.setStringFromInteger(R.string.p_rmd_quietEnd, hour + 1);

        Notifications.setNotificationManager(new TestNotificationManager() {
            public void notify(int id, Notification notification) {
                assertNull(notification.sound);
                assertTrue((notification.defaults & Notification.DEFAULT_SOUND) == 0);
            }
        });
        intent.removeExtra(Notifications.EXTRAS_TYPE);
        intent.putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_DUE);
        new Notifications().onReceive(getContext(), intent);

        // nonstop notification still sounds
        task.setValue(Task.REMINDER_FLAGS, Task.NOTIFY_MODE_NONSTOP);
        taskDao.persist(task);
        Notifications.setNotificationManager(new TestNotificationManager() {
            public void notify(int id, Notification notification) {
                assertTrue(notification.sound != null ||
                        (notification.defaults & Notification.DEFAULT_SOUND) > 0);
            }
        });
        new Notifications().onReceive(getContext(), intent);
    }

    abstract public class TestNotificationManager implements NotificationManager {
        public void cancel(int id) {
            fail("wtf cance?");
        }
        public void cancelAll() {
            fail("wtf cancel all?");
        }
    }

}
