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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.astrid.fragment.FilterListFragment;
import com.todoroo.astrid.fragment.TaskEditFragment;

/**
 * This activity is responsible for creating new tasks and editing existing
 * ones. It saves a task when it is paused (screen rotated, back button
 * pressed) as long as the task has a title.
 *
 * @author timsu
 *
 */
public final class TaskEditActivity extends Activity {
    protected TaskEditFragment taskEditFragment = null;

    public TaskEditFragment getTaskEditFragment() {
        return taskEditFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.task_edit_fragment);
        taskEditFragment = (TaskEditFragment)getFragmentManager().findFragmentById(R.id.taskedit_fragment);
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            // forward the searchrequest to the filterfragment
            FilterListFragment flFragment = (FilterListFragment)getFragmentManager().findFragmentById(R.id.filterlist_fragment);
            if (flFragment != null && flFragment.isInLayout())
                flFragment.onNewIntent(intent);
        }
    }

    @Override
    public void finish() {
        super.finish();
        taskEditFragment.finish();
    }
}
