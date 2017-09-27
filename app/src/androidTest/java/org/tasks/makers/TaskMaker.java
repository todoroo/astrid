package org.tasks.makers;

import com.google.common.base.Strings;
import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyValue;
import com.todoroo.astrid.data.Task;

import org.tasks.time.DateTime;

import static com.natpryce.makeiteasy.Property.newProperty;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.makers.Maker.make;

public class TaskMaker {

    public static Property<Task, Long> ID = newProperty();
    public static Property<Task, String> TITLE = newProperty();
    public static Property<Task, DateTime> DUE_DATE = newProperty();
    public static Property<Task, DateTime> DUE_TIME = newProperty();
    public static Property<Task, Integer> PRIORITY = newProperty();
    public static Property<Task, DateTime> REMINDER_LAST = newProperty();
    public static Property<Task, Long> RANDOM_REMINDER_PERIOD = newProperty();
    public static Property<Task, Integer> HIDE_TYPE = newProperty();
    public static Property<Task, Integer> REMINDERS = newProperty();
    public static Property<Task, DateTime> CREATION_TIME = newProperty();
    public static Property<Task, DateTime> COMPLETION_TIME = newProperty();
    public static Property<Task, DateTime> DELETION_TIME = newProperty();
    public static Property<Task, DateTime> SNOOZE_TIME = newProperty();

    @SafeVarargs
    public static Task newTask(PropertyValue<? super Task, ?>... properties) {
        return make(instantiator, properties);
    }

    private static final Instantiator<Task> instantiator = lookup -> {
        Task task = new Task();

        String title = lookup.valueOf(TITLE, (String) null);
        if (!Strings.isNullOrEmpty(title)) {
            task.setTitle(title);
        }

        long id = lookup.valueOf(ID, Task.NO_ID);
        if (id != Task.NO_ID) {
            task.setId(id);
        }

        int priority = lookup.valueOf(PRIORITY, -1);
        if (priority >= 0) {
            task.setImportance(priority);
        }

        DateTime dueDate = lookup.valueOf(DUE_DATE, (DateTime) null);
        if (dueDate != null) {
            task.setDueDate(Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate.getMillis()));
        }

        DateTime dueTime = lookup.valueOf(DUE_TIME, (DateTime) null);
        if (dueTime != null) {
            task.setDueDate(Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, dueTime.getMillis()));
        }

        DateTime completionTime = lookup.valueOf(COMPLETION_TIME, (DateTime) null);
        if (completionTime != null) {
            task.setCompletionDate(completionTime.getMillis());
        }

        DateTime deletedTime = lookup.valueOf(DELETION_TIME, (DateTime) null);
        if (deletedTime != null) {
            task.setDeletionDate(deletedTime.getMillis());
        }

        DateTime snoozeTime = lookup.valueOf(SNOOZE_TIME, (DateTime) null);
        if (snoozeTime != null) {
            task.setReminderSnooze(snoozeTime.getMillis());
        }

        int hideType = lookup.valueOf(HIDE_TYPE, -1);
        if (hideType >= 0) {
            task.setHideUntil(task.createHideUntil(hideType, 0));
        }

        int reminderFlags = lookup.valueOf(REMINDERS, -1);
        if (reminderFlags >= 0) {
            task.setReminderFlags(reminderFlags);
        }

        DateTime reminderLast = lookup.valueOf(REMINDER_LAST, (DateTime) null);
        if (reminderLast != null) {
            task.setReminderLast(reminderLast.getMillis());
        }

        long randomReminderPeriod = lookup.valueOf(RANDOM_REMINDER_PERIOD, 0L);
        if (randomReminderPeriod > 0) {
            task.setReminderPeriod(randomReminderPeriod);
        }

        DateTime creationTime = lookup.valueOf(CREATION_TIME, newDateTime());
        task.setCreationDate(creationTime.getMillis());

        return task;
    };
}
