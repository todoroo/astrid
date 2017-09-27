/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.app.PendingIntent;

/**
 * Represents an intent that can be called on a task
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAction {

    /**
     * Intent to call when invoking this operation
     */
    public PendingIntent intent = null;

    /**
     * Quick action icon
     */
    public int icon = 0;

    /**
     * Create an EditOperation object
     */
    public TaskAction(PendingIntent intent, int icon) {
        super();
        this.intent = intent;
        this.icon = icon;
    }
}
