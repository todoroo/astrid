package com.todoroo.astrid.ui;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;

public class MultilineCheckboxPreference extends CheckBoxPreference {
    public MultilineCheckboxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        MultilineHelper.makeMultiline(view);
    }
}
