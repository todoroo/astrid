package com.timsu.astrid.data.enums;

import com.timsu.astrid.R;

public enum Evaluation {
    // BEST EVALUATION

    LEVEL_1(0,
            R.color.evaluation_1),
    LEVEL_2(0,
            R.color.evaluation_2),
    LEVEL_3(0,
            R.color.evaluation_3),

    // WORST EVALUATION
    ;

    int label;
    int color;
    public static final Evaluation DEFAULT = LEVEL_2;

    private Evaluation(int label, int color) {
        this.label = label;
        this.color = color;
    }

    public int getLabelResource() {
        return label;
    }

    public int getColorResource() {
        return color;
    }

}
