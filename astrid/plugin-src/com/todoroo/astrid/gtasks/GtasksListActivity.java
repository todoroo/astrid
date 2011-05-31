package com.todoroo.astrid.gtasks;

import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.DraggableTaskListActivity;

public class GtasksListActivity extends DraggableTaskListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.task_list_fragment);
        tasklistFragment = (GtasksListFragment)getFragmentManager().findFragmentById(R.id.tasklist_fragment);
    }
}
