package org.tasks.calendars;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.annotation.Nullable;

import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static android.provider.BaseColumns._ID;

public class CalendarProvider {

    private static final String CAN_MODIFY = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">= " + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR;
    private static final String SORT = CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC";
    private static final String[] COLUMNS = {
            _ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
    };

    private final PermissionChecker permissionChecker;
    private final ContentResolver contentResolver;

    @Inject
    public CalendarProvider(@ForApplication Context context, PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
        contentResolver = context.getContentResolver();
    }

    public List<AndroidCalendar> getCalendars() {
        return getCalendars(CalendarContract.Calendars.CONTENT_URI, CAN_MODIFY);
    }

    @Nullable
    public AndroidCalendar getCalendar(String id) {
        List<AndroidCalendar> calendars = getCalendars(CalendarContract.Calendars.CONTENT_URI, CAN_MODIFY + " AND Calendars._id=" + id);
        return calendars.isEmpty() ? null : calendars.get(0);
    }

    private List<AndroidCalendar> getCalendars(Uri uri, String selection) {
        if (!permissionChecker.canAccessCalendars()) {
            return Collections.emptyList();
        }

        List<AndroidCalendar> calendars = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, COLUMNS, selection, null, SORT);
            if (cursor != null && cursor.getCount() > 0) {
                int idColumn = cursor.getColumnIndex(_ID);
                int nameColumn = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
                while (cursor.moveToNext()) {
                    calendars.add(new AndroidCalendar(cursor.getString(idColumn), cursor.getString(nameColumn)));
                }
            }
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return calendars;
    }
}
