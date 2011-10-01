package com.todoroo.astrid.gcal;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.Calendars.CalendarResult;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GCalControlSet implements TaskEditControlSet {

    // --- instance variables

    @Autowired
    private ExceptionService exceptionService;

    private final Activity activity;

    private Uri calendarUri = null;

    private Task myTask;
    private final CalendarResult calendars;
    private final CheckBox addToCalendar;
    private final Spinner calendarSelector;
    private final Button viewCalendarEvent;

    public GCalControlSet(final Activity activity, ViewGroup parent) {
        DependencyInjectionService.getInstance().inject(this);

        this.activity = activity;
        LayoutInflater.from(activity).inflate(R.layout.gcal_control, parent, true);

        this.addToCalendar = (CheckBox) activity.findViewById(R.id.add_to_calendar);
        this.calendarSelector = (Spinner) activity.findViewById(R.id.calendars);
        this.viewCalendarEvent = (Button) activity.findViewById(R.id.view_calendar_event);

        calendars = Calendars.getCalendars();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, calendars.calendars);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        calendarSelector.setAdapter(adapter);
        calendarSelector.setSelection(calendars.defaultIndex);

        addToCalendar.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                calendarSelector.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        viewCalendarEvent.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("nls")
            @Override
            public void onClick(View v) {
                if(calendarUri == null)
                    return;

                ContentResolver cr = activity.getContentResolver();
                Cursor cursor = cr.query(calendarUri, new String[] { "dtstart", "dtend" },
                        null, null, null);

                Intent intent = new Intent(Intent.ACTION_EDIT, calendarUri);
                try {
                    if(cursor == null || cursor.getCount() == 0) {
                        // event no longer exists, recreate it
                        calendarUri = null;
                        writeToModel(myTask);
                        return;
                    }
                    cursor.moveToFirst();
                    intent.putExtra("beginTime", cursor.getLong(0));
                    intent.putExtra("endTime", cursor.getLong(1));
                } catch (Exception e) {
                    Log.e("gcal-error", "Error opening calendar", e); //$NON-NLS-1$ //$NON-NLS-2$
                    Toast.makeText(activity, R.string.gcal_TEA_error, Toast.LENGTH_LONG);
                } finally {
                    if(cursor != null)
                        cursor.close();
                }

                activity.startActivity(intent);
            }
        });
    }

    @Override
    public void readFromTask(Task task) {
        this.myTask = task;
        String uri = GCalHelper.getTaskEventUri(task);
        if(!TextUtils.isEmpty(uri)) {
            try {
                calendarUri = Uri.parse(uri);

                // try to load calendar
                ContentResolver cr = activity.getContentResolver();
                Cursor cursor = cr.query(calendarUri, new String[] { "dtstart" }, null, null, null); //$NON-NLS-1$
                boolean deleted = cursor.getCount() == 0;
                cursor.close();
                if(deleted) {
                    calendarUri = null;
                    return;
                }

                addToCalendar.setVisibility(View.GONE);
                calendarSelector.setVisibility(View.GONE);
                viewCalendarEvent.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                exceptionService.reportError("unable-to-parse-calendar: " +  //$NON-NLS-1$
                        task.getValue(Task.CALENDAR_URI), e);
            }
        }
    }

    @SuppressWarnings("nls")
    @Override
    public String writeToModel(Task task) {
        boolean gcalCreateEventEnabled = Preferences.getStringValue(R.string.gcal_p_default) != null &&
                                        !Preferences.getStringValue(R.string.gcal_p_default).equals("-1");
        if ((gcalCreateEventEnabled || addToCalendar.isChecked()) &&
                calendarUri == null) {
            StatisticsService.reportEvent(StatisticsConstants.CREATE_CALENDAR_EVENT);

            try{
                ContentResolver cr = activity.getContentResolver();

                ContentValues values = new ContentValues();
                String calendarId = calendars.calendarIds[calendarSelector.getSelectedItemPosition()];
                values.put("calendar_id", calendarId);

                calendarUri = GCalHelper.createTaskEvent(task, cr, values);
                task.setValue(Task.CALENDAR_URI, calendarUri.toString());

                if (addToCalendar.isChecked() && addToCalendar.isShown()) {
                    // pop up the new event
                    Intent intent = new Intent(Intent.ACTION_EDIT, calendarUri);
                    intent.putExtra("beginTime", values.getAsLong("dtstart"));
                    intent.putExtra("endTime", values.getAsLong("dtend"));
                    activity.startActivity(intent);
                }

            } catch (Exception e) {
                exceptionService.displayAndReportError(activity,
                        activity.getString(R.string.gcal_TEA_error), e);
            }
        } else if(calendarUri != null) {
            try {
                ContentValues updateValues = new ContentValues();

                // check if we need to update the item
                ContentValues setValues = task.getSetValues();
                if(setValues.containsKey(Task.TITLE.name))
                    updateValues.put("title", task.getValue(Task.TITLE));
                if(setValues.containsKey(Task.NOTES.name))
                    updateValues.put("description", task.getValue(Task.NOTES));
                if(setValues.containsKey(Task.DUE_DATE.name) || setValues.containsKey(Task.ESTIMATED_SECONDS.name))
                    GCalHelper.createStartAndEndDate(task, updateValues);

                ContentResolver cr = activity.getContentResolver();
                if(cr.update(calendarUri, updateValues, null, null) > 0)
                    return activity.getString(R.string.gcal_TEA_calendar_updated);
            } catch (Exception e) {
                exceptionService.reportError("unable-to-update-calendar: " +  //$NON-NLS-1$
                        task.getValue(Task.CALENDAR_URI), e);
            }
        }

        return null;
    }
}