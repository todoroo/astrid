package com.todoroo.astrid.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.astrid.service.ThemeService;

public class DateAndTimeDialog extends Dialog {

    public interface DateAndTimeDialogListener {
        public void onDateAndTimeSelected(long date);
        public void onDateAndTimeCancelled();
    }

    private final DateAndTimePicker dateAndTimePicker;
    private final Button okButton;
    private final Button cancelButton;
    private boolean cancelled = false;

    private DateAndTimeDialogListener listener;

    public DateAndTimeDialog(Context context, long startDate) {
        super(context, ThemeService.getEditDialogTheme());

        /** 'Window.FEATURE_NO_TITLE' - Used to hide the title */
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        /** Design the dialog in main.xml file */
        setContentView(R.layout.date_time_dialog);

        LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.FILL_PARENT;
        params.width = LayoutParams.FILL_PARENT;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        dateAndTimePicker = (DateAndTimePicker) findViewById(R.id.date_and_time);
        dateAndTimePicker.initializeWithDate(startDate);

        okButton = (Button) findViewById(R.id.ok);
        cancelButton = (Button) findViewById(R.id.cancel);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null)
                    listener.onDateAndTimeSelected(dateAndTimePicker.constructDueDate());
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelled = true;
                cancel();
                if (listener != null)
                    listener.onDateAndTimeCancelled();
            }
        });

        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (!cancelled) { // i.e. if back button pressed, which we treat as an "OK"
                    if (listener != null)
                        listener.onDateAndTimeSelected(dateAndTimePicker.constructDueDate());
                } else {
                    cancelled = false; // reset
                }
            }
        });
    }

    public long getSelectedDate() {
        return dateAndTimePicker.constructDueDate();
    }

    public void setSelectedDateAndTime(long date) {
        dateAndTimePicker.initializeWithDate(date);
    }

    public boolean hasTime() {
        return dateAndTimePicker.hasTime();
    }

    public void setDateAndTimeDialogListener(DateAndTimeDialogListener listener) {
        this.listener = listener;
    }

    public String getDisplayString(Context context) {
        return dateAndTimePicker.getDisplayString(context, false, false);
    }

    public String getDisplayString(Context context, long forDate) {
        return DateAndTimePicker.getDisplayString(context, forDate, false, false);
    }
}
