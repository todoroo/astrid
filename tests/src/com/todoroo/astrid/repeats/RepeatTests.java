package com.todoroo.astrid.repeats;

import java.util.Date;

import android.content.Intent;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.astrid.utility.Flags;

public class RepeatTests extends DatabaseTestCase {

    @Autowired
    TaskDao taskDao;

    @Autowired
    MetadataDao metadataDao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Preferences.setStringFromInteger(R.string.p_default_urgency_key, 0);
        RepeatTaskCompleteListener.setSkipActFmCheck(true);
    }

    /** test that completing a task w/ no repeats does nothing */
    public void testNoRepeats() throws Exception{
        Task task = new Task();
        task.setValue(Task.TITLE, "nothing");
        taskDao.save(task);

        task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        saveAndTriggerRepeatListener(task);
        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.ID));
        try {
            assertEquals(1, cursor.getCount());
        } finally {
            cursor.close();
        }
    }

    private void saveAndTriggerRepeatListener(Task task) {
        Flags.set(Flags.SUPPRESS_HOOKS);
        if(task.isSaved())
            taskDao.saveExisting(task);
        else
            taskDao.createNew(task);

        Intent intent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_COMPLETED);
        intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
        new RepeatTaskCompleteListener().onReceive(getContext(), intent);
    }

    /** test daily repeat from due date, but with no due date set */
    public void testDailyWithNoDueDate() throws Exception {
        Task task = new Task();
        task.setValue(Task.TITLE, "daily");
        RRule rrule = new RRule();
        rrule.setInterval(5);
        rrule.setFreq(Frequency.DAILY);
        task.setValue(Task.RECURRENCE, rrule.toIcal());
        taskDao.save(task);

        task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        saveAndTriggerRepeatListener(task);

        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.PROPERTIES));
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            task.readFromCursor(cursor);

            assertEquals("daily", task.getValue(Task.TITLE));
            assertFalse(task.hasDueDate());
            assertTrue(task.isCompleted());

            cursor.moveToNext();
            task.readFromCursor(cursor);
            assertEquals("daily", task.getValue(Task.TITLE));
            assertFalse(task.isCompleted());
            long dueDate = task.getValue(Task.DUE_DATE);
            assertFalse(task.hasDueTime());
            assertTrue("Due date is '" + new Date(dueDate) + "', expected more like '" +
                        new Date(DateUtilities.now() + 5 * DateUtilities.ONE_DAY) + "'",
                    Math.abs(dueDate - DateUtilities.now() - 5 * DateUtilities.ONE_DAY) < DateUtilities.ONE_DAY);
        } finally {
            cursor.close();
        }
    }

    /** test weekly repeat from due date, with due date & time set */
    public void testWeeklyWithDueDate() throws Exception {
        Task task = new Task();
        task.setValue(Task.TITLE, "weekly");
        RRule rrule = new RRule();
        rrule.setInterval(1);
        rrule.setFreq(Frequency.WEEKLY);
        task.setValue(Task.RECURRENCE, rrule.toIcal());
        long originalDueDate = (DateUtilities.now() - 3 * DateUtilities.ONE_DAY) / 1000L * 1000L;
        task.setValue(Task.DUE_DATE, Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, originalDueDate));
        taskDao.save(task);

        task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        saveAndTriggerRepeatListener(task);

        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.PROPERTIES));
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            task.readFromCursor(cursor);

            assertEquals("weekly", task.getValue(Task.TITLE));
            assertEquals(originalDueDate, (long)task.getValue(Task.DUE_DATE));
            assertTrue(task.isCompleted());

            cursor.moveToNext();
            task.readFromCursor(cursor);
            assertEquals("weekly", task.getValue(Task.TITLE));
            assertFalse(task.isCompleted());
            long dueDate = task.getValue(Task.DUE_DATE);
            assertTrue(task.hasDueTime());
            assertEquals("Due date is '" + new Date(dueDate) + "', expected exactly '" +
                    new Date(originalDueDate + DateUtilities.ONE_WEEK) + "': ",
                originalDueDate + DateUtilities.ONE_WEEK, dueDate);
        } finally {
            cursor.close();
        }
    }

    /** test hourly repeat from due date, with due date but no time */
    public void testHourlyFromDueDate() throws Exception {
        Task task = new Task();
        task.setValue(Task.TITLE, "hourly");
        RRule rrule = new RRule();
        rrule.setInterval(4);
        rrule.setFreq(Frequency.HOURLY);
        task.setValue(Task.RECURRENCE, rrule.toIcal());
        long originalDueDate = (DateUtilities.now() + DateUtilities.ONE_DAY) / 1000L * 1000L;
        task.setValue(Task.DUE_DATE, Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, originalDueDate));
        taskDao.save(task);

        task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        saveAndTriggerRepeatListener(task);

        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.PROPERTIES));
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            task.readFromCursor(cursor);

            assertEquals(originalDueDate, (long)task.getValue(Task.DUE_DATE));
            assertTrue(task.isCompleted());

            cursor.moveToNext();
            task.readFromCursor(cursor);
            assertFalse(task.isCompleted());
            long dueDate = task.getValue(Task.DUE_DATE);
            assertTrue(task.hasDueTime());
            assertEquals("Due date is '" + new Date(dueDate) + "', expected exactly '" +
                    new Date(originalDueDate + 4 * DateUtilities.ONE_HOUR) + "'",
                originalDueDate + 4 * DateUtilities.ONE_HOUR, dueDate);
        } finally {
            cursor.close();
        }
    }

    public void testMinutelyFromDueDate() throws Exception {
        Task task = new Task();
        task.setValue(Task.TITLE, "minutely");
        RRule rrule = new RRule();
        rrule.setInterval(30);
        rrule.setFreq(Frequency.MINUTELY);
        task.setValue(Task.RECURRENCE, rrule.toIcal());
        long originalDueDate = (DateUtilities.now() + DateUtilities.ONE_DAY) / 1000L * 1000L;
        task.setValue(Task.DUE_DATE, Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, originalDueDate));
        taskDao.save(task);

        task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        saveAndTriggerRepeatListener(task);

        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.PROPERTIES));
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            task.readFromCursor(cursor);

            assertEquals(originalDueDate, (long)task.getValue(Task.DUE_DATE));
            assertTrue(task.isCompleted());

            cursor.moveToNext();
            task.readFromCursor(cursor);
            assertFalse(task.isCompleted());
            long dueDate = task.getValue(Task.DUE_DATE);
            assertTrue(task.hasDueTime());
            assertEquals("Due date is '" + new Date(dueDate) + "', expected exactly '" +
                    new Date(originalDueDate + 4 * DateUtilities.ONE_HOUR) + "'",
                originalDueDate + 30 * DateUtilities.ONE_MINUTE, dueDate);
        } finally {
            cursor.close();
        }
    }

    /** test after completion flag */
    public void testRepeatAfterComplete() throws Exception {
        // create a weekly task due a couple days in the past, but with the 'after completion'
        // specified. should be due 7 days from now
        Task task = new Task();
        task.setValue(Task.TITLE, "afterComplete");
        RRule rrule = new RRule();
        rrule.setInterval(1);
        rrule.setFreq(Frequency.WEEKLY);
        task.setValue(Task.RECURRENCE, rrule.toIcal());
        long originalDueDate = (DateUtilities.now() - 3 * DateUtilities.ONE_DAY) / 1000L * 1000L;
        task.setValue(Task.DUE_DATE, Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, originalDueDate));
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, true);
        taskDao.save(task);

        task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        saveAndTriggerRepeatListener(task);

        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.PROPERTIES));
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            task.readFromCursor(cursor);

            assertTrue(task.hasDueDate());
            assertTrue(task.isCompleted());

            cursor.moveToNext();
            task.readFromCursor(cursor);
            assertFalse(task.isCompleted());
            long dueDate = task.getValue(Task.DUE_DATE);
            assertFalse(task.hasDueTime());

            assertTrue("Due date is '" + new Date(dueDate) + "', expected more like '" +
                    new Date(DateUtilities.now() + DateUtilities.ONE_WEEK) + "'",
                Math.abs(dueDate - DateUtilities.now() - DateUtilities.ONE_WEEK) < DateUtilities.ONE_DAY);
        } finally {
            cursor.close();
        }
    }

    /** test that metadata is transferred to new task */
    public void testMetadataIsCopied() throws Exception {
        Task task = new Task();
        task.setValue(Task.TITLE, "meta data test");
        RRule rrule = new RRule();
        rrule.setInterval(5);
        rrule.setFreq(Frequency.DAILY);
        task.setValue(Task.RECURRENCE, rrule.toIcal());
        taskDao.save(task);

        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, "special");
        metadata.setValue(Metadata.VALUE1, "sauce");
        metadata.setValue(Metadata.TASK, task.getId());
        metadataDao.persist(metadata);

        task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        saveAndTriggerRepeatListener(task);

        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(Metadata.TASK));
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            metadata.readFromCursor(cursor);

            assertSame(task.getId(), (long)metadata.getValue(Metadata.TASK));

            cursor.moveToNext();
            metadata.readFromCursor(cursor);
            assertNotSame(task.getId(), (long)metadata.getValue(Metadata.TASK));
        } finally {
            cursor.close();
        }
    }

    /** test hide unitl date is repeated */
    public void testHideUntilRepeated() throws Exception {
        Task task = new Task();
        task.setValue(Task.TITLE, "hideUntil");
        RRule rrule = new RRule();
        rrule.setInterval(1);
        rrule.setFreq(Frequency.WEEKLY);
        task.setValue(Task.RECURRENCE, rrule.toIcal());
        task.setValue(Task.DUE_DATE, Task.createDueDate(Task.URGENCY_TODAY, 0));
        task.setValue(Task.HIDE_UNTIL, task.createHideUntil(Task.HIDE_UNTIL_DAY_BEFORE, 0));
        taskDao.save(task);

        task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        saveAndTriggerRepeatListener(task);

        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.PROPERTIES));
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            task.readFromCursor(cursor);

            assertTrue(task.hasDueDate());
            assertTrue(task.isCompleted());
            assertFalse(task.isHidden());

            cursor.moveToNext();
            task.readFromCursor(cursor);
            assertFalse(task.isCompleted());
            assertTrue(task.isHidden());
            long date = task.getValue(Task.HIDE_UNTIL);
            assertTrue("Hide Until date is '" + new Date(date) + "', expected more like '" +
                    new Date(DateUtilities.now() + 6 * DateUtilities.ONE_DAY) + "'",
                    Math.abs(date - DateUtilities.now() - 6 * DateUtilities.ONE_DAY) < DateUtilities.ONE_DAY);
        } finally {
            cursor.close();
        }
    }
}
