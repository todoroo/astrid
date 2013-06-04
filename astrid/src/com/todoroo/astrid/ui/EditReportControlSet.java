package com.todoroo.astrid.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.ThemeService;

/**
 * Control set for displaying the report field in the task edit fragment
 *
 * @author Panagiotis Koutsaftikis <p.kouts153@gmail.com>
 *
 */
public class EditReportControlSet extends PopupControlSet {

    protected EditText editText;
    protected TextView reportPreview;
    protected ImageView image;

    public EditReportControlSet(Activity activity, int viewLayout,
            int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
    }

    public EditReportControlSet(Activity activity, int viewLayout, int displayViewLayout) {
        super(activity, viewLayout, displayViewLayout, R.string.TEA_report_label);
        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
    }

    @Override
    protected void refreshDisplayView() {
        String textToUse;
        if (initialized)
            textToUse = editText.getText().toString();
        else
            textToUse = model.getValue(Task.REPORT);

        if (TextUtils.isEmpty(textToUse)) {
            reportPreview.setText(R.string.TEA_report_empty);
            reportPreview.setTextColor(unsetColor);
            image.setImageResource(R.drawable.tea_icn_report_gray);
        } else {
            reportPreview.setText(textToUse);
            reportPreview.setTextColor(themeColor);
            image.setImageResource(ThemeService.getTaskEditDrawable(R.drawable.tea_icn_report, R.drawable.tea_icn_report_lightblue));
        }

        linkifyDisplayView();
    }

    private void linkifyDisplayView() {
        if(!TextUtils.isEmpty(reportPreview.getText())) {
            reportPreview.setLinkTextColor(Color.rgb(100, 160, 255));
            Linkify.addLinks(reportPreview, Linkify.ALL);
        }
    }

    @Override
    protected void additionalDialogSetup() {
        super.additionalDialogSetup();
        dialog.getWindow()
            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

    }

    @Override
    protected void readFromTaskOnInitialize() {
        editText.setTextKeepState(model.getValue(Task.REPORT));
        reportPreview.setText(model.getValue(Task.REPORT));
        linkifyDisplayView();
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        task.setValue(Task.REPORT, editText.getText().toString());
        return null;
    }

    @Override
    protected void afterInflate() {
        editText = (EditText) getView().findViewById(R.id.report);
        reportPreview = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
    }

    @Override
    protected boolean onOkClick() {
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        return super.onOkClick();
    }

    @Override
    protected void onCancelClick() {
        super.onCancelClick();
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }
}
