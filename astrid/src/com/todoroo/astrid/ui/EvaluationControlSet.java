package com.todoroo.astrid.ui;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.service.ThemeService;

public class EvaluationControlSet extends TaskEditControlSet {
    private final List<CompoundButton> buttons = new LinkedList<CompoundButton>();
    //private final int[] colors;
    //private final List<EvaluationChangedListener> listeners = new LinkedList<EvaluationChangedListener>();

    //public interface EvaluationChangedListener {
        //public void evaluationChanged(int i, int color);
    //}

    public EvaluationControlSet(Activity activity, int layout) {
        super(activity, layout);
        //colors = Task.getEvaluationColors(activity.getResources());
    }

    public void setEvaluation(Integer i) {
        for(CompoundButton b : buttons) {
            if(b.getTag() == i) {
                b.setChecked(true);
                b.setBackgroundResource(ThemeService.getDarkVsLight(R.drawable.importance_background_selected, R.drawable.importance_background_selected_dark, false));
            } else {
                b.setChecked(false);
                b.setBackgroundResource(0);
                //b.getCompoundDrawables()[0].setAlpha(120);
                //b.getBackground().setAlpha(120);
            }
        }

        //for (EvaluationChangedListener l : listeners) {
            //l.evaluationChanged(i, colors[i]);
        //}
    }

    public Integer getEvaluation() {
        for(CompoundButton b : buttons)
            if(b.isChecked())
                return (Integer) b.getTag();
        return null;
    }

    //public void addListener(EvaluationChangedListener listener) {
        //listeners.add(listener);
    //}

    //public void removeListener(EvaluationChangedListener listener) {
        //if (listeners.contains(listener))
            //listeners.remove(listener);
    //}

    @Override
    public void readFromTask(Task task) {
        super.readFromTask(task);
        setEvaluation(model.getValue(Task.EVALUATION));
    }

    @Override
    protected void readFromTaskOnInitialize() {
        setEvaluation(model.getValue(Task.EVALUATION));
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        if(getEvaluation() != null)
            task.setValue(Task.EVALUATION, getEvaluation());
        return null;
    }

    @Override
    protected void afterInflate() {
        LinearLayout container = (LinearLayout) getView().findViewById(R.id.evaluation_container);

        int min = Task.EVALUATION_MOST;
        int max = Task.EVALUATION_LEAST;

        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        View view = new View(activity);
        Context context = view.getContext();
        context.getApplicationContext();

        for(int i = max; i >= min; i--) {
            final ToggleButton button = new ToggleButton(activity);
            LinearLayout.LayoutParams params;

            int dimension = 38;
            params = new LinearLayout.LayoutParams((int) (metrics.density * dimension), (int) (metrics.density * dimension));
            button.setLayoutParams(params);

            StringBuilder label = new StringBuilder();

            label.append(' ');

            Drawable tea_evaluation_disaster = context.getResources().getDrawable(R.drawable.tea_evaluation_disaster);
            tea_evaluation_disaster.setAlpha(120);

            Drawable tea_evaluation_bad = context.getResources().getDrawable(R.drawable.tea_evaluation_bad);
            tea_evaluation_bad.setAlpha(120);

            Drawable tea_evaluation_average = context.getResources().getDrawable(R.drawable.tea_evaluation_average);
            tea_evaluation_average.setAlpha(120);

            Drawable tea_evaluation_good = context.getResources().getDrawable(R.drawable.tea_evaluation_good);
            tea_evaluation_good.setAlpha(120);

            Drawable tea_evaluation_awesome = context.getResources().getDrawable(R.drawable.tea_evaluation_awesome);
            tea_evaluation_awesome.setAlpha(120);

            if (i == max){
                //if (button.isChecked())
                    //tea_evaluation_disaster.setAlpha(120);
                button.setButtonDrawable(tea_evaluation_disaster);
            }
            if (i == Task.EVALUATION_LEAST - 1){
                button.setButtonDrawable(tea_evaluation_bad);
            }
            if (i == Task.EVALUATION_LEAST - 2){
                button.setButtonDrawable(tea_evaluation_average);
            }
            if (i == Task.EVALUATION_LEAST - 3){
                button.setButtonDrawable(tea_evaluation_good);
            }
            if (i == Task.EVALUATION_LEAST - 4){
                button.setButtonDrawable(tea_evaluation_awesome);
            }

            button.setTextOff(label);
            button.setTextOn(label);
            button.setPadding(0, 1, 0, 0);

            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    setEvaluation((Integer)button.getTag());
                }
            });
            button.setTag(i);

            buttons.add(button);

            View padding = new View(activity);
            LinearLayout.LayoutParams paddingParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            paddingParams.weight = 1.0f;
            padding.setLayoutParams(paddingParams);
            container.addView(padding);
            container.addView(button);
        }
    }

}
