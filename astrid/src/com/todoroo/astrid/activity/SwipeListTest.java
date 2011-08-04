package com.todoroo.astrid.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ViewFlipper;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.service.ThemeService;

public class SwipeListTest extends Activity {

    private ViewFlipper flipper;
    private float oldTouchValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewGroup parent = (ViewGroup) getLayoutInflater().inflate(R.layout.swipe_list_test, null);
        ViewGroup taskListParent = (ViewGroup) parent.findViewById(R.id.taskListParent);
        taskListParent.addView(getLayoutInflater().inflate(R.layout.task_list_body_standard, taskListParent, false), 1);
        setContentView(parent);
        ThemeService.applyTheme(this);

        this.flipper = (ViewFlipper)findViewById(R.id.flipper);
        flipper.showNext();
    }

    @Override
    public boolean onTouchEvent(MotionEvent touchevent) {
        switch (touchevent.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                oldTouchValue = touchevent.getX();
                break;
            }
            case MotionEvent.ACTION_UP:
            {
                float currentX = touchevent.getX();
                if (oldTouchValue < currentX)
                {
                    flipper.setInAnimation(AnimationHelper.inFromLeftAnimation());
                    flipper.setOutAnimation(AnimationHelper.outToRightAnimation());
                    flipper.showNext();
                    new Thread() {
                        @Override
                        public void run() {
                            AndroidUtilities.sleepDeep(500L);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    flipper.setInAnimation(null);
                                    flipper.setOutAnimation(null);
                                    flipper.showPrevious();
                                }
                            });
                        }
                    }.start();
                }
                if (oldTouchValue > currentX)
                {
                    flipper.setInAnimation(AnimationHelper.inFromRightAnimation());
                    flipper.setOutAnimation(AnimationHelper.outToLeftAnimation());
                    flipper.showPrevious();
                    new Thread() {
                        @Override
                        public void run() {
                            AndroidUtilities.sleepDeep(500L);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    flipper.setInAnimation(null);
                                    flipper.setOutAnimation(null);
                                    flipper.showNext();
                                }
                            });
                        }
                    }.start();
                }
            break;
            }
        }
        return false;
    }

    // helper
    private static class AnimationHelper {
        public static Animation inFromRightAnimation() {

            Animation inFromRight = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, +1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f);
            inFromRight.setDuration(350);
            inFromRight.setInterpolator(new AccelerateInterpolator());
            return inFromRight;
        }

        public static Animation outToLeftAnimation() {
            Animation outtoLeft = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, -1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f);
            outtoLeft.setDuration(350);
            outtoLeft.setInterpolator(new AccelerateInterpolator());
            return outtoLeft;
        }

        // for the next movement
        public static Animation inFromLeftAnimation() {
            Animation inFromLeft = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, -1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f);
            inFromLeft.setDuration(350);
            inFromLeft.setInterpolator(new AccelerateInterpolator());
            return inFromLeft;
        }

        public static Animation outToRightAnimation() {
            Animation outtoRight = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, +1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f);
            outtoRight.setDuration(350);
            outtoRight.setInterpolator(new AccelerateInterpolator());
            return outtoRight;
        }
    }

}
