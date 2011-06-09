package com.todoroo.astrid.gtasks;

import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.DraggableTaskListActivity;
import com.todoroo.astrid.fragment.TaskListFragment;

public class GtasksListActivity extends DraggableTaskListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.task_list_gtasks_fragment);
    }

    public TaskListFragment getTasklistFragment() {
        return (TaskListFragment)getFragmentManager().findFragmentById(R.id.tasklist_gtasks_fragment);
    }
}
