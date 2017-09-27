package org.tasks.preferences.beast;

import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.tasks.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTouch;

class BeastModeViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.text) TextView textView;

    private final ItemTouchHelper itemTouchHelper;

    BeastModeViewHolder(View itemView, ItemTouchHelper itemTouchHelper) {
        super(itemView);
        this.itemTouchHelper = itemTouchHelper;
        ButterKnife.bind(this, itemView);
    }

    void setText(String text) {
        textView.setText(text);
    }

    @OnTouch(R.id.grabber)
    boolean onTouch(View view, MotionEvent event) {
        if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
            itemTouchHelper.startDrag(this);
        }
        return false;
    }
}
