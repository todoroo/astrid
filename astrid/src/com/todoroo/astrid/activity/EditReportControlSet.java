package com.todoroo.astrid.activity;

import android.app.Activity;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.PopupControlSet;

public class EditReportControlSet extends PopupControlSet {

    public EditReportControlSet(Activity activity, int viewLayout,
            int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        // TODO Auto-generated constructor stub
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
