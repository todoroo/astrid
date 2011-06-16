/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.todoroo.astrid.activity;

import java.util.Map.Entry;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.fragment.TaskListFragment;

/**
 * This activity is launched when a user opens up a notification from the
 * tray. It launches the appropriate activity based on the passed in parameters.
 *
 * @author timsu
 *
 */
public class ShortcutActivity extends Activity {

    // --- constants

    /** token for passing a task id through extras for viewing a single task */
    public static final String TOKEN_SINGLE_TASK = "id"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s title through extras */
    public static final String TOKEN_FILTER_TITLE = "title"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s sql through extras */
    public static final String TOKEN_FILTER_SQL = "sql"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s values for new tasks through extras as string */
    @Deprecated
    public static final String TOKEN_FILTER_VALUES = "v4nt"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s values for new tasks through extras as exploded ContentValues */
    public static final String TOKEN_FILTER_VALUES_ITEM = "v4ntp_"; //$NON-NLS-1$

    /** token for passing a ComponentNameto launch */
    public static final String TOKEN_CUSTOM_CLASS = "class"; //$NON-NLS-1$

    // --- implementation

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);

        launchTaskList(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        launchTaskList(intent);
    }

    private void launchTaskList(Intent intent) {
        Bundle extras = intent.getExtras();

        Intent taskListIntent = new Intent(this, TaskListActivity.class);

        if(extras != null && extras.containsKey(TOKEN_CUSTOM_CLASS)) {
            taskListIntent.setComponent(ComponentName.unflattenFromString(extras.getString(TOKEN_CUSTOM_CLASS)));
            taskListIntent.putExtras(intent.getExtras());
        }

        if(extras != null && extras.containsKey(TOKEN_FILTER_SQL)) {
            // launched from desktop shortcut, must create a fake filter
            String title = extras.getString(TOKEN_FILTER_TITLE);
            String sql = extras.getString(TOKEN_FILTER_SQL);
            ContentValues values = null;
            if(extras.containsKey(TOKEN_FILTER_VALUES))
                values = AndroidUtilities.contentValuesFromString(extras.getString(TOKEN_FILTER_VALUES));
            else {
                values = new ContentValues();
                for(String key : extras.keySet()) {
                    if(!key.startsWith(TOKEN_FILTER_VALUES_ITEM))
                        continue;

                    Object value = extras.get(key);
                    key = key.substring(TOKEN_FILTER_VALUES_ITEM.length());

                    // assume one of the big 4...
                    if(value instanceof String)
                        values.put(key, (String) value);
                    else if(value instanceof Integer)
                        values.put(key, (Integer) value);
                    else if(value instanceof Double)
                        values.put(key, (Double) value);
                    else if(value instanceof Long)
                        values.put(key, (Long) value);
                    else
                        throw new IllegalStateException("Unsupported bundle type " + value.getClass()); //$NON-NLS-1$
                }
            }

            Filter filter = new Filter("", title, sql, values); //$NON-NLS-1$

            taskListIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
        } else if(extras != null && extras.containsKey(TOKEN_SINGLE_TASK)) {
            Filter filter = new Filter("", getString(R.string.TLA_custom), //$NON-NLS-1$
                    new QueryTemplate().where(Task.ID.eq(extras.getLong(TOKEN_SINGLE_TASK, -1))), null);

            taskListIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            taskListIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
            startActivity(taskListIntent);
        }

        startActivity(taskListIntent);
        finish();
    }

    public static Intent createIntent(Filter filter) {
        Intent shortcutIntent = new Intent(ContextManager.getContext(),
                ShortcutActivity.class);

        if(filter instanceof FilterWithCustomIntent) {
            FilterWithCustomIntent customFilter = ((FilterWithCustomIntent)filter);
            if(customFilter.customExtras != null)
                shortcutIntent.putExtras(customFilter.customExtras);
            shortcutIntent.putExtra(TOKEN_CUSTOM_CLASS, customFilter.customTaskList.flattenToString());
        }

        shortcutIntent.setAction(Intent.ACTION_VIEW);
        shortcutIntent.putExtra(ShortcutActivity.TOKEN_FILTER_TITLE,
                filter.title);
        shortcutIntent.putExtra(ShortcutActivity.TOKEN_FILTER_SQL,
                filter.sqlQuery);
        if (filter.valuesForNewTasks != null) {
            for (Entry<String, Object> item : filter.valuesForNewTasks.valueSet()) {
                String key = TOKEN_FILTER_VALUES_ITEM + item.getKey();
                Object value = item.getValue();
                putExtra(shortcutIntent, key, value);
            }
        }
        return shortcutIntent;
    }

    private static void putExtra(Intent intent, String key, Object value) {
        // assume one of the big 4...
        if (value instanceof String)
            intent.putExtra(key, (String) value);
        else if (value instanceof Integer)
            intent.putExtra(key, (Integer) value);
        else if (value instanceof Double)
            intent.putExtra(key, (Double) value);
        else if (value instanceof Long)
            intent.putExtra(key, (Long) value);
        else
            throw new IllegalStateException(
                    "Unsupported bundle type " + value.getClass()); //$NON-NLS-1$
    }
}
