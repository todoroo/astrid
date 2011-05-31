package com.todoroo.astrid.activity;

import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.astrid.fragment.DraggableTaskListFragment;

/**
 * Activity for working with draggable task lists, like Google Tasks lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DraggableTaskListActivity extends TaskListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.task_list_fragment);
        tasklistFragment = (DraggableTaskListFragment)getFragmentManager().findFragmentById(R.id.tasklist_fragment);
    }
}
