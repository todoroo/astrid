package com.todoroo.astrid.gtasks;

import android.os.Bundle;

import com.commonsware.cwac.tlv.TouchListView;
import com.commonsware.cwac.tlv.TouchListView.DropListener;
import com.commonsware.cwac.tlv.TouchListView.SwipeListener;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.fragment.DraggableTaskListFragment;

public class GtasksListFragment extends DraggableTaskListFragment {

    @Autowired private GtasksTaskListUpdater gtasksTaskListUpdater;

    @Override
    protected IntegerProperty getIndentProperty() {
        return GtasksMetadata.INDENT;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getTouchListView().setDropListener(dropListener);
        getTouchListView().setSwipeListener(swipeListener);

        if(!Preferences.getBoolean(GtasksPreferenceService.PREF_SHOWN_LIST_HELP, false)) {
            Preferences.setBoolean(GtasksPreferenceService.PREF_SHOWN_LIST_HELP, true);
            DialogUtilities.okDialog(this.getActivity(),
                    getString(R.string.gtasks_help_title),
                    android.R.drawable.ic_dialog_info,
                    getString(R.string.gtasks_help_body), null);
        }
    }

    private final TouchListView.DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            long targetTaskId = taskAdapter.getItemId(from);
            long destinationTaskId = taskAdapter.getItemId(to);
            gtasksTaskListUpdater.moveTo(targetTaskId, destinationTaskId);
            loadTaskListContent(true);
        }
    };

    private final TouchListView.SwipeListener swipeListener = new SwipeListener() {
        @Override
        public void swipeRight(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            gtasksTaskListUpdater.indent(targetTaskId, 1);
            loadTaskListContent(true);
        }

        @Override
        public void swipeLeft(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            gtasksTaskListUpdater.indent(targetTaskId, -1);
            loadTaskListContent(true);
        }
    };
}
