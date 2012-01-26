package com.todoroo.astrid.timers;

import android.app.Activity;
import android.view.View;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.timers.TimerActionControlSet.TimerStoppedListener;
import com.todoroo.astrid.ui.PopupControlSet;
import com.todoroo.astrid.ui.TimeDurationControlSet;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TimerControlSet extends PopupControlSet implements TimerStoppedListener {

    TaskEditControlSet estimated, elapsed;

    public TimerControlSet(final Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);

        this.displayText.setText(activity.getString(R.string.TEA_timer_controls));
        estimated = new TimeDurationTaskEditControlSet(activity, getView(), Task.ESTIMATED_SECONDS,
                R.id.estimatedDuration, 0, R.string.DLG_hour_minutes
                );
        elapsed = new TimeDurationTaskEditControlSet(activity, getView(), Task.ELAPSED_SECONDS, R.id.elapsedDuration,
                0, R.string.DLG_hour_minutes
                );
    }

    @Override
    public void readFromTask(Task task) {
        estimated.readFromTask(task);
        elapsed.readFromTask(task);
    }

    @Override
    public String writeToModel(Task task) {
        estimated.writeToModel(task);
        elapsed.writeToModel(task);
        return null;
    }

    // --- TimeDurationTaskEditControlSet

    /**
     * Control set for mapping a Property to a TimeDurationControlSet
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class TimeDurationTaskEditControlSet extends TaskEditControlSet {
        private final TimeDurationControlSet controlSet;
        private final IntegerProperty property;

        public TimeDurationTaskEditControlSet(Activity activity, View v, IntegerProperty property, int timeButtonId,
                int prefixResource, int titleResource) {
            super(activity, -1);
            this.property = property;
            this.controlSet = new TimeDurationControlSet(activity, v,
                    timeButtonId, prefixResource, titleResource);
        }

        @Override
        public void readFromTask(Task task) {
            controlSet.setTimeDuration(task.getValue(property));
        }

        @Override
        public String writeToModel(Task task) {
            task.setValue(property, controlSet.getTimeDurationInSeconds());
            return null;
        }
    }

    @Override
    protected void refreshDisplayView() {
        // Nothing to do here yet
    }

    @Override
    public void timerStopped(Task task) {
        elapsed.readFromTask(task);
    }

}