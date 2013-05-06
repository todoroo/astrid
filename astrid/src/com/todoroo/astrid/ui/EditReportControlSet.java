package com.todoroo.astrid.ui;

import android.app.Activity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;

public class EditReportControlSet extends PopupControlSet {

    protected EditText editText;
    protected TextView notesPreview;
    protected ImageView image;

    public EditReportControlSet(Activity activity, int viewLayout,
            int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        // TODO Auto-generated constructor stub
    }

    public EditReportControlSet(Activity activity, int viewLayout, int displayViewLayout) {
        super(activity, viewLayout, displayViewLayout, R.string.TEA_note_label);
        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
    }

    @Override
    protected void refreshDisplayView() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void readFromTaskOnInitialize() {
        // TODO Auto-generated method stub

    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void afterInflate() {
        // TODO Auto-generated method stub

    }

}
